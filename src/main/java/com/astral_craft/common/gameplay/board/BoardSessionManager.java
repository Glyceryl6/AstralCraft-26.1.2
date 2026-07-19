package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CombatBonusDefinition;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import com.astral_craft.common.gameplay.dice.AstralDiceRollService;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.items.cards.HandcardRedirection;
import com.astral_craft.common.items.cards.pvp.HandcardSoulLink;
import com.astral_craft.common.network.BoardCardView;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.s2c.*;
import com.astral_craft.common.registry.AstralDataComponents;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    public static final int PVP_INITIAL_STAR_COINS = 6;
    public static final int LOBBY_TIMEOUT_TICKS = 20 * 25;
    public static final int TURN_TIMEOUT_TICKS = 20 * 45;
    public static final int DISCARD_TIMEOUT_TICKS = 20 * 20;
    public static final int ENCOUNTER_TIMEOUT_TICKS = 20 * 12;
    public static final int BRANCH_TIMEOUT_TICKS = 20 * 12;
    public static final int MOVEMENT_STEP_TICKS = 3;
    private static final int MOVEMENT_GAP_TICKS = 1;
    public static final int MAX_ROUTE_BRANCHES = 96;
    public static final Identifier ALL_OR_NOTHING_STATUS = AstralCraft.prefix("all_or_nothing");
    private static final Map<UUID, PendingBotEffect> PENDING_BOT_EFFECTS = new HashMap<>();
    private static final Map<UUID, PendingTimeBombRoll> PENDING_TIME_BOMB_ROLLS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_BOT_MOVEMENT_TICKS = new HashMap<>();
    private static final Map<UUID, Long> NO_HUMAN_SINCE_TICKS = new HashMap<>();

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
                .filter(session -> session.participantFor(entity).isPresent())
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
        return findByEntity(entity).flatMap(session -> session.participantFor(entity));
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
        applyStatsWithCoinAnimation(player.level(), session, participant, stats);
        return true;
    }

    public static boolean setStatsForEntity(AstralCharacterEntity entity, AstralPlayerStats stats) {
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty() || !(entity.level() instanceof ServerLevel level)) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantFor(entity).orElse(null);
        if (participant == null) return false;
        applyStatsWithCoinAnimation(level, session, participant, stats);
        return true;
    }

    private static void applyStatsWithCoinAnimation(ServerLevel level, BoardSession session, BoardParticipant participant, AstralPlayerStats stats) {
        int gainedCoins = Math.max(0, stats.starCoins() - participant.stats().starCoins());
        AstralPlayerStats immediateStats = gainedCoins > 0 ? stats.spendCoins(gainedCoins) : stats;
        updateParticipant(level, session, participant.withStats(immediateStats));
        if (gainedCoins > 0) {
            BoardWorldObjectService.awardCoins(level, session, participant.slotUuid(), gainedCoins);
        }
    }

    public static void reduceSkillCooldown(ServerPlayer player, int turns) {
        if (player == null || turns <= 0) return;
        Optional<BoardSession> maybeSession = findByController(player);
        if (maybeSession.isEmpty()) return;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null) return;
        updateParticipant(player.level(), session, participant.withSkillCooldown(
                Math.max(0, participant.skillCooldownTurns() - turns)));
    }

    public static boolean addRoundStatusEffect(AstralCharacterEntity entity, Identifier statusId, int turns) {
        if (entity == null || statusId == null || turns <= 0 || !(entity.level() instanceof ServerLevel level)) return false;
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantFor(entity).orElse(null);
        if (participant == null) return false;
        updateParticipant(level, session, participant.withRoundStatusEffect(statusId, turns));
        return true;
    }

    public static Optional<Identifier> selectedCharacterForController(ServerPlayer player) {
        return findByController(player).flatMap(session -> session.participantByController(player.getUUID())).map(BoardParticipant::characterId);
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
        if (PendingCardActionManager.hasBoardCardUi(player)) return false;
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantFor(entity).orElse(null);
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

    private static void sendTurnScreen(ServerPlayer player, BoardSession session, BoardParticipant participant, AstralCharacterEntity entity, boolean currentTurn) {
        int durationTicks = currentTurn ? Math.max(1, session.actionDurationTicks()) : 1;
        int remainingTicks = currentTurn ? (int) Math.clamp(
                session.actionDeadlineTick() - player.level().getGameTime(), 0L, durationTicks) : 0;
        PacketDistributor.sendToPlayer(player, new OpenBoardTurnPayload(session.id(), entity.getId(),
                turnCardViews(player, participant), participant.cardPlaysUsed(), participant.stats().cardPlaysPerTurn(),
                participant.skillCooldownTurns(), remainingTicks, durationTicks,
                participant.characterId(), participant.skinId(), currentTurn, false));
    }

    public static void leaveGame(ServerPlayer player, UUID boardId) {
        BoardSession session = session(player.level(), boardId).orElse(null);
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

    public static void requestMove(ServerPlayer player, UUID boardId) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING || session.movement() != null
                || session.encounter() != null || session.discard() != null
                || BoardBattleService.active(session.id())
                || PendingCardActionManager.isExclusiveBusy(player)
                || PendingCardActionManager.hasPendingSelection(player)
                || PendingCardActionManager.hasBoardCardUi(player)) return;
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || participant.knockedDown()) return;
        AstralCharacterEntity entity = BoardEntityService.entity(player.level(), participant);
        if (entity == null) return;
        participant = participant.recordManualDecision();
        updateParticipant(player.level(), session, participant);
        beginMoveRoll(player.level(), session, participant, player);
    }

    private static void beginTimedOutHumanMovement(ServerLevel level, BoardSession session, BoardParticipant participant, ServerPlayer controller) {
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
        AstralCharacterEntity entity = BoardEntityService.entity(level, current);
        if (entity == null) {
            finishTurn(level, session);
            return;
        }

        beginMoveRoll(level, session, current, controller);
    }

    public static void requestSkill(ServerPlayer player, UUID boardId) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING || BoardBattleService.active(session.id())
                || PendingCardActionManager.isExclusiveBusy(player)
                || PendingCardActionManager.hasPendingSelection(player)
                || PendingCardActionManager.hasBoardCardUi(player)) return;
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || participant.skillCooldownTurns() > 0 || session.movement() != null
                || session.encounter() != null || session.discard() != null) return;
        AstralCharacterEntity entity = BoardEntityService.entity(player.level(), participant);
        if (entity == null) return;
        int cooldown = AstralCharacterSkillService.useActiveSkillForBoard(player, entity,
                participant.characterId(), participant.skinName(), humanPlayers(player.level(), session));
        if (cooldown < 0) return;
        BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
        BoardParticipant updated = refreshed.recordManualDecision().withSkillCooldown(cooldown);
        updateParticipant(player.level(), session, updated);
        sendTurnScreen(player, session, updated, entity);
    }

    public static void chooseEncounter(ServerPlayer player, UUID boardId, boolean challenge) {
        BoardSession session = session(player.level(), boardId).orElse(null);
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

    public static void discard(ServerPlayer player, UUID boardId, List<Integer> indexes) {
        BoardSession session = session(player.level(), boardId).orElse(null);
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

    public static boolean canUseBoardCard(ServerPlayer player, UUID boardId, int index) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING) return false;
        BoardParticipant participant = currentControlledParticipant(session, player);
        return participant != null && !PendingCardActionManager.hasBoardCardUi(player)
                && session.movement() == null && session.encounter() == null
                && session.discard() == null && participant.stats().cardPlaysRemaining() > 0
                && index >= 0 && index < participant.hand().size();
    }

    public static void consumeBoardCard(ServerPlayer player, int index) {
        Optional<BoardSession> maybeSession = findByController(player);
        if (maybeSession.isEmpty()) return;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || index < 0 || index >= participant.hand().size()) return;
        updateParticipant(player.level(), session, participant.recordManualDecision().removeCard(index).useCardPlay());
    }

    public static boolean consumeCounterCard(ServerPlayer player, UUID boardId, int index) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING) return false;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null || index < 0 || index >= participant.hand().size()) return false;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
        if (!(item instanceof BaseHandCard card)) return false;
        ItemStack stack = new ItemStack(item);
        if (card.definition(stack).type() != CardType.COUNTER) return false;
        updateParticipant(player.level(), session, participant.recordManualDecision().removeCard(index));
        return true;
    }

    public static boolean consumeCounterCard(ServerLevel level, BoardSession session,
                                             BoardParticipant participant, int index) {
        if (level == null || session == null || participant == null
                || index < 0 || index >= participant.hand().size()) return false;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
        if (!(item instanceof BaseHandCard card)) return false;
        ItemStack stack = new ItemStack(item);
        if (card.definition(stack).type() != CardType.COUNTER) return false;
        updateParticipant(level, session, participant.recordManualDecision().removeCard(index));
        return true;
    }

    public static void openCounterScreen(ServerPlayer player, BoardSession session, BoardParticipant participant, int responseTicks) {
        AstralCharacterEntity entity = BoardEntityService.entity(player.level(), participant);
        if (entity == null) return;
        List<BoardCardView> counterCards = cardViews(participant.hand()).stream()
                .filter(view -> view.stack().getItem() instanceof BaseHandCard card
                        && card.definition(view.stack()).type() == CardType.COUNTER)
                .toList();
        int duration = Math.max(1, responseTicks);
        PacketDistributor.sendToPlayer(player, new OpenBoardTurnPayload(session.id(), entity.getId(),
                counterCards, participant.cardPlaysUsed(), participant.stats().cardPlaysPerTurn(),
                participant.skillCooldownTurns(), duration, duration, participant.characterId(), participant.skinId(),
                false, true));
    }

    public static void resumeAfterCardUi(ServerPlayer player, UUID boardId, long pausedTicks) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING) return;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null) return;
        if (pausedTicks > 0L && session.actionDeadlineTick() > 0L
                && session.currentParticipant().map(value -> value.slotUuid().equals(participant.slotUuid())).orElse(false)) {
            session.setActionDeadlineTick(session.actionDeadlineTick() + pausedTicks);
            markChanged(player.level());
        }
        reopenTurnScreen(player, boardId);
    }

    public static void reopenTurnScreen(ServerPlayer player, UUID boardId) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING) return;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null) return;
        AstralCharacterEntity entity = BoardEntityService.entity(player.level(), participant);
        if (entity == null) return;
        boolean currentTurn = session.currentParticipant().map(value -> value.slotUuid().equals(participant.slotUuid())).orElse(false)
                && session.movement() == null && session.encounter() == null && session.discard() == null
                && !BoardBattleService.active(session.id());
        sendTurnScreen(player, session, participant, entity, currentTurn);
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
            AstralCharacterEntity entity = BoardEntityService.entity(user.level(), target);
            if (entity == null || !acceptsBoardPawn(definition) || !card.canTarget(user, entity, stack)) continue;
            int distance = BoardRouteService.graphDistance(session, source.currentNodeKey(), target.currentNodeKey(), range);
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
        BoardParticipant selected = session.participantFor(character).orElse(null);
        if (source == null || selected == null || selected.stats().health() <= 0) return false;
        if (source.slotUuid().equals(selected.slotUuid()) && !card.allowsSelfTarget()) return false;
        if (!card.canTarget(user, character, stack)) return false;
        int range = Math.max(0, effectiveRange);
        int distance = BoardRouteService.graphDistance(session, source.currentNodeKey(), selected.currentNodeKey(), range);
        return distance >= 0 && distance <= range;
    }

    public static boolean damageFromEffect(ServerLevel level, BoardSession session, UUID slotId, int damage) {
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null || participant.knockedDown() || damage <= 0) return false;
        AstralPlayerStats damagedStats = participant.stats().damage(damage);
        BoardParticipant updated = participant.withStats(damagedStats);
        if (damagedStats.health() <= 0) {
            int lostCoins = Math.max(0, (participant.stats().starCoins() + 1) / 2);
            updated = updated.knockDown();
            BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), lostCoins);
        }
        updateParticipant(level, session, updated);
        return updated.knockedDown();
    }

    public static void onParticipantDamaged(AstralCharacterEntity entity, AstralPlayerStats stats) {
        if (stats.health() > 0) return;
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty() || !(entity.level() instanceof ServerLevel level)) return;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantFor(entity).orElse(null);
        if (participant == null || participant.knockedDownTurns() > 0) return;
        int lostCoins = Math.max(0, (participant.stats().starCoins() + 1) / 2);
        BoardParticipant knocked = participant.knockDown();
        BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), lostCoins);
        updateParticipant(level, session, knocked);
        entity.setAnimationAction("knockdown");
        participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                player.sendSystemMessage(Component.translatable("message.astral_craft.knockdown",
                        Math.max(0, (participant.stats().starCoins() + 1) / 2)), true));
    }

    public static void endGame(ServerLevel level, BoardSession session, boolean keepBoard) {
        BoardBattleService.cancel(session.id());
        BoardPanelSelectionService.clear(session.id());
        PENDING_BOT_EFFECTS.remove(session.id());
        PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
        PENDING_TIME_BOMB_ROLLS.remove(session.id());
        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BasePlatform.clearActiveBoardEffect(session.id());
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        BoardWorldObjectService.clear(level, session);
        BoardEntityService.clearRuntimeEntities(level, session);
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
        BoardProtectionService.refreshProtectedAreas(level, savedData);
    }

    public static void serverTick(MinecraftServer server) {
        Set<ResourceKey<Level>> activeDimensions = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            activeDimensions.add(level.dimension());
            BoardSavedData savedData = data(level);
            BoardProtectionService.tickLevel(level, savedData);
            for (BoardSession session : savedData.sessions()) {
                tickSession(level, session);
            }
        }

        BoardProtectionService.retainDimensions(activeDimensions);
    }

    private static void tickSession(ServerLevel level, BoardSession session) {
        if (session.phase() == BoardPhase.CHARACTER_SELECTION) {
            if (session.lobbyDeadlineTick() <= 0L) {
                session.setLobbyDeadlineTick(level.getGameTime() + LOBBY_TIMEOUT_TICKS);
            }
            if (level.getGameTime() >= session.lobbyDeadlineTick()) {
                BoardLobbyService.finalizeTimedOutSelections(level, session);
                if (session.participantCount() > 0) startGame(level, session);
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
        BoardEntityService.ensureEntities(level, session);
        BoardWorldObjectService.tick(level, session);
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
        if (processTimeBombRoll(level, session)) return;
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
        if (!automated && PendingCardActionManager.hasBoardCardUi(controller)) return;
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

    static void startGame(ServerLevel level, BoardSession session) {
        if (session.phase() == BoardPhase.PLAYING || session.participantCount() == 0) return;
        if (!session.mechanics().hasCompleteCharacterStarts()) {
            for (ServerPlayer player : humanPlayers(level, session)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.incomplete",
                        session.mechanics().characterStartNodes().size(), REQUIRED_PLAYERS)
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        fillBots(level, session);
        List<BoardParticipant> participants = BoardLobbyService.orderedParticipants(session);
        if (participants.size() != REQUIRED_PLAYERS) {
            for (ServerPlayer player : humanPlayers(level, session)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_enough_characters",
                        participants.size(), REQUIRED_PLAYERS).withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        List<String> starts = session.mechanics().characterStartNodes();
        List<UUID> order = new ArrayList<>();
        for (int index = 0; index < participants.size(); index++) {
            BoardParticipant participant = participants.get(index);
            String startNode = starts.get(index % starts.size());
            CharacterDefinition definition = CharacterManager.INSTANCE.get(participant.characterId());
            AstralPlayerStats stats = AstralPlayerStats.initial(definition.baseStats());
            stats = stats.addCoins(PVP_INITIAL_STAR_COINS - stats.starCoins());
            List<Identifier> hand = randomInitialHand(level, 4 + level.getRandom().nextInt(2));
            BoardParticipant initialized = new BoardParticipant(participant.slotId(), participant.controllerId(),
                    participant.bot(), participant.characterId(), participant.skinId(),
                    BoardParticipant.nodeIdentifier(startNode), BoardParticipant.EMPTY_NODE_ID, Optional.empty(),
                    stats, hand, Map.of(), 0, 0, 0, 7, session.nextArrivalOrder());
            session.putParticipant(initialized);
            session.setHomeNode(initialized.slotUuid(), startNode);
            BoardEntityService.spawnCharacter(level, session, initialized);
            order.add(initialized.slotUuid());
        }

        session.setTurnOrder(order);
        BoardLobbyService.closeScreens(level, session.id());
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
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        prepareCurrentTurnAction(level, session, next, stillKnockedDown);
        if (wasKnockedDown && stillKnockedDown) finishTurn(level, session);
    }

    private static void prepareCurrentTurnAction(ServerLevel level, BoardSession session, BoardParticipant participant, boolean stillKnockedDown) {
        ServerPlayer controller = participant.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        boolean automated = participant.bot() || controller == null;
        int durationTicks = stillKnockedDown || automated ? 1 : participant.decisionDurationTicks(TURN_TIMEOUT_TICKS);
        session.setActionDurationTicks(durationTicks);
        session.setActionDeadlineTick(stillKnockedDown ? 0L
                : automated ? level.getGameTime() : level.getGameTime() + durationTicks);
        markChanged(level);
        if (controller != null) {
            controller.sendSystemMessage(Component.translatable(stillKnockedDown
                    ? "message.astral_craft.board.turn_skipped_knockdown"
                    : "message.astral_craft.board.your_turn", session.round() + 1), true);
            if (!stillKnockedDown) {
                AstralCharacterEntity character = BoardEntityService.entity(level, participant);
                if (character != null) sendTurnScreen(controller, session, participant, character);
            }
        }
    }

    private static void beginMoveRoll(ServerLevel level, BoardSession session, BoardParticipant participant, @Nullable ServerPlayer controller) {
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) {
            finishTurn(level, session);
            return;
        }

        if (session.mechanics().timeBombSlot().filter(participant.slotUuid()::equals).isPresent()) {
            beginTimeBombMovementRoll(level, session, participant, entity);
            return;
        }

        if (controller != null) {
            AstralDiceRollService.DiceRollResult result = AstralDiceRollService.rollNextMove(controller,
                    entity.position().add(0.0D, entity.getBbHeight() + 0.85D, 0.0D));
            int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                    + (result.values().size() > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                    + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
            beginMovement(level, session, participant, result.total(), revealTicks);
            return;
        }

        int fixed = participant.stats().nextMoveFixed();
        int diceCount = fixed > 0 ? 1 : Math.clamp(1 + participant.stats().nextMoveExtraDice(), 1, 8);
        int total = 0;
        for (int index = 0; index < diceCount; index++) {
            total += fixed > 0 ? fixed : level.getRandom().nextInt(10) + 1;
        }

        AstralDiceEntity dice = new AstralDiceEntity(level, entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.85D, entity.getZ());
        dice.startRoll(1, fixed > 0 ? fixed : 10, AstralDiceRollService.DEFAULT_ROLL_TICKS,
                AstralDiceRollService.DEFAULT_SPIN_SPEED, fixed > 0 ? fixed : Math.max(1, total / diceCount),
                total, diceCount > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0,
                true, 0.0F, 0.0F);
        level.addFreshEntity(dice);
        updateParticipant(level, session, participant.withStats(participant.stats().clearNextMoveDiceEffects()));
        int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                + (diceCount > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
        beginMovement(level, session, participant, total, revealTicks);
    }

    private static void beginTimeBombMovementRoll(ServerLevel level, BoardSession session, BoardParticipant participant, AstralCharacterEntity entity) {
        int result = Mth.nextInt(level.getRandom(), 1, 6);
        AstralDiceEntity dice = new AstralDiceEntity(level, entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.85D, entity.getZ());
        dice.startRoll(1, 6, AstralDiceRollService.DEFAULT_ROLL_TICKS,
                AstralDiceRollService.DEFAULT_SPIN_SPEED, result, result, 0,
                true, 0.0F, 0.0F);
        level.addFreshEntity(dice);
        updateParticipant(level, session, participant.withStats(participant.stats().clearNextMoveDiceEffects()));
        long executeTick = level.getGameTime() + AstralDiceRollService.DEFAULT_ROLL_TICKS + AstralDiceEntity.RESULT_HOLD_TICKS + 2L;
        PENDING_TIME_BOMB_ROLLS.put(session.id(), new PendingTimeBombRoll(participant.slotUuid(), result, executeTick));
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
    }

    private static boolean processTimeBombRoll(ServerLevel level, BoardSession session) {
        PendingTimeBombRoll pending = PENDING_TIME_BOMB_ROLLS.get(session.id());
        if (pending == null) return false;
        if (level.getGameTime() < pending.executeTick()) return true;
        PENDING_TIME_BOMB_ROLLS.remove(session.id());
        BoardParticipant participant = session.participant(pending.slotId()).orElse(null);
        if (participant == null) {
            session.mechanics().setTimeBombSlot(Optional.empty());
            markChanged(level);
            return false;
        }

        if (pending.result() == 1) {
            session.mechanics().setTimeBombSlot(Optional.empty());
            AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
            if (entity != null) {
                BoardWorldObjectService.playExplosion(level, entity.getX(), entity.getY() + 0.8D, entity.getZ());
            }

            damageFromEffect(level, session, participant.slotUuid(), 99);
            finishTurn(level, session);
            return true;
        }

        Optional<UUID> nextBombSlot = nextTimeBombSlot(session, participant.slotUuid());
        session.mechanics().setTimeBombSlot(nextBombSlot.isPresent() ? nextBombSlot : Optional.of(participant.slotUuid()));
        markChanged(level);
        BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
        if (refreshed.knockedDown()) {
            finishTurn(level, session);
        } else {
            beginMovement(level, session, refreshed, pending.result(), 0);
        }

        return true;
    }

    public static Optional<UUID> nextActionSlot(BoardSession session, UUID currentSlotId) {
        if (session == null || session.turnOrder().isEmpty() || currentSlotId == null) return Optional.empty();
        int index = session.turnOrder().indexOf(currentSlotId);
        if (index < 0) return Optional.of(session.turnOrder().getFirst());
        return Optional.of(session.turnOrder().get((index + 1) % session.turnOrder().size()));
    }

    private static Optional<UUID> nextTimeBombSlot(BoardSession session, UUID currentSlotId) {
        if (session == null || session.turnOrder().isEmpty() || currentSlotId == null) return Optional.empty();
        int currentIndex = session.turnOrder().indexOf(currentSlotId);
        if (currentIndex < 0) currentIndex = session.turnOrder().size() - 1;
        for (int offset = 1; offset < session.turnOrder().size(); offset++) {
            UUID slotId = session.turnOrder().get((currentIndex + offset) % session.turnOrder().size());
            BoardParticipant participant = session.participant(slotId).orElse(null);
            if (participant != null && participant.knockedDownTurns() <= 1) return Optional.of(slotId);
        }
        return Optional.empty();
    }

    public static boolean giveTimeBombToNext(ServerLevel level, BoardSession session, UUID sourceSlotId) {
        Optional<UUID> next = nextTimeBombSlot(session, sourceSlotId);
        if (next.isEmpty()) return false;
        session.mechanics().setTimeBombSlot(next);
        markChanged(level);
        return true;
    }

    private static void beginMovement(ServerLevel level, BoardSession session, BoardParticipant participant, int steps, int delayTicks) {
        session.setMovement(BoardSession.MovementState.begin(participant.slotUuid(), Math.max(0, steps), level.getGameTime() + Math.max(0, delayTicks)));
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        if (steps <= 0) finishMovement(level, session);
    }

    private static void beginBotTurn(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (BoardEntityService.entity(level, participant) == null) return;
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
                BoardBotEffectContext context = new BoardBotEffectContext(
                        level, session, current.slotUuid(), definition, pending.targetSlotIds());
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
            int sourceEntityId = BoardEntityService.entityId(level, used);
            List<Integer> targetEntityIds = targetSlotIds.stream()
                    .map(session::participant).flatMap(Optional::stream)
                    .mapToInt(target -> BoardEntityService.entityId(level, target)).filter(id -> id >= 0).boxed().toList();
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

    private static void beginBotMovement(ServerLevel level, BoardSession session, BoardParticipant participant) {
        beginMoveRoll(level, session, participant, null);
    }

    private static void tickMovement(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) return;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null) {
            session.setMovement(null);
            return;
        }

        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) return;
        if (movement.stepping()) {
            BlockPos from = session.positions().get(participant.currentNodeKey());
            BlockPos to = session.positions().get(movement.activeTargetNodeId());
            if (from == null || to == null) {
                session.setMovement(null);
                return;
            }

            Direction direction = BoardEntityService.directionBetween(from, to);
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
            BoardEntityService.arrangeNode(level, session, participant.currentNodeKey());
            BoardEntityService.arrangeNode(level, session, moved.currentNodeKey());
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
                HandcardRedirection.consumeFreeDirection(level, session, participant);
                session.setMovement(movement.beginStep(choice, level.getGameTime(), MOVEMENT_STEP_TICKS));
                BoardRouteService.preview(level, session);
                markChanged(level);
            }
            return;
        }

        if (movement.remainingSteps() <= 0) {
            finishMovement(level, session);
            return;
        }

        if (level.getGameTime() < movement.nextStepTick()) return;
        BoardRouteService.preview(level, session);
        List<String> choices = BoardRouteService.nextChoices(session, participant);
        if (choices.isEmpty()) {
            finishMovement(level, session);
        } else if (choices.size() == 1 || isAutomated(level, participant)) {
            String choice = choices.size() == 1 ? choices.getFirst()
                    : choices.get(level.getRandom().nextInt(choices.size()));
            HandcardRedirection.consumeFreeDirection(level, session, participant);
            session.setMovement(movement.beginStep(choice, level.getGameTime(), MOVEMENT_STEP_TICKS));
        } else {
            int durationTicks = participant.decisionDurationTicks(BRANCH_TIMEOUT_TICKS);
            session.setMovement(movement.waitingForBranch(choices, level.getGameTime() + durationTicks));
            BoardRouteService.preview(level, session);
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

            boolean naturalLanding = movement.remainingSteps() == 0;
            BoardWorldObjectService.ArrivalResult trapResult = BoardWorldObjectService.triggerArrival(
                    level, session, participant, naturalLanding);
            participant = trapResult.participant();
            boolean resolvesLanding = naturalLanding || trapResult.stopped();
            if (resolvesLanding && !participant.knockedDown()) {
                BoardWorldObjectService.pickupAtArrival(level, session, participant);
                participant = session.participant(movement.slotId()).orElse(participant);
            }
            movement = session.movement() == null ? movement : session.movement();
            if (movement == null) return;
            movement = movement.withPanelResolved();
            session.setMovement(movement);
            if (!participant.knockedDown()) {
                applyPanel(level, session, participant, resolvesLanding);
            }
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

    private static void continueMovement(ServerLevel level, BoardSession session, BoardSession.MovementState movement) {
        if (movement.remainingSteps() <= 0) {
            finishMovement(level, session);
        } else {
            BoardRouteService.preview(level, session);
            markChanged(level);
        }
    }

    private static void finishMovement(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) return;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        session.setMovement(null);
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        if (participant == null) {
            finishTurn(level, session);
            return;
        }

        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
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
                        PacketDistributor.sendToPlayer(player, new OpenBoardDiscardPayload(session.id(),
                                cardViews(participant.hand()), extra, durationTicks, durationTicks,
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
            BoardEntityService.syncState(level, ended);
        }

        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        int previousRound = session.round();
        session.advanceTurn();
        if (session.round() != previousRound) {
            HandcardSoulLink.tickBoardLinks(level, session);
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
                BoardWorldObjectService.awardCoins(level, session, updated.slotUuid(), coinsPerReward);
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
            AstralCharacterEntity targetEntity = BoardEntityService.entity(level, target);
            String name = displayName(level, target);
            PacketDistributor.sendToPlayer(player, new OpenBoardEncounterPayload(session.id(),
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

    private static void applyPanel(ServerLevel level, BoardSession session,
                                   BoardParticipant participant, boolean landing) {
        BoardNode node = session.nodes().get(participant.currentNodeKey());
        BasePlatform platform = platform(node);
        if (platform == null || !platform.triggers(landing)) return;
        platform.applyBoardEffect(new BoardPanelContext(level, session, participant, node, landing));
        BoardParticipant updated = session.participant(participant.slotUuid()).orElse(participant);
        AstralCharacterEntity entity = BoardEntityService.entity(level, updated);
        if (entity != null && updated.stats().health() <= 0) onParticipantDamaged(entity, updated.stats());
    }

    public static void relocateParticipant(ServerLevel level, BoardSession session,
                                           BoardParticipant participant, String destinationNodeId) {
        if (!session.nodes().containsKey(destinationNodeId)) return;
        String previousNodeId = participant.currentNodeKey();
        BoardParticipant relocated = participant.withNode(participant.currentNodeId(),
                BoardParticipant.nodeIdentifier(destinationNodeId), session.nextArrivalOrder());
        updateParticipant(level, session, relocated);
        BoardEntityService.arrangeNode(level, session, previousNodeId);
        BoardEntityService.arrangeNode(level, session, destinationNodeId);
    }

    public static void resumeMovementAfterPanel(ServerLevel level, BoardSession session) {
        continueArrival(level, session);
        markChanged(level);
    }

    private static @Nullable BasePlatform platform(@Nullable BoardNode node) {
        if (node == null) return null;
        return BuiltInRegistries.BLOCK.getValue(node.platformId()) instanceof BasePlatform platform
                ? platform : null;
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

    private static BoardParticipant firstEncounterTarget(BoardSession session, BoardParticipant mover,
                                                           Set<UUID> resolvedSlots) {
        return session.participants().stream()
                .filter(participant -> !participant.slotUuid().equals(mover.slotUuid()))
                .filter(participant -> !resolvedSlots.contains(participant.slotUuid()))
                .filter(participant -> participant.currentNodeKey().equals(mover.currentNodeKey()))
                .filter(participant -> !participant.knockedDown())
                .min(Comparator.comparingInt(BoardParticipant::arrivalOrder)).orElse(null);
    }

    public static void syncBoardSnapshot(ServerLevel level, BoardSession session) {
        BoardHudSnapshotPayload snapshot = BoardHudSyncManager.createSnapshot(level, session);
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D,
                    center.getZ() + 0.5D) <= 128.0D * 128.0D) {
                PacketDistributor.sendToPlayer(player, snapshot);
            }
        }
    }

    public static void resetForLobby(ServerLevel level, BoardSession session) {
        BoardLobbyService.clear(session.id());
        BoardBattleService.cancel(session.id());
        BoardPanelSelectionService.clear(session.id());
        PENDING_BOT_EFFECTS.remove(session.id());
        PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
        PENDING_TIME_BOMB_ROLLS.remove(session.id());
        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BasePlatform.clearActiveBoardEffect(session.id());
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        BoardWorldObjectService.clear(level, session);
        BoardEntityService.clearRuntimeEntities(level, session);
        session.clearParticipants();
        session.setPhase(BoardPhase.READY);
        session.setLobbyDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        session.setProtectionEnabled(true);
        markChanged(level);
        BoardProtectionService.refreshProtectedAreas(level, data(level));
    }

    public static boolean activateAllOrNothing(ServerLevel level, BoardSession session, UUID slotId) {
        BoardParticipant participant = session == null ? null : session.participant(slotId).orElse(null);
        if (participant == null || participant.knockedDown()) return false;
        BoardParticipant updated = participant.withStats(participant.stats().addTemporary("attack", 5, 1))
                .withRoundStatusEffect(ALL_OR_NOTHING_STATUS, 1);
        updateParticipant(level, session, updated);
        return true;
    }

    public static boolean hasAllOrNothing(BoardParticipant participant) {
        return participant != null && participant.hasRoundStatusEffect(ALL_OR_NOTHING_STATUS);
    }

    public static void consumeAllOrNothing(ServerLevel level, BoardSession session, UUID slotId) {
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant != null && participant.hasRoundStatusEffect(ALL_OR_NOTHING_STATUS)) {
            updateParticipant(level, session, participant.withoutRoundStatusEffect(ALL_OR_NOTHING_STATUS));
        }
    }

    public static void knockDownFromEffect(ServerLevel level, BoardSession session, UUID slotId) {
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null || participant.knockedDown()) return;
        int lostCoins = Math.max(0, (participant.stats().starCoins() + 1) / 2);
        BoardParticipant knocked = participant.knockDown();
        BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), lostCoins);
        updateParticipant(level, session, knocked);
    }

    public static void updateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant) {
        BoardParticipant previous = session.participant(participant.slotUuid()).orElse(null);
        boolean damaged = previous != null && participant.stats().health() < previous.stats().health();
        boolean newlyKnockedDown = participant.knockedDownTurns() > 0
                && (previous == null || previous.knockedDownTurns() <= 0);
        session.putParticipant(participant);
        BoardEntityService.syncState(level, participant);
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (damaged) {
            int logicalDamage = Math.max(0, previous.stats().health() - participant.stats().health());
            HandcardSoulLink.mirrorBoardDamage(level, session, participant, logicalDamage);
            if (newlyKnockedDown || participant.stats().health() <= 0) {
                HandcardSoulLink.removeBoardLink(level, session, participant.slotUuid());
                if (entity != null) {
                    entity.flashBoardDamage(10);
                    entity.setAnimationAction("knockdown");
                }
            } else if (entity != null) {
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
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Optional.empty();
        for (ServerLevel level : server.getAllLevels()) {
            BoardSession session = data(level).get(boardId);
            if (session != null) return Optional.of(session.dimension());
        }

        return Optional.empty();
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

    private static BoardParticipant currentControlledParticipant(BoardSession session, ServerPlayer player) {
        BoardParticipant current = session.currentParticipant().orElse(null);
        return current != null && current.controlledBy(player.getUUID()) ? current : null;
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
        if (!definition.restrictions().unrestricted()) return false;
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

    private static List<BoardCardView> turnCardViews(ServerPlayer player, BoardParticipant participant) {
        List<BoardCardView> cards = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
            if (!(item instanceof BaseHandCard)) continue;
            ItemStack stack = new ItemStack(item);
            cards.add(new BoardCardView(index, stack, CardUseService.canPreviewBoardCard(player, stack)));
        }
        return List.copyOf(cards);
    }

    private static List<BoardCardView> cardViews(List<Identifier> hand) {
        List<BoardCardView> cards = new ArrayList<>();
        for (int index = 0; index < hand.size(); index++) {
            Item item = BuiltInRegistries.ITEM.getValue(hand.get(index));
            if (item instanceof BaseHandCard) cards.add(new BoardCardView(index, new ItemStack(item)));
        }
        return List.copyOf(cards);
    }

    private static BoardSavedData data(ServerLevel level) {
        return BoardSavedData.get(level);
    }

    public static void markChanged(ServerLevel level) {
        data(level).markChanged();
    }

    private record PendingTimeBombRoll(UUID slotId, int result, long executeTick) {}

    private record PendingBotEffect(UUID userSlotId, Identifier cardId, List<UUID> targetSlotIds, long executeTick) {
        private PendingBotEffect {
            targetSlotIds = List.copyOf(targetSlotIds);
        }
    }

}