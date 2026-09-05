package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardFortuneService;
import com.astral_craft.common.gameplay.board.BoardMatchmakingService;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardSpectatorService;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.gameplay.fortune.DivinationTarget;
import com.astral_craft.common.network.s2c.OpenBoardDivinationPayload;
import com.astral_craft.common.network.s2c.ResolveBoardDivinationPayload;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DivinePlatform extends BasePlatform {

    public static final int SELECTION_TIMEOUT_TICKS = 20 * 20;
    private static final int REVEAL_TICKS = 50;
    private static final Map<UUID, Execution> EXECUTIONS = new HashMap<>();

    public DivinePlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        List<BoardFortuneDefinition> options = BoardFortuneService.randomDefinitions(context.level(), 2);
        if (options.size() < 2 || EXECUTIONS.containsKey(context.session().id())) {
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }
        int duration = BoardMatchmakingService.decisionDurationTicks(context.session(), context.participant(), SELECTION_TIMEOUT_TICKS);
        Execution execution = Execution.choosing(context.participant().slotUuid(), options,
                AstralServerTickClock.now(context.level()) + duration, duration);
        EXECUTIONS.put(context.session().id(), execution);
        this.activateBoardEffect(context.session());
        this.sendSelection(context.level(), context.session(), context.participant(), execution);
        if (BoardSessionManager.isAutomated(context.level(), context.participant())) {
            this.resolve(context.level(), context.session(), execution, context.level().getRandom().nextInt(options.size()));
        }
        BoardSessionManager.markChanged(context.level());
    }

    public static void choose(ServerPlayer player, UUID boardId, int selectedIndex) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> {
            BasePlatform.activeBoardEffect(boardId).filter(DivinePlatform.class::isInstance)
                    .map(DivinePlatform.class::cast).ifPresent(platform ->
                            platform.choose(player, session, selectedIndex));
        });
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        Execution execution = EXECUTIONS.get(session.id());
        if (execution == null) {
            this.deactivateBoardEffect(session.id());
            BoardSessionManager.resumeMovementAfterPanel(level, session);
            return;
        }
        long now = AstralServerTickClock.now(level);
        if (execution.stage() == Stage.CHOOSING) {
            if (now < execution.deadlineTick()) return;
            BoardParticipant source = session.participant(execution.sourceSlot()).orElse(null);
            if (source != null && !BoardSessionManager.isAutomated(level, source)) {
                BoardSessionManager.updateParticipant(level, session, source.recordTimedOutDecision());
            }
            this.resolve(level, session, execution, level.getRandom().nextInt(execution.options().size()));
            return;
        }
        if (now < execution.deadlineTick()) return;
        BoardParticipant source = session.participant(execution.sourceSlot()).orElse(null);
        BoardFortuneDefinition definition = execution.selectedDefinition();
        if (source != null && definition != null) {
            BoardFortuneService.apply(level, session, source, definition, execution.targetSlots());
        }
        EXECUTIONS.remove(session.id());
        BoardFortuneService.closePresentation(level, session);
        this.deactivateBoardEffect(session.id());
        BoardSessionManager.resumeMovementAfterPanel(level, session);
        BoardSessionManager.markChanged(level);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        Execution execution = EXECUTIONS.get(session.id());
        if (execution == null || execution.stage() != Stage.CHOOSING || !execution.sourceSlot().equals(slotId)) return;
        this.resolve(level, session, execution, level.getRandom().nextInt(execution.options().size()));
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        EXECUTIONS.remove(boardId);
    }

    private void choose(ServerPlayer player, BoardSession session, int selectedIndex) {
        Execution execution = EXECUTIONS.get(session.id());
        BoardParticipant source = execution == null ? null : session.participant(execution.sourceSlot()).orElse(null);
        if (execution == null || execution.stage() != Stage.CHOOSING || source == null
                || !source.controlledBy(player.getUUID()) || selectedIndex < 0
                || selectedIndex >= execution.options().size()) return;
        BoardSessionManager.updateParticipant(player.level(), session, source.recordManualDecision());
        this.resolve(player.level(), session, execution, selectedIndex);
    }

    private void sendSelection(ServerLevel level, BoardSession session, BoardParticipant source, Execution execution) {
        if (BoardSessionManager.isAutomated(level, source)) return;
        ServerPlayer controller = source.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (controller == null) return;
        List<OpenBoardDivinationPayload.Option> views = execution.options().stream()
                .map(OpenBoardDivinationPayload.Option::from).toList();
        int remaining = (int) Math.max(0L, execution.deadlineTick() - AstralServerTickClock.now(level));
        PacketDistributor.sendToPlayer(controller, new OpenBoardDivinationPayload(session.id(), views,
                true, remaining, execution.durationTicks()));
    }

    private void resolve(ServerLevel level, BoardSession session, Execution execution, int selectedIndex) {
        int safeIndex = Math.clamp(selectedIndex, 0, execution.options().size() - 1);
        DivinationTarget target = DivinationTarget.values()[level.getRandom().nextInt(DivinationTarget.values().length)];
        List<UUID> targets = target.select(session.participants()).stream().map(BoardParticipant::slotUuid).toList();
        BoardFortuneDefinition selected = execution.options().get(safeIndex);
        Execution resolved = execution.resolved(safeIndex, target, targets,
                AstralServerTickClock.now(level) + REVEAL_TICKS);
        EXECUTIONS.put(session.id(), resolved);
        ResolveBoardDivinationPayload payload = new ResolveBoardDivinationPayload(session.id(), safeIndex,
                OpenBoardDivinationPayload.Option.from(selected), target);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
        BoardSessionManager.markChanged(level);
    }

    private enum Stage { CHOOSING, REVEALED }

    private record Execution(UUID sourceSlot, List<BoardFortuneDefinition> options, Stage stage,
                             int selectedIndex, @Nullable DivinationTarget target, List<UUID> targetSlots,
                             long deadlineTick, int durationTicks) {
        private Execution {
            options = List.copyOf(options);
            targetSlots = List.copyOf(targetSlots);
        }

        private static Execution choosing(UUID sourceSlot, List<BoardFortuneDefinition> options,
                                          long deadlineTick, int durationTicks) {
            return new Execution(sourceSlot, options, Stage.CHOOSING, -1, null, List.of(),
                    deadlineTick, Math.max(1, durationTicks));
        }

        private Execution resolved(int selectedIndex, DivinationTarget target, List<UUID> targetSlots,
                                   long applyTick) {
            return new Execution(this.sourceSlot, this.options, Stage.REVEALED, selectedIndex, target,
                    targetSlots, applyTick, this.durationTicks);
        }

        private @Nullable BoardFortuneDefinition selectedDefinition() {
            return this.selectedIndex >= 0 && this.selectedIndex < this.options.size()
                    ? this.options.get(this.selectedIndex) : null;
        }
    }
}
