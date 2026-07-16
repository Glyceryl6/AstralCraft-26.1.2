package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CombatBonusDefinition;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.SoulLinkManager;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillEffects;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import com.astral_craft.common.gameplay.dice.AstralDiceRollService;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.s2c.*;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Server-authoritative board coordinator. Physical layouts are stored as compact NBT-backed
 * {@link BoardSavedData}; only logical nodes, positions and match state are persisted.
 */
public class BoardSessionManager {

    public static final int REQUIRED_PLAYERS = 4;
    public static final int LOBBY_TIMEOUT_TICKS = 20 * 25;
    public static final int TURN_TIMEOUT_TICKS = 20 * 45;
    public static final int DISCARD_TIMEOUT_TICKS = 20 * 20;
    public static final int ENCOUNTER_TIMEOUT_TICKS = 20 * 12;
    public static final int BRANCH_TIMEOUT_TICKS = 20 * 12;
    public static final int MOVEMENT_STEP_TICKS = 3;
    private static final int MOVEMENT_GAP_TICKS = 1;
    public static final int MAX_ROUTE_BRANCHES = 96;
    private static final int GRAVITY_GUARD_SCAN_INTERVAL = 4;
    private static final int GRAVITY_GUARD_HEIGHT = 24;
    private static final Map<UUID, Set<UUID>> LOBBY_VIEWERS = new HashMap<>();
    private static final Map<UUID, PendingBotEffect> PENDING_BOT_EFFECTS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_BOT_MOVEMENT_TICKS = new HashMap<>();
    private static final Map<UUID, Long> NO_HUMAN_SINCE_TICKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<BoardArea>> ACTIVE_PROTECTED_AREAS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, PendingGravityRestore>> PENDING_GRAVITY_RESTORES = new HashMap<>();

    public static void selectCharacter(ServerPlayer player, String rawBoardId, Identifier characterId, Identifier skinId) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.phase() != BoardPhase.CHARACTER_SELECTION) return;
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        Optional<BoardParticipant> existing = session.participantByController(player.getUUID());
        boolean occupied = session.participants().stream().anyMatch(participant -> participant.characterId().equals(characterId)
                && existing.map(value -> !value.slotUuid().equals(participant.slotUuid())).orElse(true));
        if (occupied) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.character_taken"), true);
            sendCharacterSelection(player, session, false);
            return;
        }

        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        String safeSkin = definition.skinOrDefault(skinId.getPath()).id();
        UUID slotId = existing.map(BoardParticipant::slotUuid).orElseGet(UUID::randomUUID);
        BoardParticipant participant = new BoardParticipant(slotId, Optional.of(player.getUUID()), false,
                characterId, BoardParticipant.skinIdentifier(characterId, safeSkin),
                BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID, Optional.empty(), AstralPlayerStats.DEFAULT,
                List.of(), Map.of(), 0, 0, 0, 7, session.nextArrivalOrder());
        session.putParticipant(participant);
        session.setLobbyDeadlineTick(Math.max(session.lobbyDeadlineTick(), player.level().getGameTime() + 20L));
        markChanged(player.level());
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.character_confirmed",
                Component.translatable(definition.nameKey())).withStyle(ChatFormatting.GREEN), true);
        removeLobbyViewer(session.id(), player.getUUID());
        if (session.participantCount() >= REQUIRED_PLAYERS || isSoloIntegratedServer(player)) {
            startGame(player.level(), session);
        } else {
            refreshLobbyScreens(player.level(), session);
        }
    }

    public static boolean toggleProtection(ServerPlayer player, BlockPos pos) {
        Optional<BoardSession> maybeSession = findAt(player.level(), pos);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        session.setProtectionEnabled(!session.protectionEnabled());
        markChanged(player.level());
        refreshProtectedAreas(player.level(), data(player.level()));
        syncBoardSnapshot(player.level(), session);
        player.sendSystemMessage(Component.translatable(session.protectionEnabled()
                ? "message.astral_craft.board.protection_enabled"
                : "message.astral_craft.board.protection_disabled"), true);
        return true;
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        return data(level).sessions().stream().anyMatch(session -> session.protects(level.dimension(), pos));
    }

    public static List<BoardSession> sessions(ServerLevel level) {
        return data(level).sessions();
    }

    public static Optional<BoardSession> findAt(ServerLevel level, BlockPos pos) {
        return data(level).sessions().stream()
                .filter(session -> session.protectedArea().contains(pos)
                        || session.positions().containsValue(pos))
                .findFirst();
    }

    public static Optional<BoardSession> findByController(ServerPlayer player) {
        return data(player.level()).sessions().stream()
                .filter(session -> session.phase() == BoardPhase.PLAYING
                        && session.participantByController(player.getUUID()).isPresent())
                .findFirst();
    }

    public static Optional<BoardSession> findByEntity(AstralCharacterEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return Optional.empty();
        UUID boardId = entity.boardSessionUuid().orElse(null);
        if (boardId != null) {
            BoardSession direct = data(level).get(boardId);
            if (direct != null) return Optional.of(direct);
        }

        return data(level).sessions().stream()
                .filter(session -> session.participantByEntity(entity.getUUID()).isPresent())
                .findFirst();
    }

    public static boolean isValidPawn(AstralCharacterEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return false;
        UUID boardId = entity.boardSessionUuid().orElse(null);
        UUID slotId = entity.boardParticipantUuid().orElse(null);
        if (boardId == null || slotId == null) return false;
        BoardSession session = data(level).get(boardId);
        if (session == null || session.phase() != BoardPhase.PLAYING) return false;
        BoardParticipant participant = session.participant(slotId).orElse(null);
        return participant != null && participant.entityUuid().filter(entity.getUUID()::equals).isPresent();
    }

    public static Optional<BoardParticipant> participantForController(ServerPlayer player) {
        return findByController(player).flatMap(session -> session.participantByController(player.getUUID()));
    }

    public static Optional<BoardParticipant> participantForEntity(AstralCharacterEntity entity) {
        return findByEntity(entity).flatMap(session -> session.participantByEntity(entity.getUUID()));
    }

    public static AstralPlayerStats statsForController(ServerPlayer player, AstralPlayerStats fallback) {
        return participantForController(player).map(BoardParticipant::stats).orElse(fallback);
    }

    public static AstralPlayerStats statsForEntity(AstralCharacterEntity entity, AstralPlayerStats fallback) {
        return participantForEntity(entity).map(BoardParticipant::stats).orElse(fallback);
    }

    public static boolean setStatsForController(ServerPlayer player, AstralPlayerStats stats) {
        Optional<BoardSession> maybeSession = findByController(player);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null) return false;
        updateParticipant(player.level(), session, participant.withStats(stats));
        return true;
    }

    public static boolean setStatsForEntity(AstralCharacterEntity entity, AstralPlayerStats stats) {
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty() || !(entity.level() instanceof ServerLevel level)) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantByEntity(entity.getUUID()).orElse(null);
        if (participant == null) return false;
        updateParticipant(level, session, participant.withStats(stats));
        return true;
    }

    public static boolean addRoundStatusEffect(AstralCharacterEntity entity, Identifier statusId, int turns) {
        if (entity == null || statusId == null || turns <= 0 || !(entity.level() instanceof ServerLevel level)) return false;
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantByEntity(entity.getUUID()).orElse(null);
        if (participant == null) return false;
        updateParticipant(level, session, participant.withRoundStatusEffect(statusId, turns));
        return true;
    }

    public static Optional<Identifier> selectedCharacterForController(ServerPlayer player) {
        return findByController(player)
                .flatMap(session -> session.participantByController(player.getUUID()))
                .map(BoardParticipant::characterId);
    }

    private static boolean acceptsBoardPawn(CardDefinition definition) {
        return definition.targetTypes().stream().anyMatch(type -> type.isAssignableFrom(Player.class));
    }

    public static Optional<ServerPlayer> controllerFor(AstralCharacterEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return Optional.empty();
        return participantForEntity(entity).flatMap(BoardParticipant::controllerUuid)
                .map(level.getServer().getPlayerList()::getPlayer);
    }

    public static List<ServerPlayer> humanViewers(ServerPlayer controller) {
        Optional<BoardSession> maybeSession = findByController(controller);
        return maybeSession.map(boardSession -> humanPlayers(controller.level(), boardSession)).orElseGet(() -> List.of(controller));
    }

    public static boolean openTurnScreen(ServerPlayer player, AstralCharacterEntity entity) {
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantByEntity(entity.getUUID()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return false;
        boolean currentTurn = session.currentParticipant().map(value -> value.slotUuid().equals(participant.slotUuid())).orElse(false);
        boolean canAct = currentTurn && session.movement() == null && session.encounter() == null
                && session.discard() == null && !BoardBattleService.active(session.id());
        sendTurnScreen(player, session, participant, entity, canAct);
        return true;
    }

    private static void sendTurnScreen(ServerPlayer player, BoardSession session, BoardParticipant participant, AstralCharacterEntity entity) {
        sendTurnScreen(player, session, participant, entity, true);
    }

    private static void sendTurnScreen(ServerPlayer player, BoardSession session, BoardParticipant participant,
                                       AstralCharacterEntity entity, boolean currentTurn) {
        int durationTicks = currentTurn ? Math.max(1, session.actionDurationTicks()) : 1;
        int remainingTicks = currentTurn ? (int) Math.clamp(
                session.actionDeadlineTick() - player.level().getGameTime(), 0L, durationTicks) : 0;
        PacketDistributor.sendToPlayer(player, new OpenBoardTurnPayload(session.id().toString(), entity.getId(),
                encodeHand(participant.hand()), participant.cardPlaysUsed(), participant.stats().cardPlaysPerTurn(),
                participant.skillCooldownTurns(), remainingTicks, durationTicks,
                participant.characterId(), participant.skinId(), currentTurn));
    }


    public static void leaveGame(ServerPlayer player, String rawBoardId) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.phase() != BoardPhase.PLAYING) return;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null) return;
        PendingCardActionManager.cancel(player);
        BoardParticipant botParticipant = participant.asBot();
        updateParticipant(player.level(), session, botParticipant);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.left_game"), true);
        if (!hasOnlineHumanParticipant(player.level(), session)) {
            endGame(player.level(), session, true);
            return;
        }

        BoardBattleService.participantBecameBot(player.level(), session, botParticipant.slotUuid());
        BasePlatform.activeParticipantBecameAutomated(player.level(), session, botParticipant.slotUuid());
        if (session.phase() != BoardPhase.PLAYING) return;
        BoardSession.EncounterState encounter = session.encounter();
        if (encounter != null && encounter.moverSlotId().equals(botParticipant.slotUuid())) {
            BoardParticipant target = session.participant(encounter.targetSlotId()).orElse(null);
            session.setEncounter(null);
            if (target != null) BoardBattleService.start(player.level(), session, botParticipant, target);
        }
        BoardSession.DiscardState discard = session.discard();
        if (discard != null && discard.slotId().equals(botParticipant.slotUuid())) {
            randomDiscard(player.level(), session);
        }
        if (session.currentParticipant().map(value -> value.slotUuid().equals(botParticipant.slotUuid())).orElse(false)) {
            session.setActionDeadlineTick(player.level().getGameTime());
        }
        markChanged(player.level());
        syncBoardSnapshot(player.level(), session);
    }

    public static void requestMove(ServerPlayer player, String rawBoardId) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.phase() != BoardPhase.PLAYING || session.movement() != null
                || session.encounter() != null || session.discard() != null
                || BoardBattleService.active(session.id())
                || PendingCardActionManager.isExclusiveBusy(player)
                || PendingCardActionManager.hasPendingSelection(player)) return;
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || participant.knockedDown()) return;
        AstralCharacterEntity entity = entity(player.level(), participant);
        if (entity == null) return;
        participant = participant.recordManualDecision();
        updateParticipant(player.level(), session, participant);
        AstralDiceRollService.DiceRollResult result = AstralDiceRollService.rollNextMove(player,
                entity.position().add(0.0D, entity.getBbHeight() + 0.85D, 0.0D));
        int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                + (result.values().size() > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
        beginMovement(player.level(), session, participant, result.total(), revealTicks);
    }

    private static void beginTimedOutHumanMovement(ServerLevel level, BoardSession session,
                                                   BoardParticipant participant, ServerPlayer controller) {
        PendingCardActionManager.cancel(controller);
        BoardParticipant current = session.currentParticipant().orElse(null);
        if (current == null || !current.slotUuid().equals(participant.slotUuid())
                || current.knockedDown() || session.movement() != null || session.encounter() != null
                || session.discard() != null || BoardBattleService.active(session.id())) {
            if (current != null && current.knockedDown()) finishTurn(level, session);
            return;
        }
        current = current.recordTimedOutDecision();
        updateParticipant(level, session, current);
        AstralCharacterEntity entity = entity(level, current);
        if (entity == null) {
            finishTurn(level, session);
            return;
        }
        AstralDiceRollService.DiceRollResult result = AstralDiceRollService.rollNextMove(controller,
                entity.position().add(0.0D, entity.getBbHeight() + 0.85D, 0.0D));
        int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                + (result.values().size() > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
        beginMovement(level, session, current, result.total(), revealTicks);
    }

    public static void requestSkill(ServerPlayer player, String rawBoardId) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.phase() != BoardPhase.PLAYING || BoardBattleService.active(session.id())
                || PendingCardActionManager.isExclusiveBusy(player)
                || PendingCardActionManager.hasPendingSelection(player)) return;
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || participant.skillCooldownTurns() > 0 || session.movement() != null
                || session.encounter() != null || session.discard() != null) return;
        AstralCharacterEntity entity = entity(player.level(), participant);
        if (entity == null) return;
        int cooldown = AstralCharacterSkillService.useActiveSkillForBoard(player, entity,
                participant.characterId(), participant.skinName(), humanPlayers(player.level(), session));
        if (cooldown < 0) return;
        BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
        BoardParticipant updated = refreshed.recordManualDecision().withSkillCooldown(cooldown);
        updateParticipant(player.level(), session, updated);
        sendTurnScreen(player, session, updated, entity);
    }

    public static boolean selectBranch(ServerPlayer player, BlockPos clickedPos) {
        Optional<BoardSession> maybeSession = findByController(player);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardSession.MovementState movement = session.movement();
        if (movement == null || movement.branchChoices().isEmpty()) return false;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return false;
        String nodeId = session.positions().entrySet().stream()
                .filter(entry -> entry.getValue().equals(clickedPos)).map(Map.Entry::getKey).findFirst().orElse("");
        if (!movement.branchChoices().contains(nodeId)) return false;
        updateParticipant(player.level(), session, participant.recordManualDecision());
        session.setMovement(movement.beginStep(nodeId, player.level().getGameTime(), MOVEMENT_STEP_TICKS));
        markChanged(player.level());
        broadcastRoutePreview(player.level(), session);
        return true;
    }

    public static void chooseEncounter(ServerPlayer player, String rawBoardId, boolean challenge) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.encounter() == null) return;
        BoardSession.EncounterState encounter = session.encounter();
        BoardParticipant mover = session.participant(encounter.moverSlotId()).orElse(null);
        BoardParticipant target = session.participant(encounter.targetSlotId()).orElse(null);
        if (mover == null || target == null || !mover.controlledBy(player.getUUID())) return;
        mover = mover.recordManualDecision();
        updateParticipant(player.level(), session, mover);
        session.setEncounter(null);
        if (challenge) {
            BoardBattleService.start(player.level(), session, mover, target);
        } else {
            resumeAfterEncounter(player.level(), session);
        }

        markChanged(player.level());
    }

    public static void discard(ServerPlayer player, String rawBoardId, List<Integer> indexes) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.discard() == null) return;
        BoardSession.DiscardState discard = session.discard();
        BoardParticipant participant = session.participant(discard.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        Set<Integer> unique = new LinkedHashSet<>(indexes);
        if (unique.size() != discard.requiredCount()) return;
        if (unique.stream().anyMatch(index -> index < 0 || index >= participant.hand().size())) return;
        List<Identifier> next = new ArrayList<>(participant.hand());
        unique.stream().sorted(Comparator.reverseOrder()).forEach(index -> next.remove((int) index));
        updateParticipant(player.level(), session, participant.recordManualDecision().withHand(next));
        session.setDiscard(null);
        finishTurn(player.level(), session);
    }

    public static ItemStack boardCardStack(ServerPlayer player, int index) {
        Optional<BoardSession> maybeSession = findByController(player);
        if (maybeSession.isEmpty()) return ItemStack.EMPTY;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || index < 0 || index >= participant.hand().size()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
        return item instanceof BaseHandCard ? new ItemStack(item) : ItemStack.EMPTY;
    }

    public static boolean canUseBoardCard(ServerPlayer player, String rawBoardId, int index) {
        BoardSession session = session(player.level(), rawBoardId);
        if (session == null || session.phase() != BoardPhase.PLAYING) return false;
        BoardParticipant participant = currentControlledParticipant(session, player);
        return participant != null && session.movement() == null && session.encounter() == null
                && session.discard() == null && participant.stats().cardPlaysRemaining() > 0
                && index >= 0 && index < participant.hand().size();
    }

    public static void consumeBoardCard(ServerPlayer player, int index) {
        Optional<BoardSession> maybeSession = findByController(player);
        if (maybeSession.isEmpty()) return;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || index < 0 || index >= participant.hand().size()) return;
        BoardParticipant updated = participant.recordManualDecision().removeCard(index).useCardPlay();
        updateParticipant(player.level(), session, updated);
        AstralCharacterEntity entity = entity(player.level(), updated);
        if (entity != null && session.movement() == null && session.encounter() == null
                && session.discard() == null && !BoardBattleService.active(session.id())) {
            sendTurnScreen(player, session, updated, entity);
        }
    }

    public static List<CardTargetCandidate> cardCandidates(
            ServerPlayer user, ItemStack stack, CardDefinition definition,
            BaseHandCard card, int effectiveRange) {
        Optional<BoardSession> maybeSession = findByController(user);
        if (maybeSession.isEmpty()) return List.of();
        BoardSession session = maybeSession.get();
        BoardParticipant source = session.participantByController(user.getUUID()).orElse(null);
        if (source == null) return List.of();
        int range = Math.max(0, effectiveRange);
        List<CardTargetCandidate> result = new ArrayList<>();
        for (BoardParticipant target : session.participants()) {
            if (target.stats().health() <= 0) continue;
            if (target.slotUuid().equals(source.slotUuid()) && !card.allowsSelfTarget()) continue;
            AstralCharacterEntity entity = entity(user.level(), target);
            if (entity == null || !acceptsBoardPawn(definition)) continue;
            int distance = graphDistance(session, source.currentNodeKey(), target.currentNodeKey(), range);
            if (distance < 0 || distance > range) continue;
            result.add(new CardTargetCandidate(entity.getId(), entity.getDisplayName().copy(), distance));
        }

        result.sort(Comparator.comparingInt(CardTargetCandidate::distance));
        return result.stream().limit(OpenTargetSelectionPayload.MAX_CANDIDATES).toList();
    }

    public static boolean isValidBoardTarget(
            ServerPlayer user, Entity target, ItemStack stack,
            CardDefinition definition, BaseHandCard card, int effectiveRange) {
        if (!(target instanceof AstralCharacterEntity character) || !acceptsBoardPawn(definition)) return false;
        Optional<BoardSession> maybeSession = findByController(user);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant source = session.participantByController(user.getUUID()).orElse(null);
        BoardParticipant selected = session.participantByEntity(character.getUUID()).orElse(null);
        if (source == null || selected == null || selected.stats().health() <= 0) return false;
        if (source.slotUuid().equals(selected.slotUuid()) && !card.allowsSelfTarget()) return false;
        int range = Math.max(0, effectiveRange);
        int distance = graphDistance(session, source.currentNodeKey(), selected.currentNodeKey(), range);
        return distance >= 0 && distance <= range;
    }

    public static void onParticipantDamaged(AstralCharacterEntity entity, AstralPlayerStats stats) {
        if (stats.health() > 0) return;
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty() || !(entity.level() instanceof ServerLevel level)) return;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantByEntity(entity.getUUID()).orElse(null);
        if (participant == null || participant.knockedDownTurns() > 0) return;
        BoardParticipant knocked = participant.knockDown();
        updateParticipant(level, session, knocked);
        entity.setAnimationAction("knockdown");
        participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                player.sendSystemMessage(Component.translatable("message.astral_craft.knockdown",
                        Math.max(0, (participant.stats().starCoins() + 1) / 2)), true));
    }

    public static void endGame(ServerLevel level, BoardSession session, boolean keepBoard) {
        BoardBattleService.cancel(session.id());
        PENDING_BOT_EFFECTS.remove(session.id());
        PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BasePlatform.clearActiveBoardEffect(session.id());
        broadcastRouteState(session, false, "", "", "");
        clearRuntimeEntities(level, session);
        session.clearParticipants();
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        session.setPhase(keepBoard ? BoardPhase.READY : BoardPhase.FINISHED);
        session.setProtectionEnabled(keepBoard);
        session.setKeepAfterGame(keepBoard);
        markChanged(level);
        syncBoardSnapshot(level, session);
        BoardSavedData savedData = data(level);
        if (!keepBoard) savedData.remove(session.id());
        refreshProtectedAreas(level, savedData);
    }

    public static void serverTick(MinecraftServer server) {
        Set<ResourceKey<Level>> activeDimensions = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            activeDimensions.add(level.dimension());
            BoardSavedData savedData = data(level);
            refreshProtectedAreas(level, savedData);
            restoreGravityBlocks(level);
            if (level.getGameTime() % GRAVITY_GUARD_SCAN_INTERVAL == 0L) {
                guardGravityBlocks(level, savedData);
            }
            for (BoardSession session : savedData.sessions()) {
                tickSession(level, session);
            }
        }

        ACTIVE_PROTECTED_AREAS.keySet().retainAll(activeDimensions);
        PENDING_GRAVITY_RESTORES.keySet().retainAll(activeDimensions);
    }

    public static boolean protectFallingBlock(ServerLevel level, FallingBlockEntity fallingBlock) {
        BlockPos source = fallingBlock.blockPosition();
        boolean protectedSource = ACTIVE_PROTECTED_AREAS.getOrDefault(level.dimension(), List.of()).stream()
                .anyMatch(area -> area.contains(source));
        if (!protectedSource) return false;
        PENDING_GRAVITY_RESTORES.computeIfAbsent(level.dimension(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(source.immutable(), new PendingGravityRestore(
                        fallingBlock.getBlockState(), level.getGameTime() + 1L));
        return true;
    }

    public static void refreshProtectedAreas(ServerLevel level, BoardSavedData savedData) {
        List<BoardArea> areas = savedData.sessions().stream()
                .filter(BoardSession::protectionEnabled)
                .map(BoardSession::protectedArea).toList();
        if (areas.isEmpty()) ACTIVE_PROTECTED_AREAS.remove(level.dimension());
        else ACTIVE_PROTECTED_AREAS.put(level.dimension(), areas);
    }

    private static void restoreGravityBlocks(ServerLevel level) {
        Map<BlockPos, PendingGravityRestore> pending = PENDING_GRAVITY_RESTORES.get(level.dimension());
        if (pending == null || pending.isEmpty()) return;
        pending.entrySet().removeIf(entry -> {
            PendingGravityRestore restore = entry.getValue();
            if (level.getGameTime() < restore.restoreAfterTick()) return false;
            BlockPos pos = entry.getKey();
            if (isProtected(level, pos) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, restore.state(), 3);
            }
            return true;
        });
        if (pending.isEmpty()) PENDING_GRAVITY_RESTORES.remove(level.dimension());
    }

    private static void guardGravityBlocks(ServerLevel level, BoardSavedData data) {
        Set<UUID> handled = new HashSet<>();
        for (BoardSession session : data.sessions()) {
            if (!session.protectionEnabled()) continue;
            BoardArea area = session.protectedArea();
            AABB bounds = new AABB(area.min().getX(), area.min().getY(), area.min().getZ(),
                    area.max().getX() + 1.0D, area.max().getY() + GRAVITY_GUARD_HEIGHT, area.max().getZ() + 1.0D);
            for (FallingBlockEntity fallingBlock : level.getEntitiesOfClass(FallingBlockEntity.class, bounds)) {
                if (!handled.add(fallingBlock.getUUID())) continue;
                BlockPos current = fallingBlock.blockPosition();
                if (insideProtectedColumn(area, current)
                        && current.getY() <= area.max().getY() + GRAVITY_GUARD_HEIGHT - 1) {
                    fallingBlock.discard();
                }
            }
        }
    }

    private static boolean insideProtectedColumn(BoardArea area, BlockPos pos) {
        return pos.getX() >= area.min().getX() && pos.getX() <= area.max().getX()
                && pos.getZ() >= area.min().getZ() && pos.getZ() <= area.max().getZ()
                && pos.getY() >= area.min().getY();
    }

    private static void tickSession(ServerLevel level, BoardSession session) {
        if (session.phase() == BoardPhase.CHARACTER_SELECTION) {
            if (session.lobbyDeadlineTick() <= 0L) {
                session.setLobbyDeadlineTick(level.getGameTime() + LOBBY_TIMEOUT_TICKS);
            }
            if (session.participantCount() > 0 && level.getGameTime() >= session.lobbyDeadlineTick()) {
                startGame(level, session);
            }
            return;
        }

        if (session.phase() != BoardPhase.PLAYING) return;
        if (!hasOnlineHumanParticipant(level, session)) {
            long since = NO_HUMAN_SINCE_TICKS.computeIfAbsent(session.id(), ignored -> level.getGameTime());
            if (level.getGameTime() - since >= 100L) {
                endGame(level, session, true);
            }
            return;
        }

        NO_HUMAN_SINCE_TICKS.remove(session.id());
        ensureEntities(level, session);
        BoardParticipant current = session.currentParticipant().orElse(null);
        if (current == null) return;
        if (current.stats().health() <= 0 && current.knockedDownTurns() <= 0) {
            current = current.repairKnockdownState();
            updateParticipant(level, session, current);
            if (session.turnStarted()) {
                finishTurn(level, session);
                return;
            }
        }

        if (!session.turnStarted()) beginCurrentTurn(level, session);
        BoardSession.DiscardState discard = session.discard();
        if (discard != null) {
            if (level.getGameTime() >= discard.deadlineTick()) {
                BoardParticipant timedOut = session.participant(discard.slotId()).orElse(null);
                if (timedOut != null && !isAutomated(level, timedOut)) {
                    updateParticipant(level, session, timedOut.recordTimedOutDecision());
                }
                randomDiscard(level, session);
            }
            return;
        }

        if (BasePlatform.tickActiveBoardEffect(level, session)) return;
        BoardSession.EncounterState encounter = session.encounter();
        if (encounter != null) {
            if (level.getGameTime() >= encounter.deadlineTick()) {
                BoardParticipant mover = session.participant(encounter.moverSlotId()).orElse(null);
                BoardParticipant target = session.participant(encounter.targetSlotId()).orElse(null);
                session.setEncounter(null);
                if (mover != null && !isAutomated(level, mover)) {
                    mover = mover.recordTimedOutDecision();
                    updateParticipant(level, session, mover);
                }
                if (mover != null && target != null && isAutomated(level, mover)) {
                    BoardBattleService.start(level, session, mover, target);
                } else {
                    resumeAfterEncounter(level, session);
                }
            }

            return;
        }

        if (BoardBattleService.active(session.id())) return;
        if (session.movement() != null) {
            tickMovement(level, session);
            return;
        }

        current = session.currentParticipant().orElse(current);
        ServerPlayer controller = current.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer)
                .orElse(null);
        boolean automated = current.bot() || controller == null;
        if (session.actionDeadlineTick() <= 0L) {
            int durationTicks = automated ? 1 : current.decisionDurationTicks(TURN_TIMEOUT_TICKS);
            session.setActionDurationTicks(durationTicks);
            session.setActionDeadlineTick(automated ? level.getGameTime()
                    : level.getGameTime() + durationTicks);
            markChanged(level);
        }
        if (automated) {
            if (level.getGameTime() >= session.actionDeadlineTick()) {
                beginBotTurn(level, session, current);
            }
            return;
        }

        if (level.getGameTime() >= session.actionDeadlineTick()) {
            beginTimedOutHumanMovement(level, session, current, controller);
        }
    }

    private static void startGame(ServerLevel level, BoardSession session) {
        if (session.phase() == BoardPhase.PLAYING || session.participantCount() == 0) return;
        fillBots(level, session);
        List<BoardParticipant> participants = new ArrayList<>(session.participants());
        if (participants.size() != REQUIRED_PLAYERS) {
            for (ServerPlayer player : humanPlayers(level, session)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_enough_characters",
                        participants.size(), REQUIRED_PLAYERS).withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        List<String> starts = new ArrayList<>(session.startNodes());
        Collections.shuffle(starts, new Random(level.getRandom().nextLong()));
        List<UUID> order = new ArrayList<>();
        for (int index = 0; index < participants.size(); index++) {
            BoardParticipant participant = participants.get(index);
            String startNode = starts.get(index % starts.size());
            CharacterDefinition definition = CharacterManager.INSTANCE.get(participant.characterId());
            AstralPlayerStats stats = AstralPlayerStats.initial(definition.baseStats());
            List<Identifier> hand = randomInitialHand(level, 4 + level.getRandom().nextInt(2));
            BoardParticipant initialized = new BoardParticipant(participant.slotId(), participant.controllerId(),
                    participant.bot(), participant.characterId(), participant.skinId(),
                    BoardParticipant.nodeIdentifier(startNode), BoardParticipant.EMPTY_NODE_ID, Optional.empty(),
                    stats, hand, Map.of(), 0, 0, 0, 7, session.nextArrivalOrder());
            session.putParticipant(initialized);
            session.setHomeNode(initialized.slotUuid(), startNode);
            spawnCharacter(level, session, initialized);
            order.add(initialized.slotUuid());
        }

        Collections.shuffle(order, new Random(level.getRandom().nextLong()));
        session.setTurnOrder(order);
        closeLobbyScreens(level, session.id());
        session.setPhase(BoardPhase.PLAYING);
        session.setLobbyDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        for (ServerPlayer viewer : humanPlayers(level, session)) {
            viewer.sendSystemMessage(Component.translatable("message.astral_craft.board.game_started")
                    .withStyle(ChatFormatting.AQUA), true);
        }

        beginCurrentTurn(level, session);
    }

    private static void beginCurrentTurn(ServerLevel level, BoardSession session) {
        BoardParticipant current = session.currentParticipant().orElse(null);
        if (current == null) return;
        boolean wasKnockedDown = current.knockedDown();
        BoardParticipant next = current.beginTurn();
        boolean stillKnockedDown = next.knockedDown();
        updateParticipant(level, session, next);
        session.setTurnStarted(true);
        ServerPlayer controller = next.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        boolean automated = next.bot() || controller == null;
        int durationTicks = stillKnockedDown || automated ? 1 : next.decisionDurationTicks(TURN_TIMEOUT_TICKS);
        session.setActionDurationTicks(durationTicks);
        session.setActionDeadlineTick(stillKnockedDown ? 0L
                : automated ? level.getGameTime() : level.getGameTime() + durationTicks);
        markChanged(level);
        next.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player -> {
            player.sendSystemMessage(Component.translatable(stillKnockedDown
                    ? "message.astral_craft.board.turn_skipped_knockdown"
                    : "message.astral_craft.board.your_turn", session.round() + 1), true);
            if (!stillKnockedDown) {
                AstralCharacterEntity character = entity(level, next);
                if (character != null) sendTurnScreen(player, session, next, character);
            }
        });

        if (wasKnockedDown && stillKnockedDown) {
            finishTurn(level, session);
        }
    }

    private static void beginMovement(ServerLevel level, BoardSession session, BoardParticipant participant, int steps, int delayTicks) {
        session.setMovement(BoardSession.MovementState.begin(participant.slotUuid(), Math.max(0, steps), level.getGameTime() + Math.max(0, delayTicks)));
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        if (steps <= 0) finishMovement(level, session);
    }

    private static void beginBotTurn(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (entity(level, participant) == null) return;
        Long movementTick = PENDING_BOT_MOVEMENT_TICKS.get(session.id());
        if (movementTick != null) {
            if (level.getGameTime() < movementTick) return;
            PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
            BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
            if (refreshed.knockedDown()) finishTurn(level, session);
            else beginBotMovement(level, session, refreshed);
            return;
        }

        PendingBotEffect pending = PENDING_BOT_EFFECTS.get(session.id());
        if (pending != null) {
            if (level.getGameTime() < pending.executeTick()) return;
            PENDING_BOT_EFFECTS.remove(session.id());
            BoardParticipant current = session.participant(pending.userSlotId()).orElse(null);
            Item item = BuiltInRegistries.ITEM.getValue(pending.cardId());
            if (current != null && item instanceof BaseHandCard card && item instanceof BoardBotEffect botEffect) {
                ItemStack stack = new ItemStack(item);
                CardDefinition definition = card.definition(stack);
                BoardBotEffectContext context = new BoardBotEffectContext(level, session,
                        current.slotUuid(), definition, pending.targetSlotIds());
                int followUpDelay = botEffect.applyByBoardBot(context);
                if (followUpDelay > 0) {
                    long followUpTick = level.getGameTime() + followUpDelay;
                    PENDING_BOT_MOVEMENT_TICKS.put(session.id(), followUpTick);
                    session.setActionDeadlineTick(followUpTick);
                    markChanged(level);
                    return;
                }
            }

            BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
            if (refreshed.knockedDown()) {
                if (refreshed.knockedDownTurns() <= 0) updateParticipant(level, session, refreshed.knockDown());
                finishTurn(level, session);
            } else {
                beginBotMovement(level, session, refreshed);
            }
            return;
        }

        BoardParticipant refreshed = tryUseBotEffectCard(level, session, participant);
        if (PENDING_BOT_EFFECTS.containsKey(session.id())) return;
        if (refreshed.knockedDown()) {
            if (refreshed.knockedDownTurns() <= 0) updateParticipant(level, session, refreshed.knockDown());
            finishTurn(level, session);
            return;
        }

        beginBotMovement(level, session, refreshed);
    }

    private static BoardParticipant tryUseBotEffectCard(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (participant.cardPlaysUsed() > 0 || participant.cardPlaysUsed() >= participant.stats().cardPlaysPerTurn()) return participant;
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
            if (!(item instanceof BaseHandCard card) || !(item instanceof BoardBotEffect botEffect)) continue;
            ItemStack stack = new ItemStack(item);
            CardDefinition definition = card.definition(stack);
            BoardBotEffectContext context = new BoardBotEffectContext(level, session,
                    participant.slotUuid(), definition, List.of());
            if (definition.type() == CardType.EFFECT && botEffect.canUseByBoardBot(context)) {
                candidates.add(index);
            }
        }

        Collections.shuffle(candidates, new Random(level.getRandom().nextLong()));
        for (int handIndex : candidates) {
            Identifier cardId = participant.hand().get(handIndex);
            Item item = BuiltInRegistries.ITEM.getValue(cardId);
            if (!(item instanceof BaseHandCard card) || !(item instanceof BoardBotEffect botEffect)) continue;
            ItemStack stack = new ItemStack(item);
            CardDefinition definition = card.definition(stack);
            BoardBotEffectContext context = new BoardBotEffectContext(level, session,
                    participant.slotUuid(), definition, List.of());
            List<UUID> targetSlotIds = botEffect.selectBoardBotTargets(context);
            if (definition.needsTarget() && targetSlotIds.isEmpty()) continue;
            BoardParticipant current = session.participant(participant.slotUuid()).orElse(participant);
            BoardParticipant used = current.removeCard(handIndex).useCardPlay();
            updateParticipant(level, session, used);
            long executeTick = level.getGameTime() + CardUseService.CARD_REVEAL_DURATION_TICKS + 2L;
            PENDING_BOT_EFFECTS.put(session.id(), new PendingBotEffect(
                    used.slotUuid(), cardId, targetSlotIds, executeTick));
            session.setActionDeadlineTick(executeTick);
            int sourceEntityId = entityId(level, used);
            List<Integer> targetEntityIds = targetSlotIds.stream()
                    .map(session::participant).flatMap(Optional::stream)
                    .mapToInt(target -> entityId(level, target)).filter(id -> id >= 0).boxed().toList();
            CardRevealPayload reveal = new CardRevealPayload(cardId.toString(), stack.copyWithCount(1),
                    definition.type().getSerializedName(), definition.displayName(stack),
                    definition.effectText(stack, definition.range()), definition.largeFrontTexture(stack),
                    definition.largeBackTexture(), CardRevealPayload.ANIMATION_FLIP,
                    CardUseService.CARD_REVEAL_DURATION_TICKS, sourceEntityId, targetEntityIds);
            for (ServerPlayer viewer : humanPlayers(level, session)) PacketDistributor.sendToPlayer(viewer, reveal);
            markChanged(level);
            return used;
        }
        return participant;
    }

    static BoardParticipant applyBotDamage(ServerLevel level, BoardSession session,
                                                   BoardParticipant attacker, BoardParticipant target,
                                                   int damage, boolean rewardKnockout) {
        int resolvedDamage = Math.max(0, damage + target.stats().incomingDamageBonus()
                + Math.min(1, target.stats().buff(BuffKinds.MARK)));
        BoardParticipant damaged = target.withStats(target.stats().damage(resolvedDamage));
        if (damaged.stats().health() <= 0) {
            int lostCoins = Math.max(0, (damaged.stats().starCoins() + 1) / 2);
            damaged = damaged.knockDown();
            if (rewardKnockout && lostCoins > 0) {
                BoardParticipant currentAttacker = session.participant(attacker.slotUuid()).orElse(attacker);
                updateParticipant(level, session,
                        currentAttacker.withStats(currentAttacker.stats().addCoins(lostCoins)));
            }
        }
        updateParticipant(level, session, damaged);
        return damaged;
    }

    private static void beginBotMovement(ServerLevel level, BoardSession session, BoardParticipant participant) {
        AstralCharacterEntity entity = entity(level, participant);
        if (entity == null) return;
        int fixed = participant.stats().nextMoveFixed();
        int diceCount = fixed > 0 ? 1 : Math.clamp(1 + participant.stats().nextMoveExtraDice(), 1, 8);
        int total = 0;
        for (int i = 0; i < diceCount; i++) total += fixed > 0 ? fixed : level.getRandom().nextInt(10) + 1;
        AstralDiceEntity dice = new AstralDiceEntity(level, entity.getX(), entity.getY() + entity.getBbHeight() + 0.85D, entity.getZ());
        dice.startRoll(1, fixed > 0 ? fixed : 10, AstralDiceRollService.DEFAULT_ROLL_TICKS,
                AstralDiceRollService.DEFAULT_SPIN_SPEED, fixed > 0 ? fixed : Math.max(1, total / diceCount),
                total, diceCount > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0, true, 0.0F, 0.0F);
        level.addFreshEntity(dice);
        updateParticipant(level, session, participant.withStats(participant.stats().clearNextMoveDiceEffects()));
        int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                + (diceCount > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
        beginMovement(level, session, participant, total, revealTicks);
    }

    private static void tickMovement(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) return;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null) {
            session.setMovement(null);
            return;
        }

        AstralCharacterEntity entity = entity(level, participant);
        if (entity == null) return;
        if (movement.stepping()) {
            BlockPos from = session.positions().get(participant.currentNodeKey());
            BlockPos to = session.positions().get(movement.activeTargetNodeId());
            if (from == null || to == null) {
                session.setMovement(null);
                return;
            }

            Direction direction = directionBetween(from, to);
            entity.setBoardDirection(direction);
            float progress = Mth.clamp((level.getGameTime() - movement.stepStartedTick()) / (float) movement.stepDurationTicks(), 0.0F, 1.0F);
            double x = Mth.lerp(progress, from.getX() + 0.5D, to.getX() + 0.5D);
            double y = Mth.lerp(progress, from.getY() + 0.12D, to.getY() + 0.12D);
            double z = Mth.lerp(progress, from.getZ() + 0.5D, to.getZ() + 0.5D);
            entity.setPos(x, y, z);
            entity.setAnimationAction("walk");
            if (progress < 1.0F) return;
            BoardParticipant moved = participant.withNode(participant.currentNodeId(),
                    BoardParticipant.nodeIdentifier(movement.activeTargetNodeId()), session.nextArrivalOrder());
            session.putParticipant(moved);
            movement = movement.completeStep(movement.activeTargetNodeId(), level.getGameTime() + MOVEMENT_GAP_TICKS);
            session.setMovement(movement);
            arrangeNode(level, session, participant.currentNodeKey());
            arrangeNode(level, session, moved.currentNodeKey());
            continueArrival(level, session);
            return;
        }
        if (!movement.branchChoices().isEmpty()) {
            if (isAutomated(level, participant) || level.getGameTime() >= movement.nextStepTick()) {
                if (!isAutomated(level, participant)) {
                    participant = participant.recordTimedOutDecision();
                    updateParticipant(level, session, participant);
                }
                String choice = movement.branchChoices().get(level.getRandom().nextInt(movement.branchChoices().size()));
                session.setMovement(movement.beginStep(choice, level.getGameTime(), MOVEMENT_STEP_TICKS));
                broadcastRoutePreview(level, session);
                markChanged(level);
            }
            return;
        }

        if (movement.remainingSteps() <= 0) {
            finishMovement(level, session);
            return;
        }

        if (level.getGameTime() < movement.nextStepTick()) return;
        broadcastRoutePreview(level, session);
        List<String> choices = nextChoices(session, participant);
        if (choices.isEmpty()) {
            finishMovement(level, session);
        } else if (choices.size() == 1 || isAutomated(level, participant)) {
            String choice = choices.size() == 1 ? choices.getFirst() : choices.get(level.getRandom().nextInt(choices.size()));
            session.setMovement(movement.beginStep(choice, level.getGameTime(), MOVEMENT_STEP_TICKS));
        } else {
            int durationTicks = participant.decisionDurationTicks(BRANCH_TIMEOUT_TICKS);
            session.setMovement(movement.waitingForBranch(choices, level.getGameTime() + durationTicks));
            broadcastRoutePreview(level, session);
            participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                    player.sendSystemMessage(Component.translatable("message.astral_craft.board.choose_branch"), true));
        }

        markChanged(level);
    }

    private static void continueArrival(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) return;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null) {
            finishMovement(level, session);
            return;
        }
        if (participant.stats().health() <= 0 || participant.knockedDownTurns() > 0) {
            finishMovement(level, session);
            return;
        }

        String arrivedNodeId = movement.route().isEmpty()
                ? participant.currentNodeKey() : movement.route().getLast();
        if (!participant.currentNodeKey().equals(arrivedNodeId)) {
            continueMovement(level, session, movement);
            return;
        }

        if (!movement.panelResolved()) {
            BoardParticipant encounterTarget = firstEncounterTarget(session, participant,
                    movement.resolvedEncounterSlots());
            if (encounterTarget != null) {
                movement = movement.withResolvedEncounter(encounterTarget.slotUuid());
                session.setMovement(movement);
                beginEncounter(level, session, participant, encounterTarget);
                markChanged(level);
                return;
            }

            movement = movement.withPanelResolved();
            session.setMovement(movement);
            applyPanel(level, session, participant, movement.remainingSteps() == 0);
            if (session.phase() != BoardPhase.PLAYING
                    || BasePlatform.hasActiveBoardEffect(session.id())
                    || session.movement() != movement) return;

            participant = session.participant(movement.slotId()).orElse(participant);
            if (participant.stats().health() <= 0 || participant.knockedDownTurns() > 0) {
                finishMovement(level, session);
                return;
            }
            if (!participant.currentNodeKey().equals(arrivedNodeId)) {
                continueMovement(level, session, movement);
                return;
            }
        }

        continueMovement(level, session, movement);
    }

    private static void continueMovement(ServerLevel level, BoardSession session,
                                         BoardSession.MovementState movement) {
        if (movement.remainingSteps() <= 0) {
            finishMovement(level, session);
        } else {
            broadcastRoutePreview(level, session);
            markChanged(level);
        }
    }

    private static void finishMovement(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) return;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        session.setMovement(null);
        broadcastRouteState(session, false, "", "", "");
        if (participant == null) {
            finishTurn(level, session);
            return;
        }

        AstralCharacterEntity entity = entity(level, participant);
        if (entity != null) entity.setAnimationAction("idle");
        int extra = Math.max(0, participant.hand().size() - participant.maxHandSize());
        if (extra > 0) {
            int durationTicks = participant.decisionDurationTicks(DISCARD_TIMEOUT_TICKS);
            session.setDiscard(new BoardSession.DiscardState(participant.slotUuid(), extra,
                    level.getGameTime() + durationTicks, durationTicks));
            if (participant.bot()) {
                randomDiscard(level, session);
            } else {
                participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                        PacketDistributor.sendToPlayer(player, new OpenBoardDiscardPayload(session.id().toString(),
                                encodeHand(participant.hand()), extra, durationTicks, durationTicks,
                                participant.characterId(), participant.skinId())));
            }
        } else {
            finishTurn(level, session);
        }

        markChanged(level);
    }

    private static void finishTurn(ServerLevel level, BoardSession session) {
        BoardParticipant participant = session.currentParticipant().orElse(null);
        if (participant != null) {
            BoardParticipant ended = participant.endTurn();
            session.putParticipant(ended);
            syncEntityState(level, ended);
        }

        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        int previousRound = session.round();
        session.advanceTurn();
        if (session.round() != previousRound) {
            applyRoundRewards(level, session, session.round() + 1);
        }
        markChanged(level);
        beginCurrentTurn(level, session);
    }

    private static void applyRoundRewards(ServerLevel level, BoardSession session, int roundNumber) {
        boolean standardReward = roundNumber == 2 || roundNumber >= 6 && roundNumber % 6 == 0;
        boolean lateReward = roundNumber >= 15 && (roundNumber - 15) % 6 == 0;
        if (!standardReward && !lateReward) return;
        int rewardEvents = (standardReward ? 1 : 0) + (lateReward ? 1 : 0);
        List<UUID> order = session.turnOrder();
        for (int orderIndex = 0; orderIndex < order.size(); orderIndex++) {
            BoardParticipant participant = session.participant(order.get(orderIndex)).orElse(null);
            if (participant == null) continue;
            int coinsPerReward = roundNumber >= 15 ? 7 + Math.min(orderIndex, 3) : 5;
            BoardParticipant updated = participant;
            int totalCards = 0;
            for (int eventIndex = 0; eventIndex < rewardEvents; eventIndex++) {
                updated = updated.withStats(updated.stats().addCoins(coinsPerReward));
                Optional<Identifier> card = randomCardId(level, BoardSessionManager::validPvpCard);
                if (card.isPresent()) {
                    updated = updated.addCard(card.get());
                    totalCards++;
                }
            }

            updateParticipant(level, session, updated);
            int totalCoins = coinsPerReward * rewardEvents;
            int finalTotalCards = totalCards;
            updated.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                    player.sendSystemMessage(Component.translatable(
                            "message.astral_craft.board.round_reward", roundNumber, totalCoins, finalTotalCards)
                            .withStyle(ChatFormatting.GOLD), true));
        }
    }

    private static void beginEncounter(ServerLevel level, BoardSession session, BoardParticipant mover, BoardParticipant target) {
        int durationTicks = mover.decisionDurationTicks(ENCOUNTER_TIMEOUT_TICKS);
        session.setEncounter(new BoardSession.EncounterState(mover.slotUuid(), target.slotUuid(),
                level.getGameTime() + durationTicks, durationTicks));
        if (isAutomated(level, mover)) {
            session.setEncounter(null);
            BoardBattleService.start(level, session, mover, target);
            return;
        }

        mover.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player -> {
            AstralCharacterEntity targetEntity = entity(level, target);
            String name = displayName(level, target);
            PacketDistributor.sendToPlayer(player, new OpenBoardEncounterPayload(session.id().toString(),
                    targetEntity == null ? -1 : targetEntity.getId(), name, durationTicks, durationTicks,
                    mover.characterId(), mover.skinId()));
        });
    }

    public static void resumeAfterBattle(ServerLevel level, BoardSession session) {
        session.setEncounter(null);
        continueArrival(level, session);
        markChanged(level);
    }

    private static void resumeAfterEncounter(ServerLevel level, BoardSession session) {
        continueArrival(level, session);
    }

    private static void randomDiscard(ServerLevel level, BoardSession session) {
        BoardSession.DiscardState discard = session.discard();
        if (discard == null) return;
        BoardParticipant participant = session.participant(discard.slotId()).orElse(null);
        if (participant == null) {
            session.setDiscard(null);
            finishTurn(level, session);
            return;
        }

        List<Identifier> next = new ArrayList<>(participant.hand());
        for (int i = 0; i < discard.requiredCount() && !next.isEmpty(); i++) {
            next.remove(level.getRandom().nextInt(next.size()));
        }

        updateParticipant(level, session, participant.withHand(next));
        session.setDiscard(null);
        finishTurn(level, session);
    }

    private static void applyPanel(ServerLevel level, BoardSession session, BoardParticipant participant, boolean landing) {
        BoardNode node = session.nodes().get(participant.currentNodeKey());
        BasePlatform platform = platform(node);
        if (platform == null || !platform.triggers(landing)) return;
        platform.applyBoardEffect(new BoardPanelContext(level, session, participant, node, landing));
        BoardParticipant updated = session.participant(participant.slotUuid()).orElse(participant);
        AstralCharacterEntity entity = entity(level, updated);
        if (entity != null && updated.stats().health() <= 0) onParticipantDamaged(entity, updated.stats());
    }

    public static void relocateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant, String destinationNodeId) {
        if (!session.nodes().containsKey(destinationNodeId)) return;
        String previousNodeId = participant.currentNodeKey();
        BoardParticipant relocated = participant.withNode(participant.currentNodeId(),
                BoardParticipant.nodeIdentifier(destinationNodeId), session.nextArrivalOrder());
        updateParticipant(level, session, relocated);
        arrangeNode(level, session, previousNodeId);
        arrangeNode(level, session, destinationNodeId);
    }

    public static void resumeMovementAfterPanel(ServerLevel level, BoardSession session) {
        continueArrival(level, session);
        markChanged(level);
    }

    private static @Nullable BasePlatform platform(@Nullable BoardNode node) {
        if (node == null) return null;
        return BuiltInRegistries.BLOCK.getValue(node.platformId()) instanceof BasePlatform platform ? platform : null;
    }

    public static boolean isAutomated(ServerLevel level, BoardParticipant participant) {
        return participant.bot() || participant.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null) == null;
    }

    private static void fillBots(ServerLevel level, BoardSession session) {
        List<CharacterDefinition> available = CharacterManager.INSTANCE.values().stream()
                .filter(definition -> !session.hasCharacter(definition.id())).toList();
        List<CharacterDefinition> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, new Random(level.getRandom().nextLong()));
        int index = 0;
        while (session.participantCount() < REQUIRED_PLAYERS && index < shuffled.size()) {
            CharacterDefinition definition = shuffled.get(index++);
            String skinId = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
            BoardParticipant bot = new BoardParticipant(UUID.randomUUID(), Optional.empty(), true,
                    definition.id(), BoardParticipant.skinIdentifier(definition.id(), skinId),
                    BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID, Optional.empty(),
                    AstralPlayerStats.DEFAULT, List.of(), Map.of(), 0, 0, 0, 7, session.nextArrivalOrder());
            session.putParticipant(bot);
        }
    }

    private static void ensureEntities(ServerLevel level, BoardSession session) {
        Set<String> occupiedNodes = new LinkedHashSet<>();
        for (BoardParticipant participant : session.participants()) {
            BlockPos pos = session.positions().get(participant.currentNodeKey());
            if (pos == null || !level.hasChunkAt(pos)) continue;
            if (entity(level, participant) == null) {
                spawnCharacter(level, session, participant);
            }
            occupiedNodes.add(participant.currentNodeKey());
        }

        if (session.movement() == null) {
            occupiedNodes.forEach(nodeId -> arrangeNode(level, session, nodeId));
        }
    }

    private static void spawnCharacter(ServerLevel level, BoardSession session, BoardParticipant participant) {
        BlockPos pos = session.positions().get(participant.currentNodeKey());
        if (pos == null || !level.hasChunkAt(pos) || entity(level, participant) != null) return;
        AstralCharacterEntity entity = AstralEntities.ASTRAL_CHARACTER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) return;
        entity.setCharacterId(participant.characterId());
        entity.setSkinId(participant.skinName());
        entity.setStarCoins(participant.stats().starCoins());
        entity.setBoardSessionId(session.id());
        entity.setBoardParticipantId(participant.slotUuid());
        entity.setCustomName(Component.translatable(CharacterManager.INSTANCE.get(participant.characterId()).nameKey()));
        entity.setCustomNameVisible(false);
        AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(participant.stats().maxHealth());
        }

        entity.setHealth(Math.max(1.0F, participant.stats().health()));
        entity.setPos(pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D);
        BoardNode spawnNode = session.nodes().get(participant.currentNodeKey());
        if (spawnNode != null && !spawnNode.next().isEmpty()) {
            BlockPos firstTarget = session.positions().get(spawnNode.next().getFirst());
            if (firstTarget != null) entity.setBoardDirection(directionBetween(pos, firstTarget));
        }

        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        BoardParticipant spawned = participant.withEntity(entity.getUUID());
        session.putParticipant(spawned);
        syncEntityState(level, spawned);
        arrangeNode(level, session, participant.currentNodeKey());
        markChanged(level);
    }

    private static void syncEntityState(ServerLevel level, BoardParticipant participant) {
        AstralCharacterEntity entity = entity(level, participant);
        if (entity == null) return;
        entity.setStarCoins(participant.stats().starCoins());
        AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(participant.stats().maxHealth());
        }

        entity.setHealth(Math.max(participant.stats().health() <= 0 ? 1.0F : participant.stats().health(), 1.0F));
        if (participant.knockedDownTurns() > 0 || participant.stats().health() <= 0) {
            entity.setAnimationAction("knockdown");
        } else if ("knockdown".equals(entity.animationAction())) {
            entity.setAnimationAction("idle");
        }
        AstralCharacterSkillEffects.synchronizeRoundEffects(entity, participant.roundStatusEffects());
    }

    private static void arrangeNode(ServerLevel level, BoardSession session, String nodeId) {
        BlockPos pos = session.positions().get(nodeId);
        if (pos == null) return;
        List<BoardParticipant> occupants = session.participants().stream()
                .filter(participant -> participant.currentNodeKey().equals(nodeId))
                .sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
        BoardSession.MovementState movement = session.movement();
        for (int index = 0; index < occupants.size(); index++) {
            BoardParticipant participant = occupants.get(index);
            AstralCharacterEntity entity = entity(level, participant);
            if (entity == null || (movement != null
                    && movement.slotId().equals(participant.slotUuid())
                    && movement.stepping())) continue;
            double angle = occupants.size() == 1 ? 0.0D : Math.PI * 2.0D * index / occupants.size();
            double radius = occupants.size() == 1 ? 0.0D : Math.min(0.34D, 0.12D + occupants.size() * 0.035D);
            entity.setPos(pos.getX() + 0.5D + Math.cos(angle) * radius,
                    pos.getY() + 0.12D,
                    pos.getZ() + 0.5D + Math.sin(angle) * radius);
        }
    }

    public static void clearRuntimeEntities(ServerLevel level, BoardSession session) {
        for (BoardParticipant participant : session.participants()) {
            AstralCharacterEntity entity = entity(level, participant);
            if (entity != null) entity.discard();
        }
    }

    private static BoardParticipant firstEncounterTarget(BoardSession session, BoardParticipant mover, Set<UUID> resolvedSlots) {
        if (session.startNodes().contains(mover.currentNodeKey())) return null;
        return session.participants().stream()
                .filter(participant -> !participant.slotUuid().equals(mover.slotUuid()))
                .filter(participant -> !resolvedSlots.contains(participant.slotUuid()))
                .filter(participant -> participant.currentNodeKey().equals(mover.currentNodeKey()))
                .filter(participant -> !participant.knockedDown())
                .min(Comparator.comparingInt(BoardParticipant::arrivalOrder)).orElse(null);
    }

    private static List<String> nextChoices(BoardSession session, BoardParticipant participant) {
        BoardNode node = session.nodes().get(participant.currentNodeKey());
        if (node == null || node.next().isEmpty()) return List.of();
        List<String> choices = new ArrayList<>(node.next());
        if (!participant.hasPreviousNode() && choices.size() > 1) {
            return List.of(choices.getFirst());
        }

        if (participant.hasPreviousNode() && choices.size() > 1) {
            choices.remove(participant.previousNodeKey());
        }

        return choices.isEmpty() ? node.next() : List.copyOf(choices);
    }

    static int graphDistance(BoardSession session, String start, String target, int maximum) {
        if (start.equals(target)) return 0;
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> distances = new HashMap<>();
        queue.add(start);
        distances.put(start, 0);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int distance = distances.get(current);
            if (distance >= maximum) continue;
            BoardNode node = session.nodes().get(current);
            if (node == null) continue;
            for (String next : node.next()) {
                if (distances.containsKey(next)) continue;
                int nextDistance = distance + 1;
                if (next.equals(target)) return nextDistance;
                distances.put(next, nextDistance);
                queue.add(next);
            }
        }

        return -1;
    }

    private static void broadcastRoutePreview(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) {
            broadcastRouteState(session, false, "", "", "");
            return;
        }

        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null) return;
        List<List<String>> paths = possiblePaths(session, participant.currentNodeKey(),
                participant.previousNodeKey(), movement.remainingSteps());
        List<List<String>> highlightedPaths = startOpportunityPaths(session, participant, paths);
        String route = encodePaths(session, paths);
        String highlightedRoute = encodePaths(session, highlightedPaths);
        String branches = encodeNodePositions(session, movement.branchChoices());
        broadcastRouteState(session, true, route, highlightedRoute, branches);
    }

    private static List<List<String>> possiblePaths(BoardSession session, String start, String previous, int steps) {
        List<List<String>> result = new ArrayList<>();
        collectPaths(session, start, previous, steps, new ArrayList<>(List.of(start)), result);
        return result;
    }

    private static List<List<String>> startOpportunityPaths(BoardSession session, BoardParticipant participant, List<List<String>> paths) {
        Set<String> startNodes = Set.copyOf(session.startNodes());
        return paths.stream().filter(path -> {
            if (path.size() < 2) return false;
            if (startNodes.contains(path.getLast())) return true;
            return path.subList(1, path.size()).stream()
                    .anyMatch(nodeId -> session.canStopAtStart(participant, nodeId));
        }).toList();
    }

    private static void collectPaths(BoardSession session, String current, String previous, int remaining, List<String> path, List<List<String>> result) {
        if (result.size() >= MAX_ROUTE_BRANCHES) return;
        if (remaining <= 0) {
            result.add(List.copyOf(path));
            return;
        }

        BoardNode node = session.nodes().get(current);
        if (node == null || node.next().isEmpty()) {
            result.add(List.copyOf(path));
            return;
        }

        List<String> choices = new ArrayList<>(node.next());
        if ((previous == null || previous.isBlank()) && choices.size() > 1) {
            choices = new ArrayList<>(List.of(choices.getFirst()));
        } else if (previous != null && !previous.isBlank() && choices.size() > 1) {
            choices.remove(previous);
        }

        for (String next : choices) {
            path.add(next);
            collectPaths(session, next, current, remaining - 1, path, result);
            path.removeLast();
            if (result.size() >= MAX_ROUTE_BRANCHES) break;
        }
    }

    private static String encodePaths(BoardSession session, List<List<String>> paths) {
        StringJoiner routes = new StringJoiner("|");
        for (List<String> path : paths) routes.add(encodeNodePositions(session, path));
        return routes.toString();
    }

    private static String encodeNodePositions(BoardSession session, List<String> nodeIds) {
        StringJoiner joiner = new StringJoiner(";");
        for (String nodeId : nodeIds) {
            BlockPos pos = session.positions().get(nodeId);
            if (pos != null) joiner.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }

        return joiner.toString();
    }

    public static void broadcastRouteState(BoardSession session, boolean active, String route, String highlightedRoute, String branches) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(session.dimension());
        if (level == null) return;
        BoardSession.MovementState movement = session.movement();
        BoardParticipant participant = movement == null ? null : session.participant(movement.slotId()).orElse(null);
        int decisionTicks = active && movement != null && !movement.branchChoices().isEmpty()
                ? (int) Math.max(0L, movement.nextStepTick() - level.getGameTime()) : 0;
        int decisionDurationTicks = active && participant != null && !movement.branchChoices().isEmpty()
                ? participant.decisionDurationTicks(BRANCH_TIMEOUT_TICKS) : 1;
        Identifier characterId = participant == null ? AstralCraft.prefix("mimi") : participant.characterId();
        Identifier skinId = participant == null ? Identifier.withDefaultNamespace("default") : participant.skinId();
        BoardRouteStatePayload payload = new BoardRouteStatePayload(session.id().toString(), route,
                highlightedRoute, branches, decisionTicks, decisionDurationTicks, characterId, skinId, active);
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D,
                    center.getZ() + 0.5D) <= 160.0D * 160.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void syncBoardSnapshot(ServerLevel level, BoardSession session) {
        String encoded = BoardHudSyncManager.encode(level, session);
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= 128.0D * 128.0D) {
                PacketDistributor.sendToPlayer(player, new BoardHudSnapshotPayload(encoded));
            }
        }
    }

    public static void registerLobbyViewer(ServerPlayer player, BoardSession session) {
        LOBBY_VIEWERS.computeIfAbsent(session.id(), ignored -> new LinkedHashSet<>()).add(player.getUUID());
        sendCharacterSelection(player, session, false);
    }

    public static void sendCharacterSelection(ServerPlayer player, BoardSession session, boolean refresh) {
        BoardParticipant selected = session.participantByController(player.getUUID()).orElse(null);
        List<Identifier> occupiedCharacters = new ArrayList<>();
        for (BoardParticipant value : session.participants()) {
            if (selected == null || !value.slotUuid().equals(selected.slotUuid())) {
                occupiedCharacters.add(value.characterId());
            }
        }

        CharacterDefinition fallback = CharacterManager.INSTANCE.defaultCharacter();
        String fallbackSkin = fallback.skins().isEmpty() ? "default" : fallback.skins().getFirst().id();
        int remaining = (int) Math.clamp(session.lobbyDeadlineTick() - player.level().getGameTime(), 0L, LOBBY_TIMEOUT_TICKS);
        PacketDistributor.sendToPlayer(player, new OpenBoardCharacterSelectionPayload(session.id().toString(),
                CharacterManager.INSTANCE.encodeList(), List.copyOf(occupiedCharacters),
                selected == null ? fallback.id() : selected.characterId(),
                selected == null ? BoardParticipant.skinIdentifier(fallback.id(), fallbackSkin) : selected.skinId(),
                remaining, LOBBY_TIMEOUT_TICKS, refresh));
    }

    private static void refreshLobbyScreens(ServerLevel level, BoardSession session) {
        Set<UUID> viewers = LOBBY_VIEWERS.get(session.id());
        if (viewers == null || viewers.isEmpty()) return;
        viewers.removeIf(viewerId -> {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer == null || viewer.level() != level) return true;
            sendCharacterSelection(viewer, session, true);
            return false;
        });

        if (viewers.isEmpty()) LOBBY_VIEWERS.remove(session.id());
    }

    private static void removeLobbyViewer(UUID boardId, UUID playerId) {
        Set<UUID> viewers = LOBBY_VIEWERS.get(boardId);
        if (viewers == null) return;
        viewers.remove(playerId);
        if (viewers.isEmpty()) LOBBY_VIEWERS.remove(boardId);
    }

    private static void closeLobbyScreens(ServerLevel level, UUID boardId) {
        Set<UUID> viewers = LOBBY_VIEWERS.remove(boardId);
        if (viewers == null) return;
        CharacterDefinition fallback = CharacterManager.INSTANCE.defaultCharacter();
        String fallbackSkin = fallback.skins().isEmpty() ? "default" : fallback.skins().getFirst().id();
        OpenBoardCharacterSelectionPayload closePayload = new OpenBoardCharacterSelectionPayload(
                boardId.toString(), "", List.of(), fallback.id(),
                BoardParticipant.skinIdentifier(fallback.id(), fallbackSkin), 0, LOBBY_TIMEOUT_TICKS, false);
        for (UUID viewerId : viewers) {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) PacketDistributor.sendToPlayer(viewer, closePayload);
        }
    }

    public static void resetForLobby(ServerLevel level, BoardSession session) {
        LOBBY_VIEWERS.remove(session.id());
        BoardBattleService.cancel(session.id());
        PENDING_BOT_EFFECTS.remove(session.id());
        PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BasePlatform.clearActiveBoardEffect(session.id());
        broadcastRouteState(session, false, "", "", "");
        clearRuntimeEntities(level, session);
        session.clearParticipants();
        session.setPhase(BoardPhase.READY);
        session.setLobbyDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        session.setProtectionEnabled(true);
        markChanged(level);
        refreshProtectedAreas(level, data(level));
    }

    public static void updateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant) {
        BoardParticipant previous = session.participant(participant.slotUuid()).orElse(null);
        boolean damaged = previous != null && participant.stats().health() < previous.stats().health();
        boolean newlyKnockedDown = participant.knockedDownTurns() > 0
                && (previous == null || previous.knockedDownTurns() <= 0);
        session.putParticipant(participant);
        syncEntityState(level, participant);
        AstralCharacterEntity entity = entity(level, participant);
        if (entity != null && damaged) {
            int logicalDamage = Math.max(0, previous.stats().health() - participant.stats().health());
            SoulLinkManager.mirrorLogicalDamage(level, entity, logicalDamage);
            if (newlyKnockedDown || participant.stats().health() <= 0) {
                entity.flashBoardDamage(10);
                entity.setAnimationAction("knockdown");
            } else {
                entity.playBoardHurtAnimation(10);
            }
        }

        markChanged(level);
    }

    private static boolean hasOnlineHumanParticipant(ServerLevel level, BoardSession session) {
        return session.participants().stream().anyMatch(participant -> participant.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).isPresent());
    }

    public static List<ServerPlayer> humanPlayers(ServerLevel level, BoardSession session) {
        List<ServerPlayer> result = new ArrayList<>();
        for (BoardParticipant participant : session.participants()) {
            participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(result::add);
        }

        return List.copyOf(result);
    }

    public static Optional<BoardSession> session(ServerLevel level, UUID id) {
        return Optional.ofNullable(data(level).get(id));
    }

    public static Optional<ResourceKey<Level>> dimension(UUID boardId) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Optional.empty();
        for (ServerLevel level : server.getAllLevels()) {
            BoardSession session = data(level).get(boardId);
            if (session != null) return Optional.of(session.dimension());
        }

        return Optional.empty();
    }

    public static int entityId(ServerLevel level, BoardParticipant participant) {
        AstralCharacterEntity entity = entity(level, participant);
        return entity == null ? -1 : entity.getId();
    }

    public static int revealSourceEntityId(ServerPlayer player) {
        BoardSession session = findByController(player).orElse(null);
        if (session == null) return player.getId();
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        int entityId = participant == null ? -1 : entityId(player.level(), participant);
        return entityId < 0 ? player.getId() : entityId;
    }

    public static LivingEntity effectSourceEntity(ServerPlayer player) {
        BoardSession session = findByController(player).orElse(null);
        if (session == null) return player;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        AstralCharacterEntity pawn = participant == null ? null : entity(player.level(), participant);
        return pawn == null ? player : pawn;
    }

    public static String displayName(ServerLevel level, BoardParticipant participant) {
        ServerPlayer controller = participant.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (controller != null) return controller.getGameProfile().name();
        if (!participant.bot()) return Component.translatable("gui.astral_craft.board.player").getString();
        BoardSession session = sessions(level).stream()
                .filter(value -> value.participant(participant.slotUuid()).isPresent())
                .findFirst().orElse(null);
        if (session == null) return Component.translatable("gui.astral_craft.board.bot").getString();
        List<BoardParticipant> bots = session.participants().stream().filter(BoardParticipant::bot).toList();
        int index = 1;
        for (BoardParticipant bot : bots) {
            if (bot.slotUuid().equals(participant.slotUuid())) break;
            index++;
        }
        return Component.translatable("gui.astral_craft.board.bot_numbered", index).getString();
    }

    private record PendingGravityRestore(BlockState state, long restoreAfterTick) {}

    private static boolean isSoloIntegratedServer(ServerPlayer player) {
        return player.server.isSingleplayer() && player.server.getPlayerList().getPlayerCount() <= 1;
    }

    private static BoardParticipant currentControlledParticipant(BoardSession session, ServerPlayer player) {
        BoardParticipant current = session.currentParticipant().orElse(null);
        return current != null && current.controlledBy(player.getUUID()) ? current : null;
    }

    static @Nullable AstralCharacterEntity entity(ServerLevel level, BoardParticipant participant) {
        return participant.entityUuid().map(level::getEntity)
                .filter(AstralCharacterEntity.class::isInstance)
                .map(AstralCharacterEntity.class::cast).orElse(null);
    }

    private static Direction directionBetween(BlockPos from, BlockPos to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dz = Integer.compare(to.getZ(), from.getZ());
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (dz != 0) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return Direction.NORTH;
    }

    private static List<Identifier> randomInitialHand(ServerLevel level, int count) {
        List<Identifier> cards = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            randomCardId(level, BoardSessionManager::validPvpCard).ifPresent(cards::add);
        }
        return List.copyOf(cards);
    }

    public static boolean validPvpCard(BaseHandCard card) {
        ItemStack stack = new ItemStack(card);
        CardDefinition definition = card.definition(stack);
        if (definition.type() != CardType.ATTACK && definition.type() != CardType.DEFENSE) return true;
        CombatBonusDefinition bonus = stack.get(AstralDataComponents.COMBAT_BONUS);
        return bonus != null && bonus.standardPvp();
    }

    private static Optional<Identifier> randomCardId(ServerLevel level, Predicate<BaseHandCard> filter) {
        List<Identifier> candidates = new ArrayList<>();
        for (AstralItems.ModelledCardItem entry : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = entry.item().get();
            if (!(item instanceof BaseHandCard card) || !filter.test(card)) continue;
            Package itemPackage = item.getClass().getPackage();
            if (itemPackage != null && itemPackage.getName().contains(".cards.pve")) continue;
            candidates.add(BuiltInRegistries.ITEM.getKey(item));
        }

        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(level.getRandom().nextInt(candidates.size())));
    }

    private static String encodeHand(List<Identifier> hand) {
        return hand.stream().map(Identifier::toString).reduce((left, right) -> left + ";" + right).orElse("");
    }

    private static BoardSession session(ServerLevel level, String rawId) {
        try {
            return data(level).get(UUID.fromString(rawId));
        } catch (Exception exception) {
            return null;
        }
    }

    private static BoardSavedData data(ServerLevel level) {
        return BoardSavedData.get(level);
    }

    public static void markChanged(ServerLevel level) {
        data(level).markChanged();
    }

    private record PendingBotEffect(UUID userSlotId, Identifier cardId, List<UUID> targetSlotIds, long executeTick) {
        private PendingBotEffect {
            targetSlotIds = List.copyOf(targetSlotIds);
        }
    }

}