package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.network.s2c.OpenBoardStartChoicePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StartPlatform extends BasePlatform {

    public static final int TIMEOUT_TICKS = 20 * 10;
    private static final int[] STAR_COSTS = {0, 15, 30, 50};
    private final Map<UUID, StartChoiceState> choices = new HashMap<>();

    public StartPlatform(Block.Properties properties) {
        super(properties, Trigger.BOTH);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        this.arrive(context);
    }

    public static void choose(ServerPlayer player, String rawBoardId, boolean stop) {
        try {
            BoardSessionManager.session(player.level(), UUID.fromString(rawBoardId))
                    .ifPresent(session -> choose(player, session, stop));
        } catch (IllegalArgumentException ignored) {}
    }

    private static void choose(ServerPlayer player, BoardSession session, boolean stop) {
        BasePlatform.activeBoardEffect(session.id())
                .filter(StartPlatform.class::isInstance)
                .map(StartPlatform.class::cast)
                .ifPresent(platform -> platform.chooseInternal(player, session, stop));
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        StartChoiceState state = this.choices.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }
        if (level.getGameTime() < state.deadlineTick()) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant != null) {
            BoardParticipant timedOut = participant.recordTimedOutDecision();
            if (timedOut != participant) BoardSessionManager.updateParticipant(level, session, timedOut);
            this.resolve(level, session, timedOut, false);
        } else {
            this.choices.remove(session.id());
            this.deactivateBoardEffect(session.id());
        }
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        StartChoiceState state = this.choices.get(session.id());
        if (state == null || !state.slotId().equals(slotId)) return;
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant != null) this.resolve(level, session, participant, true);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.choices.remove(boardId);
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, boolean stop) {
        StartChoiceState state = this.choices.get(session.id());
        if (state == null) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        BoardParticipant manual = participant.recordManualDecision();
        if (manual != participant) {
            BoardSessionManager.updateParticipant(player.level(), session, manual);
            participant = manual;
        }
        this.resolve(player.level(), session, participant, stop);
    }

    private void arrive(BoardPanelContext context) {
        BoardSession.MovementState movement = context.session().movement();
        if (movement == null) return;
        if (context.landing()) {
            this.resolve(context.level(), context.session(), context.participant(), true);
            return;
        }
        if (!context.session().canStopAtStart(context.participant(), context.participant().currentNodeKey())) return;
        if (BoardSessionManager.isAutomated(context.level(), context.participant())) {
            this.resolve(context.level(), context.session(), context.participant(), true);
        } else {
            this.open(context.level(), context.session(), context.participant());
        }
    }

    private void open(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (this.choices.containsKey(session.id())) return;
        ServerPlayer player = participant.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (player == null) {
            this.resolve(level, session, participant, true);
            return;
        }
        int duration = participant.decisionDurationTicks(TIMEOUT_TICKS);
        this.choices.put(session.id(), new StartChoiceState(participant.slotUuid(),
                level.getGameTime() + duration, duration));
        this.activateBoardEffect(session);
        PacketDistributor.sendToPlayer(player, new OpenBoardStartChoicePayload(session.id().toString(),
                participant.stats().health(), participant.stats().maxHealth(), participant.stats().stars(),
                participant.stats().starCoins(), nextStarCost(participant.stats().stars()), duration, duration,
                participant.characterId(), participant.skinId()));
    }

    private void resolve(ServerLevel level, BoardSession session,
                         BoardParticipant participant, boolean stop) {
        this.choices.remove(session.id());
        this.deactivateBoardEffect(session.id());
        BoardSession.MovementState movement = session.movement();
        if (movement == null || !movement.slotId().equals(participant.slotUuid())) return;
        if (stop) {
            session.setMovement(movement.stop());
            BoardParticipant updated = this.applyBenefits(level, session, participant);
            if (this.checkVictory(level, session, updated)) return;
        }
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private BoardParticipant applyBenefits(ServerLevel level, BoardSession session,
                                            BoardParticipant participant) {
        var stats = participant.stats().heal(2);
        int cost = nextStarCost(stats.stars());
        boolean leveled = cost > 0 && stats.starCoins() >= cost && stats.stars() < 3;
        if (leveled) stats = stats.spendCoins(cost).addStars(1);
        BoardParticipant updated = participant.withStats(stats);
        BoardSessionManager.updateParticipant(level, session, updated);
        ServerPlayer player = updated.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_healed", 2), true);
            if (leveled) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.star_up_with_cost",
                        stats.stars(), cost).withStyle(ChatFormatting.GOLD), true);
            }
        }
        return updated;
    }

    private boolean checkVictory(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (participant.stats().stars() < 3) return false;
        String winner = BoardSessionManager.displayName(level, participant);
        for (ServerPlayer player : BoardSessionManager.humanPlayers(level, session)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.victory", winner)
                    .withStyle(ChatFormatting.GOLD), false);
        }
        BoardSessionManager.endGame(level, session, true);
        return true;
    }

    private static int nextStarCost(int stars) {
        int next = Math.clamp(stars + 1, 0, STAR_COSTS.length - 1);
        return next >= 3 && stars >= 3 ? 0 : STAR_COSTS[next];
    }

    private record StartChoiceState(UUID slotId, long deadlineTick, int durationTicks) {}
}
