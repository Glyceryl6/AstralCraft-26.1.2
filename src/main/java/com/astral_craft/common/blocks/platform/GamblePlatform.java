package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardSpectatorService;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.board.BoardEntityService;
import com.astral_craft.common.gameplay.dice.AstralDiceRollService;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.network.s2c.CloseBoardGamblePayload;
import com.astral_craft.common.network.s2c.OpenBoardGamblePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class GamblePlatform extends BasePlatform {

    private static final int CHOICE_TICKS = 20 * 10;
    private static final int ROLL_TICKS = 20 * 3;
    private static final int RESULT_TICKS = 20 * 3;
    private final Map<UUID, GambleState> games = new HashMap<>();

    public GamblePlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        List<BoardParticipant> eligible = context.session().participants().stream()
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> participant.stats().starCoins() > 3).toList();
        if (eligible.size() <= 1) {
            context.participant().controllerUuid().map(context.level().getServer().getPlayerList()::getPlayer)
                    .ifPresent(player -> player.sendSystemMessage(Component.translatable(
                            "message.astral_craft.board.gamble.not_enough_players"), true));
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }

        Set<UUID> eligibleSlots = new LinkedHashSet<>();
        Map<UUID, Boolean> choices = new LinkedHashMap<>();
        for (BoardParticipant participant : eligible) {
            eligibleSlots.add(participant.slotUuid());
            BoardSessionManager.updateParticipant(context.level(), context.session(),
                    participant.withStats(participant.stats().spendCoins(3)));
            BoardParticipant updated = context.session().participant(participant.slotUuid()).orElse(participant);
            if (BoardSessionManager.isAutomated(context.level(), updated)) {
                choices.put(updated.slotUuid(), context.level().getRandom().nextBoolean());
            }
        }

        int roundNumber = context.session().round() + 1;
        int baseReward = 3 + Math.max(0, roundNumber / 10) * 3;
        int totalReward = baseReward + eligibleSlots.size() * 3;
        GambleState state = new GambleState(eligibleSlots, choices, OpenBoardGamblePayload.Phase.CHOOSING,
                0, totalReward, AstralServerTickClock.now(context.level()) + CHOICE_TICKS, CHOICE_TICKS);
        this.games.put(context.session().id(), state);
        this.activateBoardEffect(context.session());
        if (choices.size() >= eligibleSlots.size()) {
            this.startRoll(context.level(), context.session(), state);
        } else {
            this.broadcast(context.level(), context.session(), state);
        }
    }

    public static void choose(ServerPlayer player, UUID boardId, boolean odd) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> BasePlatform.activeBoardEffect(boardId)
                .filter(GamblePlatform.class::isInstance).map(GamblePlatform.class::cast)
                .ifPresent(platform -> platform.chooseInternal(player, session, odd)));
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        GambleState state = this.games.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }

        if (AstralServerTickClock.now(level) < state.deadlineTick()) return;
        switch (state.phase()) {
            case CHOOSING -> {
                Map<UUID, Boolean> choices = new LinkedHashMap<>(state.choices());
                for (UUID slotId : state.eligibleSlots()) choices.putIfAbsent(slotId, level.getRandom().nextBoolean());
                this.startRoll(level, session, state.withChoices(choices));
            }

            case ROLLING -> this.settle(level, session, state);
            case RESULT -> this.finish(level, session);
        }
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        GambleState state = this.games.get(session.id());
        if (state == null || state.phase() != OpenBoardGamblePayload.Phase.CHOOSING
                || !state.eligibleSlots().contains(slotId) || state.choices().containsKey(slotId)) return;
        Map<UUID, Boolean> choices = new LinkedHashMap<>(state.choices());
        choices.put(slotId, level.getRandom().nextBoolean());
        GambleState updated = state.withChoices(choices);
        this.games.put(session.id(), updated);
        if (choices.size() >= state.eligibleSlots().size()) this.startRoll(level, session, updated);
        else this.broadcast(level, session, updated);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.games.remove(boardId);
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, boolean odd) {
        GambleState state = this.games.get(session.id());
        if (state == null || state.phase() != OpenBoardGamblePayload.Phase.CHOOSING) return;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        if (participant == null || !state.eligibleSlots().contains(participant.slotUuid())
                || state.choices().containsKey(participant.slotUuid())) return;
        BoardSessionManager.updateParticipant(player.level(), session, participant.recordManualDecision());
        Map<UUID, Boolean> choices = new LinkedHashMap<>(state.choices());
        choices.put(participant.slotUuid(), odd);
        GambleState updated = state.withChoices(choices);
        this.games.put(session.id(), updated);
        if (choices.size() >= state.eligibleSlots().size()) this.startRoll(player.level(), session, updated);
        else this.broadcast(player.level(), session, updated);
    }

    private void startRoll(ServerLevel level, BoardSession session, GambleState state) {
        int dieResult = level.getRandom().nextInt(6) + 1;
        session.currentParticipant().map(participant -> BoardEntityService.entity(level, participant)).ifPresent(entity -> {
            AstralDiceEntity dice = new AstralDiceEntity(level, entity.getX(),
                    entity.getY() + entity.getBbHeight() + 0.85D, entity.getZ());
            dice.setBoardSessionId(session.id());
            dice.startRoll(1, 6, AstralDiceRollService.DEFAULT_ROLL_TICKS,
                    AstralDiceRollService.DEFAULT_SPIN_SPEED, dieResult, dieResult,
                    0, true, 0.0F, 0.0F);
            level.addFreshEntity(dice);
        });

        GambleState rolling = state.withPhase(OpenBoardGamblePayload.Phase.ROLLING,
                dieResult, AstralServerTickClock.now(level) + ROLL_TICKS, ROLL_TICKS);
        this.games.put(session.id(), rolling);
        this.broadcast(level, session, rolling);
    }

    private void settle(ServerLevel level, BoardSession session, GambleState state) {
        boolean odd = state.dieResult() % 2 != 0;
        List<UUID> winners = state.choices().entrySet().stream()
                .filter(entry -> entry.getValue() == odd).map(Map.Entry::getKey).toList();
        int share = winners.isEmpty() ? 0 : state.totalReward() / winners.size();
        for (UUID slotId : winners) BoardWorldObjectService.awardCoins(level, session, slotId, share);
        for (ServerPlayer viewer : BoardSessionManager.humanPlayers(level, session)) {
            viewer.sendSystemMessage(winners.isEmpty()
                    ? Component.translatable("message.astral_craft.board.gamble.no_winner", state.totalReward())
                    .withStyle(ChatFormatting.GRAY)
                    : Component.translatable("message.astral_craft.board.gamble.winners", winners.size(), share)
                    .withStyle(ChatFormatting.GOLD), true);
        }

        GambleState result = state.withPhase(OpenBoardGamblePayload.Phase.RESULT, state.dieResult(),
                AstralServerTickClock.now(level) + RESULT_TICKS, RESULT_TICKS);
        this.games.put(session.id(), result);
        this.broadcast(level, session, result);
        BoardSessionManager.markChanged(level);
    }

    private void finish(ServerLevel level, BoardSession session) {
        this.games.remove(session.id());
        this.deactivateBoardEffect(session.id());
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardGamblePayload(session.id()));
        }

        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private void broadcast(ServerLevel level, BoardSession session, GambleState state) {
        boolean resultOdd = state.dieResult() % 2 != 0;
        List<OpenBoardGamblePayload.Entry> entries = session.turnOrder().stream().map(session::participant)
                .flatMap(Optional::stream).map(participant -> new OpenBoardGamblePayload.Entry(
                        participant.slotUuid(), BoardSessionManager.displayName(level, participant),
                        participant.characterId(), participant.skinId(),
                        state.eligibleSlots().contains(participant.slotUuid()),
                        state.choices().containsKey(participant.slotUuid()),
                        state.phase() == OpenBoardGamblePayload.Phase.RESULT
                                && state.choices().getOrDefault(participant.slotUuid(), !resultOdd) == resultOdd
                                && state.eligibleSlots().contains(participant.slotUuid()))).toList();
        int remaining = (int) Math.max(0L, state.deadlineTick() - AstralServerTickClock.now(level));
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            BoardParticipant local = session.participantByController(viewer.getUUID()).orElse(null);
            boolean canChoose = state.phase() == OpenBoardGamblePayload.Phase.CHOOSING && local != null
                    && state.eligibleSlots().contains(local.slotUuid()) && !state.choices().containsKey(local.slotUuid());
            PacketDistributor.sendToPlayer(viewer, new OpenBoardGamblePayload(session.id(), state.phase(), entries,
                    canChoose, state.dieResult(), state.totalReward(), remaining, state.durationTicks()));
        }
    }

    private record GambleState(Set<UUID> eligibleSlots, Map<UUID, Boolean> choices,
                               OpenBoardGamblePayload.Phase phase, int dieResult, int totalReward,
                               long deadlineTick, int durationTicks) {
        private GambleState {
            eligibleSlots = Set.copyOf(eligibleSlots);
            choices = Map.copyOf(choices);
        }

        private GambleState withChoices(Map<UUID, Boolean> choices) {
            return new GambleState(this.eligibleSlots, choices, this.phase, this.dieResult, this.totalReward, this.deadlineTick, this.durationTicks);
        }

        private GambleState withPhase(OpenBoardGamblePayload.Phase phase, int dieResult, long deadlineTick, int durationTicks) {
            return new GambleState(this.eligibleSlots, this.choices, phase, dieResult, this.totalReward, deadlineTick, durationTicks);
        }

    }

}