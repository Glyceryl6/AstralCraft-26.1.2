package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.network.s2c.CloseBoardHospitalPayload;
import com.astral_craft.common.network.s2c.OpenBoardHospitalPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HospitalPlatform extends BasePlatform {

    public static final Identifier HOSPITALIZED_STATUS = AstralCraft.prefix("hospitalized");
    private static final int CHECKING_TICKS = 20 * 5;
    private static final int RESULT_TICKS = 20 * 3;
    private final Map<UUID, HospitalState> states = new HashMap<>();

    public HospitalPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        BoardParticipant healed = context.participant().withStats(context.participant().stats().heal(2));
        BoardSessionManager.updateParticipant(context.level(), context.session(), healed);
        OpenBoardHospitalPayload.Result result = context.level().getRandom().nextBoolean()
                ? OpenBoardHospitalPayload.Result.INJECTION : OpenBoardHospitalPayload.Result.HOSPITALIZED;
        boolean automated = BoardSessionManager.isAutomated(context.level(), healed);
        OpenBoardHospitalPayload.Phase phase = automated
                ? OpenBoardHospitalPayload.Phase.RESULT : OpenBoardHospitalPayload.Phase.CHECKING;
        int duration = automated ? RESULT_TICKS : CHECKING_TICKS;
        HospitalState state = new HospitalState(healed.slotUuid(), phase, result,
                AstralServerTickClock.now(context.level()) + duration, duration);
        this.states.put(context.session().id(), state);
        this.activateBoardEffect(context.session());
        this.broadcast(context.level(), context.session(), state);
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        HospitalState state = this.states.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }

        if (AstralServerTickClock.now(level) < state.deadlineTick()) return;
        if (state.phase() == OpenBoardHospitalPayload.Phase.CHECKING) {
            HospitalState result = new HospitalState(state.slotId(), OpenBoardHospitalPayload.Phase.RESULT,
                    state.result(), AstralServerTickClock.now(level) + RESULT_TICKS, RESULT_TICKS);
            this.states.put(session.id(), result);
            this.broadcast(level, session, result);
            return;
        }

        this.finish(level, session, state);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        HospitalState state = this.states.get(session.id());
        if (state == null || !state.slotId().equals(slotId)
                || state.phase() == OpenBoardHospitalPayload.Phase.RESULT) return;
        HospitalState result = new HospitalState(state.slotId(), OpenBoardHospitalPayload.Phase.RESULT,
                state.result(), AstralServerTickClock.now(level) + RESULT_TICKS, RESULT_TICKS);
        this.states.put(session.id(), result);
        this.broadcast(level, session, result);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.states.remove(boardId);
    }

    private void finish(ServerLevel level, BoardSession session, HospitalState state) {
        this.states.remove(session.id());
        this.deactivateBoardEffect(session.id());
        if (state.result() == OpenBoardHospitalPayload.Result.HOSPITALIZED) {
            session.participant(state.slotId()).ifPresent(participant -> BoardSessionManager.updateParticipant(level, session,
                    participant.withRoundStatusEffect(HOSPITALIZED_STATUS, 1)));
        }

        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardHospitalPayload(session.id()));
        }

        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private void broadcast(ServerLevel level, BoardSession session, HospitalState state) {
        int remaining = (int) Math.max(0L, state.deadlineTick() - AstralServerTickClock.now(level));
        OpenBoardHospitalPayload payload = new OpenBoardHospitalPayload(session.id(), state.phase(), state.result(), remaining, state.durationTicks());
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private record HospitalState(UUID slotId, OpenBoardHospitalPayload.Phase phase, OpenBoardHospitalPayload.Result result, long deadlineTick, int durationTicks) {}

}