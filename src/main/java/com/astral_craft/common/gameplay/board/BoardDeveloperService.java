package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgress;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.BoardDeveloperConfigPayload;
import com.astral_craft.common.network.s2c.BoardCharacterSelectionEntry;
import com.astral_craft.common.network.s2c.OpenBoardDeveloperPayload;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.TimedStatModifier;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** TeaCon/development board editor used for deterministic setup and safe live bot changes. */
public class BoardDeveloperService {

    public static final int MAX_BASE_STAT = 99;
    public static final int MAX_HEALTH = 999;
    public static final int MAX_STAR_COINS = 999999;
    public static final int MAX_STARS = 999;
    public static final int MAX_CARD_PLAYS = 99;
    public static final int MAX_FIXED_MOVE = 999;
    public static final int MAX_SKILL_COOLDOWN = 9999;
    public static final int MAX_KNOCKDOWN_TURNS = 99;
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();
    private static final Map<UUID, Long> PAUSED_AT_TICKS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> CONFIGURED_SLOTS = new HashMap<>();

    public static void begin(ServerPlayer player, BoardSession session) {
        if (player == null || session == null) return;
        OWNERS.put(session.id(), player.getUUID());
        if (session.phase() == BoardPhase.PLAYING) {
            PAUSED_AT_TICKS.putIfAbsent(session.id(), AstralServerTickClock.now(player.level()));
            CONFIGURED_SLOTS.remove(session.id());
        }
    }

    public static boolean active(UUID boardId) {
        return boardId != null && OWNERS.containsKey(boardId);
    }

    public static boolean ownedBy(UUID boardId, UUID playerId) {
        return boardId != null && playerId != null && playerId.equals(OWNERS.get(boardId));
    }

    public static boolean ownerOnline(ServerLevel level, UUID boardId) {
        UUID owner = boardId == null ? null : OWNERS.get(boardId);
        ServerPlayer player = level == null || owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        return player != null && player.level() == level;
    }

    public static boolean hasOtherHuman(BoardSession session, UUID playerId) {
        if (session == null || playerId == null) return true;
        return session.partyParticipants().stream().anyMatch(participant -> !participant.bot() && !participant.controlledBy(playerId));
    }

    public static boolean canEditLive(ServerPlayer player, BoardSession session) {
        if (player == null || session == null || session.phase() != BoardPhase.PLAYING) return false;
        return session.movement() == null && session.encounter() == null && session.discard() == null
                && !BoardBattleService.active(session.id()) && !BoardEventService.active(session.id())
                && !BoardLotteryService.active(session.id()) && !BasePlatform.hasActiveBoardEffect(session.id())
                && !BoardMonsterService.active(session.id()) && !BoardPanelSelectionService.hasPending(player)
                && !PendingCardActionManager.isExclusiveBusy(player) && !PendingCardActionManager.hasPendingSelection(player)
                && !PendingCardActionManager.hasBoardCardUi(player)
                && !BoardSessionManager.hasPendingDeveloperUnsafeAction(session.id());
    }

    public static boolean openConfiguration(ServerPlayer player, BoardSession session) {
        if (!ownedBy(session.id(), player.getUUID())) return false;
        boolean live = session.phase() == BoardPhase.PLAYING;
        if (!live && session.phase() != BoardPhase.CHARACTER_SELECTION) return false;
        if (live && !canEditLive(player, session)) return false;
        BoardParticipant currentHuman = session.participantByController(player.getUUID()).orElse(null);
        if (live && currentHuman == null) return false;
        if (hasOtherHuman(session, player.getUUID())) return false;
        List<CharacterDefinition> characters = CharacterManager.INSTANCE.values();
        List<Identifier> botSelectable = characters.stream()
                .filter(definition -> CharacterManager.INSTANCE.character(definition.id()).botSelectable())
                .map(CharacterDefinition::id).toList();
        Identifier humanCharacterId = currentHuman == null ? preferredDeveloperCharacter(player) : currentHuman.characterId();
        CharacterDefinition humanDefinition = CharacterManager.INSTANCE.get(humanCharacterId);
        Identifier humanSkinId = currentHuman == null ? preferredDeveloperSkin(player, humanDefinition) : currentHuman.skinId();
        List<OpenBoardDeveloperPayload.BotView> bots = live ? liveBotViews(session)
                : defaultBotViews(characters, botSelectable, humanCharacterId);
        if (bots.size() != BoardSessionManager.REQUIRED_PLAYERS - 1) return false;
        BoardCharacterSelectionEntry humanEntry = new BoardCharacterSelectionEntry(0, player.getScoreboardName(),
                humanCharacterId, humanSkinId, true, true);
        PacketDistributor.sendToPlayer(player, new OpenBoardDeveloperPayload(session.id(), characters,
                botSelectable, developerCardIds(), humanEntry, bots, live));
        return true;
    }

    public static void apply(ServerPlayer player, BoardDeveloperConfigPayload payload) {
        BoardSession session = BoardSessionManager.session(player.level(), payload.boardId()).orElse(null);
        if (session == null || !ownedBy(payload.boardId(), player.getUUID())) return;
        if (payload.bots().isEmpty()) {
            if (session.phase() == BoardPhase.CHARACTER_SELECTION) BoardSessionManager.resetForLobby(player.level(), session);
            else resume(player.level(), session);
            return;
        }
        if (payload.bots().size() != BoardSessionManager.REQUIRED_PLAYERS - 1 || hasOtherHuman(session, player.getUUID())) {
            reject(player, session);
            return;
        }
        boolean applied = session.phase() == BoardPhase.CHARACTER_SELECTION
                ? applyInitial(player, session, payload.humanCharacterId(), payload.humanSkinId(), payload.bots())
                : session.humanCount() == 1 && canEditLive(player, session) && applyLive(player, session, payload.bots());
        if (!applied) reject(player, session);
    }

    public static boolean configured(UUID boardId, UUID slotId) {
        Set<UUID> slots = CONFIGURED_SLOTS.get(boardId);
        return slots != null && slots.contains(slotId);
    }

    public static void clear(UUID boardId) {
        if (boardId == null) return;
        OWNERS.remove(boardId);
        PAUSED_AT_TICKS.remove(boardId);
        CONFIGURED_SLOTS.remove(boardId);
    }

    public static void resume(ServerLevel level, BoardSession session) {
        if (level == null || session == null) return;
        Long pausedAt = PAUSED_AT_TICKS.remove(session.id());
        if (pausedAt != null && session.phase() == BoardPhase.PLAYING) {
            long pausedTicks = Math.max(0L, AstralServerTickClock.now(level) - pausedAt);
            if (pausedTicks > 0L) {
                if (session.actionPromptDeadlineTick() > 0L) {
                    session.setActionPromptDeadlineTick(session.actionPromptDeadlineTick() + pausedTicks);
                }
                if (session.actionDeadlineTick() > 0L) {
                    session.setActionDeadlineTick(session.actionDeadlineTick() + pausedTicks);
                }
                BoardSessionManager.markChanged(level);
            }
        }
        OWNERS.remove(session.id());
        CONFIGURED_SLOTS.remove(session.id());
        if (session.phase() == BoardPhase.PLAYING) BoardHudSyncManager.send(level, session);
    }

    private static boolean applyInitial(ServerPlayer player, BoardSession session, Identifier humanCharacterId,
                                        Identifier humanSkinId, List<BoardDeveloperConfigPayload.BotSetup> setups) {
        if (!CharacterManager.INSTANCE.contains(humanCharacterId)) return false;
        CharacterDefinition humanDefinition = CharacterManager.INSTANCE.get(humanCharacterId);
        CharacterSkinDefinition humanSkin = humanDefinition.skinOrDefault(humanSkinId.getPath());
        Identifier safeHumanSkin = BoardParticipant.skinIdentifier(humanDefinition.id(), humanSkin.id());
        if (!safeHumanSkin.equals(humanSkinId)) return false;
        Set<Identifier> allowedCards = new HashSet<>(developerCardIds());
        Set<Identifier> usedCharacters = new LinkedHashSet<>();
        usedCharacters.add(humanDefinition.id());
        List<ConfiguredBot> configured = validateSetups(setups, usedCharacters, allowedCards, Map.of());
        if (configured == null) return false;
        session.clearParticipants();
        BoardLobbyService.clear(session.id());
        BoardParticipant human = new BoardParticipant(UUID.randomUUID(), player.getUUID(), false,
                humanDefinition.id(), safeHumanSkin, BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID,
                null, AstralPlayerStats.DEFAULT, List.of(), Map.of(), 0, 0, 0, 7, session.nextArrivalOrder());
        session.putParticipant(human);
        Set<UUID> slots = new LinkedHashSet<>();
        for (ConfiguredBot bot : configured) {
            UUID slotId = UUID.randomUUID();
            BoardParticipant participant = new BoardParticipant(slotId, null, true, bot.characterId(), bot.skinId(),
                    BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID, null, bot.stats(), bot.hand(),
                    Map.of(), bot.skillCooldownTurns(), bot.knockedDownTurns(), bot.cardPlaysUsed(), bot.maxHandSize(),
                    session.nextArrivalOrder());
            session.putParticipant(participant);
            slots.add(slotId);
        }

        CONFIGURED_SLOTS.put(session.id(), Set.copyOf(slots));
        BoardSessionManager.markChanged(player.level());
        BoardSessionManager.startGame(player.level(), session);
        return true;
    }

    private static boolean applyLive(ServerPlayer player, BoardSession session, List<BoardDeveloperConfigPayload.BotSetup> setups) {
        Map<UUID, BoardParticipant> bots = new LinkedHashMap<>();
        for (BoardParticipant participant : orderedPartyParticipants(session)) {
            if (participant.bot() && !participant.monster()) bots.put(participant.slotUuid(), participant);
        }
        if (bots.size() != BoardSessionManager.REQUIRED_PLAYERS - 1) return false;
        BoardParticipant human = session.participantByController(player.getUUID()).orElse(null);
        if (human == null) return false;
        Set<Identifier> usedCharacters = new LinkedHashSet<>();
        usedCharacters.add(human.characterId());
        List<ConfiguredBot> configured = validateSetups(setups, usedCharacters, new HashSet<>(developerCardIds()), bots);
        if (configured == null || configured.size() != bots.size()) return false;
        ServerLevel level = player.level();
        for (ConfiguredBot bot : configured) {
            BoardParticipant current = bots.get(bot.slotId());
            if (current == null) return false;
            boolean identityChanged = !current.characterId().equals(bot.characterId()) || !current.skinId().equals(bot.skinId());
            BoardParticipant updated = current.withIdentity(bot.characterId(), bot.skinId())
                    .withStats(bot.stats()).withHand(bot.hand()).withSkillCooldown(bot.skillCooldownTurns())
                    .withKnockedDownTurns(bot.knockedDownTurns()).withCardPlaysUsed(bot.cardPlaysUsed())
                    .withMaxHandSize(bot.maxHandSize());
            session.putParticipant(updated);
            BoardEntityService.syncState(level, updated);
            if (identityChanged && BoardEntityService.entity(level, updated) != null) {
                BoardEntityService.entity(level, updated).setAnimationAction("idle");
            }
        }

        BoardSessionManager.markChanged(level);
        resume(level, session);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.applied"), true);
        return true;
    }

    private static List<ConfiguredBot> validateSetups(List<BoardDeveloperConfigPayload.BotSetup> setups,
                                                       Set<Identifier> usedCharacters, Set<Identifier> allowedCards,
                                                       Map<UUID, BoardParticipant> existingBots) {
        List<ConfiguredBot> result = new ArrayList<>();
        Set<UUID> usedSlots = new HashSet<>();
        for (BoardDeveloperConfigPayload.BotSetup setup : setups) {
            if (!CharacterManager.INSTANCE.contains(setup.characterId()) || !usedCharacters.add(setup.characterId())) return null;
            if (!CharacterManager.INSTANCE.character(setup.characterId()).botSelectable()) return null;
            CharacterDefinition definition = CharacterManager.INSTANCE.get(setup.characterId());
            CharacterSkinDefinition skin = definition.skinOrDefault(setup.skinId().getPath());
            Identifier skinId = BoardParticipant.skinIdentifier(definition.id(), skin.id());
            if (!skinId.equals(setup.skinId())) return null;
            BoardParticipant existing = existingBots.isEmpty() ? null : existingBots.get(setup.slotId());
            if (!existingBots.isEmpty() && (existing == null || !usedSlots.add(existing.slotUuid()))) return null;
            List<Identifier> hand = expandHand(setup.cards(), allowedCards);
            if (hand == null) return null;
            AstralPlayerStats stats = validatedStats(setup.stats(), existing == null ? null : existing.stats());
            int maxHandSize = Math.clamp(setup.maxHandSize(), 1, BoardParticipant.MAX_SUPPORTED_HAND_SIZE);
            int cooldown = Math.clamp(setup.skillCooldownTurns(), 0, MAX_SKILL_COOLDOWN);
            int knockedDownTurns = Math.clamp(setup.knockedDownTurns(), 0, MAX_KNOCKDOWN_TURNS);
            int cardPlaysUsed = Math.clamp(setup.cardPlaysUsed(), 0, MAX_CARD_PLAYS);
            result.add(new ConfiguredBot(existing == null ? setup.slotId() : existing.slotUuid(), definition.id(),
                    skinId, stats, hand, cooldown, knockedDownTurns, cardPlaysUsed, maxHandSize));
        }
        return result;
    }

    private static AstralPlayerStats validatedStats(BoardDeveloperConfigPayload.BotStats value, AstralPlayerStats existing) {
        int maxHealth = Math.clamp(value.maxHealth(), 1, MAX_HEALTH);
        Map<Identifier, BoardBuffInstance> buffs = existing == null ? Map.of() : existing.buffs();
        List<TimedStatModifier> modifiers = existing == null ? List.of() : existing.modifiers();
        return new AstralPlayerStats(
                Math.clamp(value.baseAttack(), -MAX_BASE_STAT, MAX_BASE_STAT),
                Math.clamp(value.baseDefense(), -MAX_BASE_STAT, MAX_BASE_STAT),
                maxHealth, Math.clamp(value.health(), 0, maxHealth),
                Math.clamp(value.starCoins(), 0, MAX_STAR_COINS), Math.clamp(value.stars(), 0, MAX_STARS),
                Math.clamp(value.cardPlaysPerTurn(), 0, MAX_CARD_PLAYS),
                Math.clamp(value.cardPlaysRemaining(), 0, MAX_CARD_PLAYS),
                Math.clamp(value.nextMoveFixed(), 0, MAX_FIXED_MOVE), buffs, modifiers);
    }

    private static List<OpenBoardDeveloperPayload.BotView> defaultBotViews(List<CharacterDefinition> characters,
                                                                           List<Identifier> botSelectable,
                                                                           Identifier humanCharacterId) {
        Set<Identifier> selectable = new HashSet<>(botSelectable);
        List<OpenBoardDeveloperPayload.BotView> result = new ArrayList<>();
        for (CharacterDefinition definition : characters) {
            if (result.size() >= BoardSessionManager.REQUIRED_PLAYERS - 1) break;
            if (definition.id().equals(humanCharacterId) || !selectable.contains(definition.id())) continue;
            AstralPlayerStats stats = defaultStats(definition);
            result.add(new OpenBoardDeveloperPayload.BotView(UUID.randomUUID(), definition.id(), firstSkin(definition),
                    stats, List.of(), 0, 0, 0, 7));
        }
        return List.copyOf(result);
    }

    private static List<OpenBoardDeveloperPayload.BotView> liveBotViews(BoardSession session) {
        List<OpenBoardDeveloperPayload.BotView> result = new ArrayList<>();
        for (BoardParticipant participant : orderedPartyParticipants(session)) {
            if (!participant.bot() || participant.monster()) continue;
            result.add(new OpenBoardDeveloperPayload.BotView(participant.slotUuid(), participant.characterId(),
                    participant.skinId(), participant.stats(), participant.hand(), participant.skillCooldownTurns(),
                    participant.knockedDownTurns(), participant.cardPlaysUsed(), participant.maxHandSize()));
        }
        return List.copyOf(result);
    }

    private static List<BoardParticipant> orderedPartyParticipants(BoardSession session) {
        List<BoardParticipant> result = new ArrayList<>();
        Set<UUID> added = new HashSet<>();
        for (UUID slotId : session.turnOrder()) {
            BoardParticipant participant = session.participant(slotId).orElse(null);
            if (participant != null && !participant.monster() && added.add(participant.slotUuid())) result.add(participant);
        }
        for (BoardParticipant participant : session.partyParticipants()) {
            if (added.add(participant.slotUuid())) result.add(participant);
        }
        return result;
    }

    private static Identifier preferredDeveloperCharacter(ServerPlayer player) {
        CharacterProgress progress = CharacterProgressManager.progress(player);
        Identifier selected = progress.selectedCharacter();
        return CharacterManager.INSTANCE.contains(selected) ? selected : CharacterManager.INSTANCE.defaultCharacter().id();
    }

    private static Identifier preferredDeveloperSkin(ServerPlayer player, CharacterDefinition definition) {
        CharacterProgress progress = CharacterProgressManager.progress(player);
        CharacterSkinDefinition skin = definition.skinOrDefault(progress.entry(definition.id()).selectedSkin());
        return BoardParticipant.skinIdentifier(definition.id(), skin.id());
    }

    private static AstralPlayerStats defaultStats(CharacterDefinition definition) {
        AstralPlayerStats stats = CharacterManager.INSTANCE.character(definition.id())
                .initializeBoardStats(AstralPlayerStats.initial(definition.baseStats()));
        return stats.addCoins(BoardSessionManager.PVP_INITIAL_STAR_COINS - stats.starCoins());
    }

    private static Identifier firstSkin(CharacterDefinition definition) {
        String skin = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
        return BoardParticipant.skinIdentifier(definition.id(), skin);
    }

    private static List<Identifier> expandHand(List<BoardDeveloperConfigPayload.CardCount> entries, Set<Identifier> allowedCards) {
        List<Identifier> hand = new ArrayList<>();
        int total = 0;
        for (BoardDeveloperConfigPayload.CardCount entry : entries) {
            if (entry.count() <= 0 || !allowedCards.contains(entry.cardId())) return null;
            total += entry.count();
            if (total > BoardParticipant.MAX_SUPPORTED_HAND_SIZE) return null;
            for (int index = 0; index < entry.count(); index++) hand.add(entry.cardId());
        }
        return List.copyOf(hand);
    }

    private static List<Identifier> developerCardIds() {
        List<Identifier> candidates = new ArrayList<>();
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BaseHandCard)
                .map(BuiltInRegistries.ITEM::getKey).forEach(candidates::add);
        return List.copyOf(candidates);
    }

    private static void reject(ServerPlayer player, BoardSession session) {
        if (session.phase() == BoardPhase.CHARACTER_SELECTION) BoardSessionManager.resetForLobby(player.level(), session);
        else resume(player.level(), session);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.invalid"), true);
    }

    private record ConfiguredBot(UUID slotId, Identifier characterId, Identifier skinId, AstralPlayerStats stats,
                                 List<Identifier> hand, int skillCooldownTurns, int knockedDownTurns,
                                 int cardPlaysUsed, int maxHandSize) {}

}