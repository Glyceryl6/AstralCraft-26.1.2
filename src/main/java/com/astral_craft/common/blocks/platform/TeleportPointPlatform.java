package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.s2c.CloseBoardPlatformTargetPayload;
import com.astral_craft.common.network.s2c.OpenBoardPlatformTargetPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class TeleportPointPlatform extends BasePlatform {

    private static final int TARGET_TIMEOUT_TICKS = 20 * 10;
    private final Map<UUID, AssaultState> states = new HashMap<>();

    public TeleportPointPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        List<BoardParticipant> targets = this.targets(context.session(), context.participant());
        if (targets.isEmpty()) {
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }

        if (BoardSessionManager.isAutomated(context.level(), context.participant())) {
            this.assault(context.level(), context.session(), context.participant(),
                    targets.get(context.level().getRandom().nextInt(targets.size())));
            return;
        }

        ServerPlayer player = context.participant().controllerUuid()
                .map(context.level().getServer().getPlayerList()::getPlayer).orElse(null);
        if (player == null) {
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }

        int duration = context.participant().decisionDurationTicks(TARGET_TIMEOUT_TICKS);
        this.states.put(context.session().id(), new AssaultState(context.participant().slotUuid(),
                context.level().getGameTime() + duration));
        this.activateBoardEffect(context.session());
        PacketDistributor.sendToPlayer(player, new OpenBoardPlatformTargetPayload(context.session().id(),
                OpenBoardPlatformTargetPayload.Action.ASSAULT, this.candidates(context.level(), targets), duration, duration));
    }

    public static void choose(ServerPlayer player, UUID boardId, int entityId) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> BasePlatform.activeBoardEffect(boardId)
                .filter(TeleportPointPlatform.class::isInstance).map(TeleportPointPlatform.class::cast)
                .ifPresent(platform -> platform.chooseInternal(player, session, entityId)));
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        AssaultState state = this.states.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }

        if (level.getGameTime() >= state.deadlineTick()) this.cancel(level, session, state.sourceSlotId());
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        AssaultState state = this.states.get(session.id());
        if (state == null || !state.sourceSlotId().equals(slotId)) return;
        BoardParticipant source = session.participant(slotId).orElse(null);
        if (source == null) {
            this.cancel(level, session, slotId);
            return;
        }

        List<BoardParticipant> targets = this.targets(session, source);
        if (targets.isEmpty()) this.cancel(level, session, slotId);
        else this.assault(level, session, source, targets.get(level.getRandom().nextInt(targets.size())));
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.states.remove(boardId);
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, int entityId) {
        AssaultState state = this.states.get(session.id());
        if (state == null) return;
        BoardParticipant source = session.participant(state.sourceSlotId()).orElse(null);
        if (source == null || !source.controlledBy(player.getUUID())) return;
        BoardSessionManager.updateParticipant(player.level(), session, source.recordManualDecision());
        if (entityId < 0) {
            this.cancel(player.level(), session, source.slotUuid());
            return;
        }

        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof AstralCharacterEntity character)) return;
        BoardParticipant target = session.participantFor(character).orElse(null);
        if (target == null || this.targets(session, source).stream()
                .noneMatch(candidate -> candidate.slotUuid().equals(target.slotUuid()))) return;
        this.assault(player.level(), session, source, target);
    }

    private void assault(ServerLevel level, BoardSession session, BoardParticipant source, BoardParticipant target) {
        this.states.remove(session.id());
        this.deactivateBoardEffect(session.id());
        this.closeSelector(level, source, session.id());
        BoardSessionManager.relocateParticipant(level, session, source, target.currentNodeKey());
        BoardParticipant relocated = session.participant(source.slotUuid()).orElse(source);
        BoardSessionManager.beginPanelEncounter(level, session, relocated, target);
    }

    private void cancel(ServerLevel level, BoardSession session, UUID sourceSlotId) {
        this.states.remove(session.id());
        this.deactivateBoardEffect(session.id());
        session.participant(sourceSlotId).ifPresent(source -> this.closeSelector(level, source, session.id()));
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private void closeSelector(ServerLevel level, BoardParticipant source, UUID boardId) {
        source.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                .ifPresent(player -> PacketDistributor.sendToPlayer(player, new CloseBoardPlatformTargetPayload(boardId)));
    }

    private List<BoardParticipant> targets(BoardSession session, BoardParticipant source) {
        return session.participants().stream()
                .filter(target -> !target.slotUuid().equals(source.slotUuid()))
                .filter(target -> !target.knockedDown())
                .filter(target -> !BoardSessionManager.isHospitalProtected(session, target))
                .sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
    }

    private List<CardTargetCandidate> candidates(ServerLevel level, List<BoardParticipant> targets) {
        return targets.stream().map(target -> {
            AstralCharacterEntity entity = BoardEntityService.entity(level, target);
            return entity == null ? null : new CardTargetCandidate(entity.getId(), entity.getDisplayName().copy(), 0);
        }).filter(Objects::nonNull).toList();
    }

    private record AssaultState(UUID sourceSlotId, long deadlineTick) {}

}