package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.projectile.FirecrackersProjectileEntity;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.s2c.CloseBoardPlatformTargetPayload;
import com.astral_craft.common.network.s2c.OpenBoardPlatformTargetPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.ParametersAreNullableByDefault;
import java.util.*;

public class FirePlatform extends BasePlatform {

    private static final int TARGET_TIMEOUT_TICKS = 20 * 10;
    private static final int PROJECTILE_TICKS = 28;
    private final Map<UUID, FireState> states = new HashMap<>();

    public FirePlatform(Block.Properties properties) {
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
            BoardParticipant target = targets.get(context.level().getRandom().nextInt(targets.size()));
            this.fire(context.level(), context.session(), context.participant(), target);
            return;
        }

        ServerPlayer player = context.participant().controllerUuid()
                .map(context.level().getServer().getPlayerList()::getPlayer).orElse(null);
        if (player == null) {
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }

        int duration = context.participant().decisionDurationTicks(TARGET_TIMEOUT_TICKS);
        this.states.put(context.session().id(), new FireState(context.participant().slotUuid(), null,
                AstralServerTickClock.now(context.level()) + duration));
        this.activateBoardEffect(context.session());
        PacketDistributor.sendToPlayer(player, new OpenBoardPlatformTargetPayload(context.session().id(),
                OpenBoardPlatformTargetPayload.Action.FIRE, this.candidates(context.level(), context.session(), targets),
                duration, duration));
    }

    @Override
    public void handleBoardTargetSelection(ServerPlayer player, BoardSession session, int entityId) {
        this.chooseInternal(player, session, entityId);
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        FireState state = this.states.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }

        if (AstralServerTickClock.now(level) < state.deadlineTick()) return;
        this.finish(level, session, false);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        FireState state = this.states.get(session.id());
        if (state == null || state.targetSlotId() != null || !state.sourceSlotId().equals(slotId)) return;
        BoardParticipant source = session.participant(slotId).orElse(null);
        if (source == null) {
            this.finish(level, session, true);
            return;
        }
        List<BoardParticipant> targets = this.targets(session, source);
        if (targets.isEmpty()) this.finish(level, session, true);
        else this.fire(level, session, source, targets.get(level.getRandom().nextInt(targets.size())));
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.states.remove(boardId);
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, int entityId) {
        FireState state = this.states.get(session.id());
        if (state == null || state.targetSlotId() != null) return;
        BoardParticipant source = session.participant(state.sourceSlotId()).orElse(null);
        if (source == null || !source.controlledBy(player.getUUID())) return;
        if (entityId < 0) {
            BoardSessionManager.updateParticipant(player.level(), session, source.recordManualDecision());
            this.finish(player.level(), session, true);
            return;
        }

        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof AstralCharacterEntity character)) return;
        BoardParticipant target = session.participantFor(character).orElse(null);
        if (target == null || this.targets(session, source).stream()
                .noneMatch(candidate -> candidate.slotUuid().equals(target.slotUuid()))) return;
        BoardSessionManager.updateParticipant(player.level(), session, source.recordManualDecision());
        this.fire(player.level(), session, source, target);
    }

    private void fire(ServerLevel level, BoardSession session, BoardParticipant source, BoardParticipant target) {
        AstralCharacterEntity sourceEntity = BoardEntityService.entity(level, source);
        AstralCharacterEntity targetEntity = BoardEntityService.entity(level, target);
        if (sourceEntity == null || targetEntity == null) {
            this.finish(level, session, true);
            return;
        }

        this.closeSelector(level, source, session.id());
        level.playSound(null, sourceEntity.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.75F);
        level.addFreshEntity(new FirecrackersProjectileEntity(level, sourceEntity, targetEntity, 2, PROJECTILE_TICKS - 4));
        this.states.put(session.id(), new FireState(source.slotUuid(), target.slotUuid(), AstralServerTickClock.now(level) + PROJECTILE_TICKS));
        this.activateBoardEffect(session);
    }

    private void finish(ServerLevel level, BoardSession session, boolean closeSelector) {
        FireState state = this.states.remove(session.id());
        this.deactivateBoardEffect(session.id());
        if (closeSelector && state != null) {
            session.participant(state.sourceSlotId()).ifPresent(source -> this.closeSelector(level, source, session.id()));
        }

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
                .sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
    }

    private List<CardTargetCandidate> candidates(ServerLevel level, BoardSession session, List<BoardParticipant> targets) {
        return targets.stream().map(target -> {
            AstralCharacterEntity entity = BoardEntityService.entity(level, target);
            return entity == null ? null : new CardTargetCandidate(entity.getId(), entity.getDisplayName().copy(), 0);
        }).filter(Objects::nonNull).toList();
    }

    @ParametersAreNullableByDefault
    private record FireState(UUID sourceSlotId, UUID targetSlotId, long deadlineTick) {}

}