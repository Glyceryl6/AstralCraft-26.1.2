package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.blocks.platform.HospitalPlatform;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CombatBonusDefinition;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.DamagePresentation;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import com.astral_craft.common.gameplay.chip.ChipSelectionService;
import com.astral_craft.common.gameplay.dice.AstralDiceRollService;
import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.gameplay.handcard.PendingCounterEffectManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.items.cards.HandcardRedirection;
import com.astral_craft.common.items.cards.pvp.HandcardSoulLink;
import com.astral_craft.common.network.BoardCardView;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.s2c.*;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.function.Function;
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
    public static final int TURN_START_PROMPT_TICKS = 40;
    public static final int DISCARD_TIMEOUT_TICKS = 20 * 20;
    public static final int ENCOUNTER_TIMEOUT_TICKS = 20 * 12;
    public static final int BRANCH_TIMEOUT_TICKS = 20 * 12;
    public static final int MOVEMENT_STEP_TICKS = 3;
    private static final int MOVEMENT_GAP_TICKS = 1;
    public static final int MAX_ROUTE_BRANCHES = 96;
    private static final Map<UUID, PendingBotEffect> PENDING_BOT_EFFECTS = new HashMap<>();
    private static final Map<UUID, PendingTimeBombRoll> PENDING_TIME_BOMB_ROLLS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_BOT_MOVEMENT_TICKS = new HashMap<>();
    private static final Set<UUID> PENDING_BOT_COUNTERS = new HashSet<>();
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
        AstralPlayerStats resolved = stats;
        if (resolved.health() < participant.stats().health()) {
            int requestedDamage = participant.stats().health() - resolved.health();
            int damage = resolveIncomingDamage(level, session, participant, requestedDamage);
            participant = session.participant(participant.slotUuid()).orElse(participant);
            resolved = resolved.withHealth(Math.max(0, participant.stats().health() - damage));
        }

        if (isHospitalProtected(session, participant) && resolved.health() < participant.stats().health()) {
            resolved = resolved.withHealth(participant.stats().health());
        }

        updateParticipant(level, session, participant.withStats(resolved));
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

    public static boolean addBoardBuff(AstralCharacterEntity entity, BoardBuff buff, int duration, int amplifier) {
        if (entity == null || buff == null || !(entity.level() instanceof ServerLevel level)) return false;
        BoardSession session = findByEntity(entity).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantFor(entity).orElse(null);
        if (participant == null) return false;
        updateParticipant(level, session, participant.withStats(participant.stats().addBuff(buff, duration, amplifier)));
        return true;
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
        return maybeSession.map(boardSession -> BoardSpectatorService.presentationViewers(controller.level(), boardSession)).orElseGet(() -> List.of(controller));
    }

    public static boolean openTurnScreen(ServerPlayer player, AstralCharacterEntity entity) {
        if (PendingCardActionManager.hasBoardCardUi(player)) return false;
        Optional<BoardSession> maybeSession = findByEntity(entity);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        BoardParticipant participant = session.participantFor(entity).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return false;
        boolean currentTurn = session.currentParticipant().map(value -> value.slotUuid().equals(participant.slotUuid())).orElse(false);
        boolean canAct = currentTurn && isTurnActionReady(session, player.level()) && !participant.knockedDown()
                && session.movement() == null && session.encounter() == null && session.discard() == null
                && !BoardBattleService.active(session.id()) && !BoardEventService.active(session.id());
        sendTurnScreen(player, session, participant, entity, canAct);
        return true;
    }

    private static void sendTurnScreen(ServerPlayer player, BoardSession session, BoardParticipant participant, AstralCharacterEntity entity) {
        sendTurnScreen(player, session, participant, entity, true);
    }

    private static void sendTurnScreen(ServerPlayer player, BoardSession session, BoardParticipant participant, AstralCharacterEntity entity, boolean currentTurn) {
        int durationTicks = currentTurn ? Math.max(1, session.actionDurationTicks()) : 1;
        int remainingTicks = currentTurn ? (int) Math.clamp(
                session.actionDeadlineTick() - AstralServerTickClock.now(player.level()), 0L, durationTicks) : 0;
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
            session.setActionPromptDeadlineTick(0L);
            session.setActionDeadlineTick(AstralServerTickClock.now(player.level()));
        }

        markChanged(player.level());
        syncBoardSnapshot(player.level(), session);
    }

    public static void requestMove(ServerPlayer player, UUID boardId) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.PLAYING || !isTurnActionReady(session, player.level())
                || session.movement() != null
                || session.encounter() != null || session.discard() != null
                || BoardBattleService.active(session.id()) || BoardEventService.active(session.id())
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
        if (session == null || session.phase() != BoardPhase.PLAYING || !isTurnActionReady(session, player.level())
                || BoardBattleService.active(session.id())
                || BoardEventService.active(session.id()) || PendingCardActionManager.isExclusiveBusy(player)
                || PendingCardActionManager.hasPendingSelection(player)
                || PendingCardActionManager.hasBoardCardUi(player)) return;
        BoardParticipant participant = currentControlledParticipant(session, player);
        if (participant == null || participant.skillCooldownTurns() > 0 || session.movement() != null
                || session.encounter() != null || session.discard() != null) return;
        AstralCharacterEntity entity = BoardEntityService.entity(player.level(), participant);
        if (entity == null) return;
        int cooldown = AstralCharacterSkillService.useActiveSkillForBoard(player, entity, session, participant,
                BoardSpectatorService.presentationViewers(player.level(), session));
        if (cooldown < 0) return;
        BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
        int adjustedCooldown = Math.max(0, cooldown - ChipSelectionService.skillCooldownReduction(refreshed));
        BoardParticipant updated = refreshed.recordManualDecision().withSkillCooldown(adjustedCooldown);
        updateParticipant(player.level(), session, updated);
        sendTurnScreen(player, session, updated, entity);
    }

    public static void chooseEncounter(ServerPlayer player, UUID boardId, boolean challenge) {
        BoardSession session = session(player.level(), boardId).orElse(null);
        if (session == null || session.encounter() == null) return;
        BoardSession.EncounterState encounter = session.encounter();
        if (encounter == null) return;
        BoardParticipant mover = session.participant(encounter.moverSlotId()).orElse(null);
        BoardParticipant target = session.participant(encounter.targetSlotId()).orElse(null);
        if (mover == null || target == null || !mover.controlledBy(player.getUUID())) return;
        mover = mover.recordManualDecision();
        updateParticipant(player.level(), session, mover);
        session.setEncounter(null);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(player.level(), session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardEncounterPayload(session.id()));
        }

        if (challenge && !isHospitalProtected(session, target)) {
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
        if (discard == null) return;
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
        return participant != null && isTurnActionReady(session, player.level())
                && !PendingCardActionManager.hasBoardCardUi(player)
                && !BoardEventService.active(session.id())
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
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
        ItemStack stack = new ItemStack(item);
        BoardParticipant updated = participant.recordManualDecision().removeCard(index).useCardPlay();
        updated = ChipSelectionService.afterEffectCardPlayed(player.level(), updated);
        updateParticipant(player.level(), session, updated);
        CharacterManager.INSTANCE.character(updated.characterId()).onBoardEffectCardUsed(player.level(), session, updated, stack);
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

    public static boolean consumeCounterCard(ServerLevel level, BoardSession session, BoardParticipant participant, int index) {
        if (level == null || session == null || participant == null || index < 0 || index >= participant.hand().size()) return false;
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
                        && card.definition(view.stack()).type() == CardType.COUNTER).toList();
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
        Function<BoardParticipant, Boolean> function = value -> value.slotUuid().equals(participant.slotUuid());
        if (pausedTicks > 0L && session.actionDeadlineTick() > 0L && session.currentParticipant().map(function).orElse(false)) {
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
                && isTurnActionReady(session, player.level()) && session.movement() == null
                && session.encounter() == null && session.discard() == null && !BoardBattleService.active(session.id());
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

    public static void damageFromEffect(ServerLevel level, BoardSession session, UUID slotId, int damage) {
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null || participant.knockedDown() || damage <= 0
                || isHospitalProtected(session, participant)) return;
        int resolvedDamage = resolveIncomingDamage(level, session, participant, damage);
        participant = session.participant(slotId).orElse(participant);
        if (resolvedDamage <= 0) return;
        AstralPlayerStats damagedStats = participant.stats().damage(resolvedDamage);
        BoardParticipant updated = participant.withStats(damagedStats);
        if (damagedStats.health() <= 0) {
            int lostCoins = Math.max(0, (participant.stats().starCoins() + 1) / 2);
            updated = updated.knockDown();
            BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), lostCoins);
        }

        updateParticipant(level, session, updated);
    }

    public static int resolveIncomingDamage(ServerLevel level, BoardSession session, BoardParticipant participant, int damage) {
        if (participant == null || damage <= 0 || isHospitalProtected(session, participant)) return 0;
        AstralPlayerStats stats = participant.stats();
        int resolved = stats.resolveIncomingDamage(Math.max(0, damage + stats.incomingDamageBonus()));
        AstralPlayerStats consumed = stats.consumeIncomingDamageBuffs();
        if (!consumed.equals(stats)) updateParticipant(level, session, participant.withStats(consumed));
        return resolved;
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
        PENDING_BOT_COUNTERS.remove(session.id());
        PENDING_TIME_BOMB_ROLLS.remove(session.id());
        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BoardFortuneService.closePresentation(level, session);
        BasePlatform.clearActiveBoardEffect(session.id());
        BoardEventService.clear(session.id());
        BoardDeveloperService.clear(session.id());
        BoardMonsterService.clear(session.id());
        BoardLotteryService.clear(level, session);
        BoardSpectatorService.clearBoard(level, session.id());
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        BoardWorldObjectService.clear(level, session);
        BoardEntityService.clearBoardDice(level, session);
        BoardEntityService.clearRuntimeEntities(level, session);
        session.clearParticipants();
        session.setActionPromptDeadlineTick(0L);
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
            if (BoardDeveloperService.active(session.id())) {
                if (!BoardDeveloperService.ownerOnline(level, session.id())) {
                    resetForLobby(level, session);
                    return;
                }
                session.setLobbyDeadlineTick(AstralServerTickClock.now(level) + LOBBY_TIMEOUT_TICKS);
                return;
            }
            if (session.lobbyDeadlineTick() <= 0L) {
                session.setLobbyDeadlineTick(AstralServerTickClock.now(level) + LOBBY_TIMEOUT_TICKS);
            }

            if (AstralServerTickClock.now(level) >= session.lobbyDeadlineTick()) {
                BoardLobbyService.finalizeTimedOutSelections(level, session);
                if (BoardMatchmakingService.handleSelectionTimeout(level, session)) return;
                if (session.participantCount() > 0) {
                    startGame(level, session);
                }
            }

            return;
        }

        if (session.phase() != BoardPhase.PLAYING) return;
        if (BoardDeveloperService.active(session.id())) {
            if (!BoardDeveloperService.ownerOnline(level, session.id())) BoardDeveloperService.resume(level, session);
            return;
        }
        if (!hasOnlineHumanParticipant(level, session)) {
            long since = NO_HUMAN_SINCE_TICKS.computeIfAbsent(session.id(), ignored -> AstralServerTickClock.now(level));
            if (AstralServerTickClock.now(level) - since >= 100L) {
                endGame(level, session, true);
            }
            return;
        }

        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BoardEntityService.ensureEntities(level, session);
        BoardWorldObjectService.tick(level, session);
        if (BoardMonsterService.tick(level, session)) return;
        if (BoardEventService.hasRoundExecution(session.id())) {
            if (BoardEventService.tickRoundEffects(level, session)) return;
            continueRoundStart(level, session, session.round() + 1);
            return;
        }

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

        if (BoardLotteryService.tick(level, session)) return;
        if (BoardEventService.active(session.id()) && !BasePlatform.hasActiveBoardEffect(session.id())
                && BoardEventService.tick(level, session)) return;
        if (!session.turnStarted()) beginCurrentTurn(level, session);
        if (!activateCurrentTurnAfterPrompt(level, session)) return;
        if (processTimeBombRoll(level, session)) return;
        BoardSession.DiscardState discard = session.discard();
        if (discard != null) {
            if (AstralServerTickClock.now(level) >= discard.deadlineTick()) {
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
            if (AstralServerTickClock.now(level) >= encounter.deadlineTick()) {
                BoardParticipant mover = session.participant(encounter.moverSlotId()).orElse(null);
                BoardParticipant target = session.participant(encounter.targetSlotId()).orElse(null);
                session.setEncounter(null);
                if (mover != null && !isAutomated(level, mover)) {
                    mover = mover.recordTimedOutDecision();
                    updateParticipant(level, session, mover);
                }

                if (target != null && isAutomated(level, mover) && !isHospitalProtected(session, target)) {
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
        ServerPlayer controller = current.controllerUuid().map(level.getServer()
                .getPlayerList()::getPlayer).orElse(null);
        boolean automated = current.bot();
        if (!automated && controller == null) return;
        if (!automated && PendingCardActionManager.hasBoardCardUi(controller)) return;
        if (session.actionDeadlineTick() <= 0L) {
            int durationTicks = automated ? 1 : current.decisionDurationTicks(TURN_TIMEOUT_TICKS);
            session.setActionDurationTicks(durationTicks);
            session.setActionDeadlineTick(automated ? AstralServerTickClock.now(level) : AstralServerTickClock.now(level) + durationTicks);
            markChanged(level);
        }

        if (automated) {
            if (AstralServerTickClock.now(level) >= session.actionDeadlineTick()) {
                beginBotTurn(level, session, current);
            }
            return;
        }

        if (AstralServerTickClock.now(level) >= session.actionDeadlineTick()) {
            beginTimedOutHumanMovement(level, session, current, controller);
        }
    }

    static void startGame(ServerLevel level, BoardSession session) {
        if (session.phase() == BoardPhase.PLAYING || session.participantCount() == 0) return;
        if (session.startNodes().size() != REQUIRED_PLAYERS) {
            MutableComponent component = Component.translatable("message.astral_craft.board.scan_error.need_4_start_panels");
            for (ServerPlayer player : humanPlayers(level, session)) {
                player.sendSystemMessage(component.withStyle(ChatFormatting.RED), true);
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

        session.setTravelDirection(BoardTravelDirection.random(level.getRandom()));
        List<String> starts = session.startNodes();
        List<UUID> order = new ArrayList<>();
        for (int index = 0; index < participants.size(); index++) {
            BoardParticipant participant = participants.get(index);
            String startNode = starts.get(index);
            boolean developerConfigured = BoardDeveloperService.configured(session.id(), participant.slotUuid());
            CharacterDefinition definition = CharacterManager.INSTANCE.get(participant.characterId());
            AstralPlayerStats stats = participant.stats();
            if (!developerConfigured) {
                stats = CharacterManager.INSTANCE.character(participant.characterId())
                        .initializeBoardStats(AstralPlayerStats.initial(definition.baseStats()));
                stats = stats.addCoins(PVP_INITIAL_STAR_COINS - stats.starCoins());
            }
            List<Identifier> hand = developerConfigured ? participant.hand()
                    : randomInitialHand(level, 4 + level.getRandom().nextInt(2));
            String previousNode = BoardRouteService.initialPreviousNode(session, startNode);
            Identifier previousNodeId = previousNode.isBlank() ? BoardParticipant.EMPTY_NODE_ID
                    : BoardParticipant.nodeIdentifier(previousNode);
            BoardParticipant initialized = new BoardParticipant(participant.slotId(), participant.controllerId(),
                    participant.bot(), participant.characterId(), participant.skinId(),
                    BoardParticipant.nodeIdentifier(startNode), previousNodeId, null,
                    stats, hand, Map.of(), developerConfigured ? participant.skillCooldownTurns() : 0,
                    developerConfigured ? participant.knockedDownTurns() : 0, developerConfigured ? participant.cardPlaysUsed() : 0,
                    developerConfigured ? participant.maxHandSize() : 7, session.nextArrivalOrder());
            session.putParticipant(initialized);
            session.setHomeNode(initialized.slotUuid(), startNode);
            BoardEntityService.spawnCharacter(level, session, initialized);
            order.add(initialized.slotUuid());
        }

        session.setTurnOrder(order);
        BoardDeveloperService.finishSetup(session.id());
        BoardLobbyService.closeScreens(level, session.id());
        BoardMatchmakingService.clear(session.id());
        session.setPhase(BoardPhase.PLAYING);
        session.setLobbyDeadlineTick(0L);
        session.setActionPromptDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        for (ServerPlayer viewer : humanPlayers(level, session)) {
            viewer.sendSystemMessage(Component.translatable("message.astral_craft.board.game_started")
                    .withStyle(ChatFormatting.AQUA), true);
        }

        BoardHudSyncManager.announce(level, session,
                Component.translatable("message.astral_craft.board.announcement.round_start", 1), Component.empty(),
                BoardHudSyncManager.ROUND_START_SOUND, 50);
        beginCurrentTurn(level, session);
    }

    private static void beginCurrentTurn(ServerLevel level, BoardSession session) {
        BoardParticipant current = session.currentParticipant().orElse(null);
        if (current == null) return;
        boolean wasKnockedDown = current.knockedDown();
        boolean hospitalized = current.hasRoundStatusEffect(HospitalPlatform.HOSPITALIZED_STATUS);
        BoardParticipant prepared = ChipSelectionService.beforeTurnStart(level, current);
        int turnStartHealing = prepared.stats().turnStartHealing();
        int turnStartDamage = prepared.stats().turnStartDamage();
        BoardParticipant next = prepared.beginTurn();
        updateParticipant(level, session, next);
        if (!wasKnockedDown && !next.knockedDown() && turnStartHealing > 0) {
            next = next.withStats(next.stats().heal(turnStartHealing));
            updateParticipant(level, session, next);
        }
        if (!wasKnockedDown && !next.knockedDown() && turnStartDamage > 0) {
            damageFromEffect(level, session, next.slotUuid(), turnStartDamage);
            next = session.participant(next.slotUuid()).orElse(next);
        }

        AstralCharacterEntity turnEntity = BoardEntityService.entity(level, next);
        if (turnEntity != null) CharacterManager.INSTANCE.character(next.characterId()).onBoardTurnStart(turnEntity);
        session.setTurnStarted(true);
        session.setActionPromptDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        if (hospitalized) {
            next.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                    player.sendSystemMessage(Component.translatable("message.astral_craft.board.hospital.turn_skipped"), true));
            finishTurn(level, session);
            return;
        }
        if (next.knockedDown()) {
            next.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                    player.sendSystemMessage(Component.translatable("message.astral_craft.board.turn_skipped_knockdown"), true));
            finishTurn(level, session);
            return;
        }

        prepareCurrentTurnAction(level, session, next);
    }

    private static void prepareCurrentTurnAction(ServerLevel level, BoardSession session, BoardParticipant participant) {
        int durationTicks = participant.bot() ? 1 : participant.decisionDurationTicks(TURN_TIMEOUT_TICKS);
        session.setActionDurationTicks(durationTicks);
        session.setActionDeadlineTick(0L);
        session.setActionPromptDeadlineTick(AstralServerTickClock.now(level) + TURN_START_PROMPT_TICKS);
        markChanged(level);
        BoardHudSyncManager.send(level, session);
    }

    private static boolean activateCurrentTurnAfterPrompt(ServerLevel level, BoardSession session) {
        long promptDeadlineTick = session.actionPromptDeadlineTick();
        if (promptDeadlineTick <= 0L) return true;
        long currentTick = AstralServerTickClock.now(level);
        if (currentTick < promptDeadlineTick) return false;
        BoardParticipant participant = session.currentParticipant().orElse(null);
        if (participant == null || participant.knockedDown()) {
            session.setActionPromptDeadlineTick(0L);
            return true;
        }
        ServerPlayer controller = participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (!participant.bot() && controller == null) {
            session.setActionPromptDeadlineTick(currentTick + 20L);
            markChanged(level);
            return false;
        }
        int durationTicks = Math.max(1, session.actionDurationTicks());
        session.setActionPromptDeadlineTick(0L);
        session.setActionDeadlineTick(participant.bot() ? currentTick : currentTick + durationTicks);
        markChanged(level);
        BoardHudSyncManager.send(level, session);
        AstralCharacterEntity character = BoardEntityService.entity(level, participant);
        if (controller != null && character != null) {
            controller.sendSystemMessage(Component.translatable("message.astral_craft.board.your_turn", session.round() + 1), true);
            sendTurnScreen(controller, session, participant, character);
        }
        return true;
    }

    public static boolean isTurnActionReady(BoardSession session, ServerLevel level) {
        return session.turnStarted() && session.actionPromptDeadlineTick() <= 0L
                && (session.actionDeadlineTick() <= 0L || AstralServerTickClock.now(level) <= session.actionDeadlineTick());
    }

    public static void beginMoveRoll(ServerLevel level, BoardSession session, BoardParticipant participant, @Nullable ServerPlayer controller) {
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) {
            finishTurn(level, session);
            return;
        }

        if (session.mechanics().timeBombSlot().filter(participant.slotUuid()::equals).isPresent()) {
            beginTimeBombCheckRoll(level, session, participant, entity);
            return;
        }

        beginStandardMoveRoll(level, session, participant, controller, entity);
    }

    private static void beginStandardMoveRoll(ServerLevel level, BoardSession session, BoardParticipant participant,
                                              @Nullable ServerPlayer controller) {
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) {
            finishTurn(level, session);
            return;
        }
        beginStandardMoveRoll(level, session, participant, controller, entity);
    }

    private static void beginStandardMoveRoll(ServerLevel level, BoardSession session, BoardParticipant participant,
                                              @Nullable ServerPlayer controller, AstralCharacterEntity entity) {
        if (controller != null) {
            AstralDiceRollService.DiceRollResult result = AstralDiceRollService.rollNextMove(controller,
                    entity.position().add(0.0D, entity.getBbHeight() + 0.85D, 0.0D), participant.stats(), session.id());
            int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                    + (result.values().size() > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                    + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
            BoardParticipant updated = participant.withStats(participant.stats().clearNextMoveDiceEffects().consumeMoveRollBuffs());
            updateParticipant(level, session, updated);
            beginMovement(level, session, updated, result.total(), revealTicks);
            return;
        }

        int fixed = participant.stats().nextMoveFixed();
        int diceCount = fixed > 0 ? 1 : Math.clamp(1 + participant.stats().moveDiceBonus(), 1, 8);
        List<Integer> values = new ArrayList<>(diceCount);
        for (int index = 0; index < diceCount; index++) {
            values.add(fixed > 0 ? fixed : level.getRandom().nextInt(10) + 1);
        }

        int total = Math.max(0, values.stream().mapToInt(Integer::intValue).sum() + participant.stats().speed());
        Identifier texture = participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                .map(DiceSkinPreferenceManager::selectedTexture).orElse(DiceSkinPreferenceManager.DEFAULT_TEXTURE);
        float spacing = diceCount > 1 ? 0.72F : 0.0F;
        double center = (diceCount - 1) * 0.5D;
        for (int index = 0; index < values.size(); index++) {
            double offset = (index - center) * spacing;
            AstralDiceEntity dice = new AstralDiceEntity(level, entity.getX() + offset,
                    entity.getY() + entity.getBbHeight() + 0.85D, entity.getZ());
            dice.setTexture(texture);
            dice.setBoardSessionId(session.id());
            dice.startRoll(1, fixed > 0 ? fixed : 10, AstralDiceRollService.DEFAULT_ROLL_TICKS,
                    AstralDiceRollService.DEFAULT_SPIN_SPEED, values.get(index), total,
                    diceCount > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0, index == 0,
                    (float) -offset, 0.0F);
            level.addFreshEntity(dice);
        }

        BoardParticipant updated = participant.withStats(participant.stats().clearNextMoveDiceEffects().consumeMoveRollBuffs());
        updateParticipant(level, session, updated);
        int revealTicks = AstralDiceRollService.DEFAULT_ROLL_TICKS
                + (diceCount > 1 ? AstralDiceRollService.DEFAULT_MERGE_TICKS : 0)
                + AstralDiceEntity.RESULT_HOLD_TICKS + 2;
        beginMovement(level, session, updated, total, revealTicks);
    }

    private static void beginTimeBombCheckRoll(ServerLevel level, BoardSession session, BoardParticipant participant,
                                               AstralCharacterEntity entity) {
        int result = Mth.nextInt(level.getRandom(), 1, 6);
        AstralDiceEntity dice = new AstralDiceEntity(level, entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.95D, entity.getZ());
        dice.setBoardSessionId(session.id());
        dice.startFlatRoll(1, 6, AstralDiceRollService.DEFAULT_ROLL_TICKS, result);
        level.addFreshEntity(dice);
        long executeTick = AstralServerTickClock.now(level) + AstralDiceRollService.DEFAULT_ROLL_TICKS
                + AstralDiceEntity.RESULT_HOLD_TICKS + 2L;
        PENDING_TIME_BOMB_ROLLS.put(session.id(), new PendingTimeBombRoll(participant.slotUuid(), result, executeTick));
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
    }

    private static boolean processTimeBombRoll(ServerLevel level, BoardSession session) {
        PendingTimeBombRoll pending = PENDING_TIME_BOMB_ROLLS.get(session.id());
        if (pending == null) return false;
        if (AstralServerTickClock.now(level) < pending.executeTick()) return true;
        PENDING_TIME_BOMB_ROLLS.remove(session.id());
        BoardParticipant participant = session.participant(pending.slotId()).orElse(null);
        AstralCharacterEntity rollingEntity = participant == null ? null : BoardEntityService.entity(level, participant);
        if (rollingEntity != null) {
            level.playSound(null, rollingEntity.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 0.9F, 1.35F);
        }

        if (participant == null) {
            session.mechanics().setTimeBombSlot(null);
            markChanged(level);
            return false;
        }

        if (pending.result() == 1) {
            session.mechanics().setTimeBombSlot(null);
            AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
            if (entity != null) BoardWorldObjectService.playExplosion(level, entity.getX(), entity.getY() + 0.8D, entity.getZ());
            int lostCoins = Math.max(0, (participant.stats().starCoins() + 1) / 2);
            BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), lostCoins);
            updateParticipant(level, session, participant.knockDown());
            finishTurn(level, session);
            return true;
        }

        Optional<UUID> nextBombSlot = nextTimeBombSlot(session, participant.slotUuid());
        session.mechanics().setTimeBombSlot(nextBombSlot.orElse(participant.slotUuid()));
        markChanged(level);
        BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
        if (refreshed.knockedDown()) {
            finishTurn(level, session);
            return true;
        }

        ServerPlayer controller = refreshed.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        beginStandardMoveRoll(level, session, refreshed, controller);
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
        session.mechanics().setTimeBombSlot(next.orElse(null));
        markChanged(level);
        return true;
    }

    private static void beginMovement(ServerLevel level, BoardSession session, BoardParticipant participant, int steps, int delayTicks) {
        CharacterManager.INSTANCE.character(participant.characterId()).onBoardMoveStarted(level, session, participant);
        session.setMovement(BoardSession.MovementState.begin(participant.slotUuid(), Math.max(0, steps), AstralServerTickClock.now(level) + Math.max(0, delayTicks)));
        session.setActionPromptDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        markChanged(level);
        if (steps <= 0) finishMovement(level, session);
    }

    private static void beginBotTurn(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (BoardEntityService.entity(level, participant) == null || PENDING_BOT_COUNTERS.contains(session.id())
                || isBoardActionBlocked(session)) return;
        Long movementTick = PENDING_BOT_MOVEMENT_TICKS.get(session.id());
        if (movementTick != null) {
            if (AstralServerTickClock.now(level) < movementTick) return;
            PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
            BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
            if (refreshed.knockedDown()) finishTurn(level, session);
            else beginBotMovement(level, session, refreshed);
            return;
        }

        PendingBotEffect pending = PENDING_BOT_EFFECTS.get(session.id());
        if (pending != null) {
            if (AstralServerTickClock.now(level) < pending.executeTick()) return;
            PENDING_BOT_EFFECTS.remove(session.id());
            BoardParticipant current = session.participant(pending.userSlotId()).orElse(null);
            Item item = BuiltInRegistries.ITEM.getValue(pending.cardId());
            if (current != null && item instanceof BaseHandCard card && item instanceof BoardBotEffect botEffect) {
                ItemStack stack = new ItemStack(item);
                CardDefinition definition = card.definition(stack);
                BoardBotEffectContext context = new BoardBotEffectContext(
                        level, session, current.slotUuid(), definition, pending.targetSlotIds());
                boolean counterPending = PendingCounterEffectManager.offerBoardBotCard(level, session, current,
                        stack, definition, pending.targetSlotIds(),
                        targetSlotIds -> botEffect.applyByBoardBot(context.withTargets(targetSlotIds)),
                        followUpDelay -> completeBotEffect(level, session, current.slotUuid(), followUpDelay));
                if (counterPending) {
                    PENDING_BOT_COUNTERS.add(session.id());
                    return;
                }

                int followUpDelay = botEffect.applyByBoardBot(context);
                if (followUpDelay > 0) {
                    scheduleBotMovement(level, session, followUpDelay);
                    return;
                }
            }

            BoardParticipant refreshed = session.participant(participant.slotUuid()).orElse(participant);
            if (refreshed.knockedDown()) {
                if (refreshed.knockedDownTurns() <= 0) {
                    updateParticipant(level, session, refreshed.knockDown());
                }

                finishTurn(level, session);
            } else {
                beginBotMovement(level, session, refreshed);
            }

            return;
        }

        BoardParticipant refreshed = tryUseBotEffectCard(level, session, participant);
        if (PENDING_BOT_EFFECTS.containsKey(session.id())) return;
        if (refreshed.knockedDown()) {
            if (refreshed.knockedDownTurns() <= 0) {
                updateParticipant(level, session, refreshed.knockDown());
            }

            finishTurn(level, session);
            return;
        }

        beginBotMovement(level, session, refreshed);
    }

    private static void completeBotEffect(ServerLevel level, BoardSession session, UUID sourceSlotId, int followUpDelay) {
        PENDING_BOT_COUNTERS.remove(session.id());
        BoardSession activeSession = data(level).get(session.id());
        if (activeSession == null || activeSession.phase() != BoardPhase.PLAYING) return;
        if (followUpDelay > 0) {
            scheduleBotMovement(level, activeSession, followUpDelay);
            return;
        }

        BoardParticipant refreshed = activeSession.participant(sourceSlotId).orElse(null);
        if (refreshed == null) return;
        if (refreshed.knockedDown()) {
            if (refreshed.knockedDownTurns() <= 0) updateParticipant(level, activeSession, refreshed.knockDown());
            finishTurn(level, activeSession);
        } else {
            beginBotMovement(level, activeSession, refreshed);
        }
    }

    private static void scheduleBotMovement(ServerLevel level, BoardSession session, int delayTicks) {
        long followUpTick = AstralServerTickClock.now(level) + Math.max(0, delayTicks);
        PENDING_BOT_MOVEMENT_TICKS.put(session.id(), followUpTick);
        session.setActionDeadlineTick(followUpTick);
        markChanged(level);
    }

    private static BoardParticipant tryUseBotEffectCard(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (isBoardActionBlocked(session) || participant.cardPlaysUsed() > 0
                || participant.cardPlaysUsed() >= participant.stats().cardPlaysPerTurn()) return participant;
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
            if (!(item instanceof BaseHandCard card) || !(item instanceof BoardBotEffect botEffect)) continue;
            ItemStack stack = new ItemStack(item);
            CardDefinition definition = card.definition(stack);
            BoardBotEffectContext context = new BoardBotEffectContext(level, session, participant.slotUuid(), definition, List.of());
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
            used = ChipSelectionService.afterEffectCardPlayed(level, used);
            updateParticipant(level, session, used);
            CharacterManager.INSTANCE.character(used.characterId()).onBoardEffectCardUsed(level, session, used, stack);
            long executeTick = AstralServerTickClock.now(level) + CardUseService.CARD_REVEAL_DURATION_TICKS + 2L;
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
            for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) PacketDistributor.sendToPlayer(viewer, reveal);
            markChanged(level);
            return used;
        }

        return participant;
    }

    private static void beginBotMovement(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (isBoardActionBlocked(session)) {
            scheduleBotMovement(level, session, 1);
            return;
        }
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
            float progress = Mth.clamp((AstralServerTickClock.now(level) - movement.stepStartedTick()) / (float) movement.stepDurationTicks(), 0.0F, 1.0F);
            double x = Mth.lerp(progress, from.getX() + 0.5D, to.getX() + 0.5D);
            double y = Mth.lerp(progress, from.getY() + 0.12D, to.getY() + 0.12D);
            double z = Mth.lerp(progress, from.getZ() + 0.5D, to.getZ() + 0.5D);
            entity.setPos(x, y, z);
            entity.setAnimationAction("walk");
            if (AstralServerTickClock.now(level) == movement.stepStartedTick() + 1L) {
                level.playSound(null, entity.blockPosition(), SoundEvents.STONE_STEP, SoundSource.PLAYERS, 0.55F, 1.2F);
            }
            if (progress < 1.0F) return;
            BoardParticipant moved = participant.withNode(participant.currentNodeId(),
                    BoardParticipant.nodeIdentifier(movement.activeTargetNodeId()), session.nextArrivalOrder());
            session.putParticipant(moved);
            movement = movement.completeStep(movement.activeTargetNodeId(), AstralServerTickClock.now(level) + MOVEMENT_GAP_TICKS);
            session.setMovement(movement);
            BoardEntityService.arrangeNode(level, session, participant.currentNodeKey());
            BoardEntityService.arrangeNode(level, session, moved.currentNodeKey());
            continueArrival(level, session);
            return;
        }

        if (!movement.branchChoices().isEmpty()) {
            if (isAutomated(level, participant) || AstralServerTickClock.now(level) >= movement.nextStepTick()) {
                if (!isAutomated(level, participant)) {
                    participant = participant.recordTimedOutDecision();
                    updateParticipant(level, session, participant);
                }

                String choice = movement.branchChoices().get(level.getRandom().nextInt(movement.branchChoices().size()));
                HandcardRedirection.consumeFreeDirection(level, session, participant);
                session.setMovement(movement.beginStep(choice, AstralServerTickClock.now(level), MOVEMENT_STEP_TICKS));
                BoardRouteService.preview(level, session);
                markChanged(level);
            }

            return;
        }

        if (AstralServerTickClock.now(level) < movement.nextStepTick()) return;
        if (movement.route().isEmpty()) {
            level.playSound(null, entity.blockPosition(),
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 0.9F, 1.35F);
        }

        if (movement.remainingSteps() <= 0) {
            finishMovement(level, session);
            return;
        }

        BoardRouteService.preview(level, session);
        List<String> choices = BoardRouteService.nextChoices(session, participant);
        if (choices.isEmpty()) {
            finishMovement(level, session);
        } else if (choices.size() == 1 || isAutomated(level, participant)) {
            String choice = choices.size() == 1 ? choices.getFirst()
                    : choices.get(level.getRandom().nextInt(choices.size()));
            HandcardRedirection.consumeFreeDirection(level, session, participant);
            session.setMovement(movement.beginStep(choice, AstralServerTickClock.now(level), MOVEMENT_STEP_TICKS));
        } else {
            int durationTicks = participant.decisionDurationTicks(BRANCH_TIMEOUT_TICKS);
            session.setMovement(movement.waitingForBranch(choices, AstralServerTickClock.now(level) + durationTicks));
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
            BoardParticipant encounterTarget = firstEncounterTarget(session, participant, movement.resolvedEncounterSlots());
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

    private static BoardParticipant applyMovementBuffs(ServerLevel level, BoardSession session,
                                                       BoardSession.MovementState movement, BoardParticipant participant) {
        BoardParticipant updated = participant;
        for (BoardBuffInstance instance : participant.stats().buffs().values()) {
            updated = instance.buff().onMovementFinished(level, session, movement, updated, instance);
        }
        if (!updated.equals(participant)) updateParticipant(level, session, updated);
        return updated;
    }

    private static void finishMovement(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) return;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant != null) participant = applyMovementBuffs(level, session, movement, participant);

        session.setMovement(null);
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        if (participant == null) {
            finishTurn(level, session);
            return;
        }

        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity != null) entity.setAnimationAction("idle");
        CharacterManager.INSTANCE.character(participant.characterId()).onBoardMoveFinished(level, session, participant);
        int extra = Math.max(0, participant.hand().size() - participant.maxHandSize());
        if (extra > 0) {
            int durationTicks = participant.decisionDurationTicks(DISCARD_TIMEOUT_TICKS);
            session.setDiscard(new BoardSession.DiscardState(participant.slotUuid(), extra,
                    AstralServerTickClock.now(level) + durationTicks, durationTicks));
            if (participant.bot()) {
                randomDiscard(level, session);
            } else {
                BoardParticipant finalParticipant = participant;
                participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                        PacketDistributor.sendToPlayer(player, new OpenBoardDiscardPayload(session.id(),
                                cardViews(finalParticipant.hand()), extra, durationTicks, durationTicks,
                                finalParticipant.characterId(), finalParticipant.skinId())));
            }
        } else {
            finishTurn(level, session);
        }

        markChanged(level);
    }

    private static void finishTurn(ServerLevel level, BoardSession session) {
        BoardParticipant participant = session.currentParticipant().orElse(null);
        if (participant != null) {
            AstralCharacterEntity turnEntity = BoardEntityService.entity(level, participant);
            if (turnEntity != null) CharacterManager.INSTANCE.character(participant.characterId()).onBoardTurnEnd(turnEntity);
            BoardParticipant ended = ChipSelectionService.afterTurnEnd(level, participant.endTurn());
            session.putParticipant(ended);
            BoardEntityService.syncState(level, ended);
            HandcardSoulLink.tickBoardLinks(level, session);
        }

        session.setActionPromptDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        int previousRound = session.round();
        session.advanceTurn();
        if (session.round() != previousRound) {
            int roundNumber = session.round() + 1;
            BoardHudSyncManager.announce(level, session,
                    Component.translatable("message.astral_craft.board.announcement.round_start", roundNumber), Component.empty(),
                    BoardHudSyncManager.ROUND_START_SOUND, 50);
            if (BoardEventService.beginRoundEffects(level, session)) {
                markChanged(level);
                return;
            }

            continueRoundStart(level, session, roundNumber);
            return;
        }

        markChanged(level);
        beginCurrentTurn(level, session);
    }

    private static void continueRoundStart(ServerLevel level, BoardSession session, int roundNumber) {
        applyRoundRewards(level, session, roundNumber);
        boolean lotteryStarted = BoardLotteryService.begin(level, session, roundNumber);
        markChanged(level);
        if (!lotteryStarted && !BoardMonsterService.beginPhase(level, session)) beginCurrentTurn(level, session);
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
            int coinsPerEvent = coinsPerReward + participant.stats().roundRewardBonus();
            for (int eventIndex = 0; eventIndex < rewardEvents; eventIndex++) {
                updated = updated.withStats(updated.stats().addCoins(coinsPerEvent));
                Optional<Identifier> card = randomCardId(level, BoardSessionManager::validPvpCard);
                if (card.isPresent()) {
                    updated = updated.addCard(card.get());
                    totalCards++;
                }
            }

            updateParticipant(level, session, updated);
            int totalCoins = coinsPerEvent * rewardEvents;
            int finalTotalCards = totalCards;
            updated.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                    player.sendSystemMessage(Component.translatable(
                            "message.astral_craft.board.round_reward", roundNumber, totalCoins, finalTotalCards)
                            .withStyle(ChatFormatting.GOLD), true));
        }
    }

    public static void beginPanelEncounter(ServerLevel level, BoardSession session, BoardParticipant mover, BoardParticipant target) {
        if (level == null || session == null || mover == null || target == null || target.knockedDown() || isHospitalProtected(session, target)) {
            if (level != null && session != null) resumeMovementAfterPanel(level, session);
            return;
        }

        beginEncounter(level, session, mover, target);
        markChanged(level);
    }

    private static void beginEncounter(ServerLevel level, BoardSession session, BoardParticipant mover, BoardParticipant target) {
        boolean automated = isAutomated(level, mover);
        int durationTicks = automated ? 20 : mover.decisionDurationTicks(ENCOUNTER_TIMEOUT_TICKS);
        session.setEncounter(new BoardSession.EncounterState(mover.slotUuid(), target.slotUuid(),
                AstralServerTickClock.now(level) + durationTicks, durationTicks));
        int targetEntityId = BoardEntityService.entityId(level, target);
        String name = displayName(level, target);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            boolean interactive = !automated && mover.controlledBy(viewer.getUUID());
            PacketDistributor.sendToPlayer(viewer, new OpenBoardEncounterPayload(session.id(),
                    targetEntityId, name, durationTicks,
                    durationTicks, interactive, mover.characterId(), mover.skinId()));
        }
    }

    public static void resumeAfterBattle(ServerLevel level, BoardSession session) {
        session.setEncounter(null);
        if (BoardMonsterService.resumeAfterBattle(level, session)) {
            markChanged(level);
            return;
        }
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
        AstralCharacterEntity entity = BoardEntityService.entity(level, updated);
        if (entity != null && updated.stats().health() <= 0) onParticipantDamaged(entity, updated.stats());
    }

    public static void relocateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant, String destinationNodeId) {
        relocateParticipant(level, session, participant, destinationNodeId, BoardRouteService.travelDirection(session, participant));
    }

    public static void relocateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant,
                                           String destinationNodeId, Direction travelDirection) {
        if (!session.nodes().containsKey(destinationNodeId)) return;
        String oldNodeId = participant.currentNodeKey();
        String previousNodeId = BoardRouteService.previousNodeForTravelDirection(session, destinationNodeId, travelDirection);
        Identifier previous = previousNodeId.isBlank() ? BoardParticipant.EMPTY_NODE_ID
                : BoardParticipant.nodeIdentifier(previousNodeId);
        BoardParticipant relocated = participant.withNode(previous, BoardParticipant.nodeIdentifier(destinationNodeId),
                session.nextArrivalOrder());
        updateParticipant(level, session, relocated);
        AstralCharacterEntity entity = BoardEntityService.entity(level, relocated);
        if (entity != null) entity.setBoardDirection(BoardRouteService.facingDirection(session, relocated));
        BoardEntityService.arrangeNode(level, session, oldNodeId);
        BoardEntityService.arrangeNode(level, session, destinationNodeId);
    }

    public static void resumeMovementAfterPanel(ServerLevel level, BoardSession session) {
        continueArrival(level, session);
        markChanged(level);
    }

    public static void resumeAfterLotteryDraw(ServerLevel level, BoardSession session) {
        if (session.phase() != BoardPhase.PLAYING || session.turnStarted()) return;
        if (!BoardMonsterService.beginPhase(level, session)) beginCurrentTurn(level, session);
    }

    static void resumeAfterMonsterPhase(ServerLevel level, BoardSession session) {
        if (session.phase() == BoardPhase.PLAYING && !session.turnStarted()) beginCurrentTurn(level, session);
    }

    private static @Nullable BasePlatform platform(@Nullable BoardNode node) {
        if (node == null) return null;
        return BuiltInRegistries.BLOCK.getValue(node.platformId()) instanceof BasePlatform platform ? platform : null;
    }

    static boolean hasPendingDeveloperUnsafeAction(UUID boardId) {
        return boardId != null && (PENDING_BOT_EFFECTS.containsKey(boardId) || PENDING_TIME_BOMB_ROLLS.containsKey(boardId)
                || PENDING_BOT_MOVEMENT_TICKS.containsKey(boardId) || PENDING_BOT_COUNTERS.contains(boardId));
    }

    private static boolean isBoardActionBlocked(BoardSession session) {
        return session == null || BasePlatform.hasActiveBoardEffect(session.id())
                || BoardEventService.active(session.id()) || BoardLotteryService.active(session.id())
                || session.encounter() != null || session.discard() != null
                || BoardBattleService.active(session.id()) || BoardMonsterService.active(session.id())
                || session.movement() != null;
    }

    public static boolean isAutomated(ServerLevel level, BoardParticipant participant) {
        return participant != null && participant.bot();
    }

    private static void fillBots(ServerLevel level, BoardSession session) {
        List<CharacterDefinition> available = CharacterManager.INSTANCE.values().stream()
                .filter(definition -> !session.hasCharacter(definition.id()))
                .filter(definition -> CharacterManager.INSTANCE.character(definition.id()).botSelectable())
                .toList();
        List<CharacterDefinition> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, new Random(level.getRandom().nextLong()));
        int index = 0;
        while (session.participantCount() < REQUIRED_PLAYERS && index < shuffled.size()) {
            CharacterDefinition definition = shuffled.get(index++);
            String skinId = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
            BoardParticipant bot = new BoardParticipant(UUID.randomUUID(), null, true,
                    definition.id(), BoardParticipant.skinIdentifier(definition.id(), skinId),
                    BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID, null,
                    AstralPlayerStats.DEFAULT, List.of(), Map.of(), 0, 0, 0, 7, session.nextArrivalOrder());
            session.putParticipant(bot);
        }
    }

    private static BoardParticipant firstEncounterTarget(BoardSession session, BoardParticipant mover, Set<UUID> resolvedSlots) {
        return session.participants().stream()
                .filter(participant -> !participant.slotUuid().equals(mover.slotUuid()))
                .filter(participant -> !resolvedSlots.contains(participant.slotUuid()))
                .filter(participant -> participant.currentNodeKey().equals(mover.currentNodeKey()))
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> !isHospitalProtected(session, participant))
                .min(Comparator.comparingInt(BoardParticipant::arrivalOrder)).orElse(null);
    }

    public static boolean isHospitalProtected(BoardSession session, BoardParticipant participant) {
        if (session == null || participant == null) return false;
        BasePlatform platform = platform(session.nodes().get(participant.currentNodeKey()));
        return platform != null && platform.protectsBoardParticipant();
    }

    public static boolean isHospitalProtected(AstralCharacterEntity entity) {
        if (entity == null) return false;
        BoardSession session = findByEntity(entity).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantFor(entity).orElse(null);
        return participant != null && isHospitalProtected(session, participant);
    }

    public static void syncBoardSnapshot(ServerLevel level, BoardSession session) {
        BoardHudSnapshotPayload snapshot = BoardHudSyncManager.createSnapshot(level, session);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, snapshot);
        }
    }

    public static void resetForLobby(ServerLevel level, BoardSession session) {
        BoardLobbyService.clear(session.id());
        BoardMatchmakingService.clear(session.id());
        BoardDeveloperService.clear(session.id());
        BoardMonsterService.clear(session.id());
        BoardBattleService.cancel(session.id());
        BoardPanelSelectionService.clear(session.id());
        PENDING_BOT_EFFECTS.remove(session.id());
        PENDING_BOT_MOVEMENT_TICKS.remove(session.id());
        PENDING_BOT_COUNTERS.remove(session.id());
        PENDING_TIME_BOMB_ROLLS.remove(session.id());
        NO_HUMAN_SINCE_TICKS.remove(session.id());
        BoardFortuneService.closePresentation(level, session);
        BasePlatform.clearActiveBoardEffect(session.id());
        BoardEventService.clear(session.id());
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        BoardWorldObjectService.clear(level, session);
        BoardEntityService.clearBoardDice(level, session);
        BoardEntityService.clearRuntimeEntities(level, session);
        session.clearParticipants();
        session.setPhase(BoardPhase.READY);
        session.setLobbyDeadlineTick(0L);
        session.setActionPromptDeadlineTick(0L);
        session.setActionDeadlineTick(0L);
        session.setActionDurationTicks(0);
        session.setProtectionEnabled(true);
        markChanged(level);
        BoardProtectionService.refreshProtectedAreas(level, data(level));
    }

    public static void knockDownFromEffect(ServerLevel level, BoardSession session, UUID slotId) {
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null || participant.knockedDown() || isHospitalProtected(session, participant)) return;
        int lostCoins = Math.max(0, (participant.stats().starCoins() + 1) / 2);
        BoardParticipant knocked = participant.knockDown();
        BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), lostCoins);
        updateParticipant(level, session, knocked);
    }

    public static void updateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant) {
        updateParticipant(level, session, participant, true);
    }

    static void updateParticipantWithoutCoinAnimation(ServerLevel level, BoardSession session, BoardParticipant participant) {
        updateParticipant(level, session, participant, false);
    }

    private static void updateParticipant(ServerLevel level, BoardSession session, BoardParticipant participant, boolean animateCoinChange) {
        BoardParticipant previous = session.participant(participant.slotUuid()).orElse(null);
        boolean damaged = previous != null && participant.stats().health() < previous.stats().health();
        boolean healed = previous != null && participant.stats().health() > previous.stats().health();
        boolean gainedStatus = previous != null && AstralCardEffects.gainedStatus(previous.stats(), participant.stats());
        boolean newlyKnockedDown = participant.knockedDownTurns() > 0
                && (previous == null || previous.knockedDownTurns() <= 0);
        int coinDelta = previous == null ? 0 : participant.stats().starCoins() - previous.stats().starCoins();
        session.putParticipant(participant);
        if (newlyKnockedDown && session.mechanics().timeBombSlot().filter(participant.slotUuid()::equals).isPresent()) {
            session.mechanics().setTimeBombSlot(null);
            PENDING_TIME_BOMB_ROLLS.remove(session.id());
        }
        BoardEntityService.syncState(level, participant);
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity != null) {
            if (damaged) DamagePresentation.playDamageImpact(level, entity);
            else if (healed) AstralCardEffects.playHealingEffect(entity);
            else if (gainedStatus) AstralCardEffects.playStatusEffect(entity);
        }
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
        if (animateCoinChange && coinDelta < 0) {
            BoardWorldObjectService.animateCoinLoss(level, session, participant.slotUuid(), -coinDelta);
        } else if (animateCoinChange && coinDelta > 0) {
            BoardWorldObjectService.animateCoinAward(level, session, participant.slotUuid(), coinDelta);
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
        if (participant.monster()) return Component.translatable("gui.astral_craft.board.monster").getString();
        if (!participant.bot()) return Component.translatable("gui.astral_craft.board.player").getString();
        BoardSession session = sessions(level).stream()
                .filter(value -> value.participant(participant.slotUuid()).isPresent())
                .findFirst().orElse(null);
        if (session == null) return Component.translatable("gui.astral_craft.board.bot").getString();
        List<BoardParticipant> bots = session.participants().stream()
                .filter(BoardParticipant::bot).filter(p -> !p.monster()).toList();
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

    public static Optional<Identifier> randomPvpCardId(ServerLevel level) {
        return randomCardId(level, BoardSessionManager::validPvpCard);
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
