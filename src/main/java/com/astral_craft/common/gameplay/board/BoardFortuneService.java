package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.gameplay.fortune.BoardFortuneManager;
import com.astral_craft.common.gameplay.fortune.DivinationTarget;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.network.s2c.CloseBoardPresentationPayload;
import com.astral_craft.common.network.s2c.OpenBoardDivinationPayload;
import com.astral_craft.common.network.s2c.ResolveBoardDivinationPayload;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardFortuneService {

    public static final int DIVINATION_TIMEOUT_TICKS = 20 * 20;
    private static final int DESTINY_REVEAL_TICKS = 48;
    private static final int DIVINATION_REVEAL_TICKS = 34;
    private static final Map<UUID, Execution> EXECUTIONS = new HashMap<>();

    public static boolean triggerDestiny(ServerLevel level, BoardSession session, BoardParticipant source) {
        if (!canStart(session)) return false;
        BoardFortuneDefinition definition = randomDefinition(level);
        if (definition == null) return false;
        EXECUTIONS.put(session.id(), Execution.destiny(source.slotUuid(), definition,
                AstralServerTickClock.now(level) + DESTINY_REVEAL_TICKS + 2L));
        broadcastDestiny(level, session, source, definition);
        BoardSessionManager.markChanged(level);
        return true;
    }

    public static boolean triggerDivination(ServerLevel level, BoardSession session, BoardParticipant source) {
        if (!canStart(session)) return false;
        List<BoardFortuneDefinition> options = randomDefinitions(level, 2);
        if (options.size() < 2) return false;
        int duration = source.decisionDurationTicks(DIVINATION_TIMEOUT_TICKS);
        Execution execution = Execution.choosing(source.slotUuid(), options,
                AstralServerTickClock.now(level) + duration, duration);
        EXECUTIONS.put(session.id(), execution);
        sendDivination(level, session, source, execution);
        if (BoardSessionManager.isAutomated(level, source)) {
            resolve(level, session, execution, level.getRandom().nextInt(options.size()));
        }
        BoardSessionManager.markChanged(level);
        return true;
    }

    public static void choose(ServerPlayer player, UUID boardId, int selectedIndex) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> {
            Execution execution = EXECUTIONS.get(boardId);
            BoardParticipant source = execution == null ? null : session.participant(execution.sourceSlotId()).orElse(null);
            if (execution == null || execution.stage() != Stage.CHOOSING || source == null
                    || !source.controlledBy(player.getUUID()) || selectedIndex < 0
                    || selectedIndex >= execution.options().size()) return;
            BoardSessionManager.updateParticipant(player.level(), session, source.recordManualDecision());
            resolve(player.level(), session, execution, selectedIndex);
        });
    }

    public static boolean tick(ServerLevel level, BoardSession session) {
        Execution execution = EXECUTIONS.get(session.id());
        if (execution == null) return false;
        long now = AstralServerTickClock.now(level);
        if (execution.stage() == Stage.CHOOSING) {
            if (now < execution.deadlineTick()) return true;
            BoardParticipant source = session.participant(execution.sourceSlotId()).orElse(null);
            if (source != null && !BoardSessionManager.isAutomated(level, source)) {
                BoardSessionManager.updateParticipant(level, session, source.recordTimedOutDecision());
            }
            resolve(level, session, execution, level.getRandom().nextInt(execution.options().size()));
            return true;
        }

        if (now < execution.deadlineTick()) return true;
        apply(level, session, execution);
        EXECUTIONS.remove(session.id());
        close(level, session);
        BoardSessionManager.markChanged(level);
        return false;
    }

    public static void participantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        Execution execution = EXECUTIONS.get(session.id());
        if (execution == null || execution.stage() != Stage.CHOOSING || !execution.sourceSlotId().equals(slotId)) return;
        resolve(level, session, execution, level.getRandom().nextInt(execution.options().size()));
    }

    public static void clear(ServerLevel level, BoardSession session) {
        if (session == null || EXECUTIONS.remove(session.id()) == null) return;
        close(level, session);
    }

    public static void clear(UUID boardId) {
        if (boardId != null) EXECUTIONS.remove(boardId);
    }

    private static boolean canStart(BoardSession session) {
        return session != null && !EXECUTIONS.containsKey(session.id());
    }

    private static void resolve(ServerLevel level, BoardSession session, Execution execution, int selectedIndex) {
        int safeIndex = Math.clamp(selectedIndex, 0, execution.options().size() - 1);
        DivinationTarget target = DivinationTarget.values()[level.getRandom().nextInt(DivinationTarget.values().length)];
        List<UUID> targets = target.select(session.participants()).stream().map(BoardParticipant::slotUuid).toList();
        Execution resolved = execution.resolved(safeIndex, target, targets,
                AstralServerTickClock.now(level) + DIVINATION_REVEAL_TICKS);
        EXECUTIONS.put(session.id(), resolved);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new ResolveBoardDivinationPayload(session.id(), safeIndex, target));
        }

        BoardSessionManager.markChanged(level);
    }

    private static void apply(ServerLevel level, BoardSession session, Execution execution) {
        BoardFortuneDefinition definition = execution.selectedDefinition();
        if (definition == null) return;
        BoardParticipant source = session.participant(execution.sourceSlotId()).orElse(null);
        ServerPlayer triggerPlayer = source == null ? null : source.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        List<UUID> targetIds = execution.targetSlots().isEmpty()
                ? List.of(execution.sourceSlotId()) : execution.targetSlots();
        for (UUID targetId : targetIds) {
            BoardParticipant target = session.participant(targetId).orElse(null);
            AstralCharacterEntity pawn = target == null ? null : BoardEntityService.entity(level, target);
            if (target == null || pawn == null || target.knockedDown()) continue;
            AstralEventContext context = new AstralEventContext(triggerPlayer, pawn, level, null,
                    pawn.blockPosition(), null, null, null, 0.0F, false);
            for (AstralEventEffect effect : definition.effects()) {
                if (effect != null) effect.apply(context);
            }
        }

        BoardSessionManager.syncBoardSnapshot(level, session);
    }

    private static void broadcastDestiny(ServerLevel level, BoardSession session, BoardParticipant source, BoardFortuneDefinition definition) {
        AstralCharacterEntity pawn = BoardEntityService.entity(level, source);
        int sourceEntityId = pawn == null ? -1 : pawn.getId();
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(definition.id().toString(), ItemStack.EMPTY,
                    CardType.EVENT.getSerializedName(), Component.translatable(definition.nameKey()),
                    Component.translatable(definition.descriptionKey()), definition.texture(),
                    CardBackPreferenceManager.selectedTexture(viewer), CardRevealPayload.ANIMATION_APPROACH,
                    DESTINY_REVEAL_TICKS, sourceEntityId, List.of(sourceEntityId), false));
        }
    }

    private static void sendDivination(ServerLevel level, BoardSession session, BoardParticipant source, Execution execution) {
        List<OpenBoardDivinationPayload.Option> views = execution.options().stream()
                .map(definition -> new OpenBoardDivinationPayload.Option(definition.id(), definition.nameKey(),
                        definition.descriptionKey(), definition.texture())).toList();
        int remaining = (int) Math.max(0L, execution.deadlineTick() - AstralServerTickClock.now(level));
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new OpenBoardDivinationPayload(session.id(), views,
                    source.controlledBy(viewer.getUUID()), remaining, execution.durationTicks()));
        }
    }

    private static void close(ServerLevel level, BoardSession session) {
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardPresentationPayload(session.id()));
        }
    }

    private static BoardFortuneDefinition randomDefinition(ServerLevel level) {
        List<BoardFortuneDefinition> values = BoardFortuneManager.INSTANCE.values();
        if (values.isEmpty()) return null;
        int total = values.stream().mapToInt(BoardFortuneDefinition::weight).sum();
        int roll = level.getRandom().nextInt(Math.max(1, total));
        for (BoardFortuneDefinition definition : values) {
            roll -= definition.weight();
            if (roll < 0) return definition;
        }
        return values.getLast();
    }

    private static List<BoardFortuneDefinition> randomDefinitions(ServerLevel level, int count) {
        List<BoardFortuneDefinition> pool = new ArrayList<>(BoardFortuneManager.INSTANCE.values());
        List<BoardFortuneDefinition> result = new ArrayList<>();
        while (!pool.isEmpty() && result.size() < count) {
            int total = pool.stream().mapToInt(BoardFortuneDefinition::weight).sum();
            int roll = level.getRandom().nextInt(Math.max(1, total));
            BoardFortuneDefinition selected = pool.getLast();
            for (BoardFortuneDefinition definition : pool) {
                roll -= definition.weight();
                if (roll < 0) {
                    selected = definition;
                    break;
                }
            }

            result.add(selected);
            pool.remove(selected);
        }
        return List.copyOf(result);
    }

    private enum Stage { CHOOSING, REVEALED }

    private record Execution(UUID sourceSlotId, List<BoardFortuneDefinition> options, Stage stage,
                             int selectedIndex, DivinationTarget target, List<UUID> targetSlots,
                             long deadlineTick, int durationTicks) {
        private Execution {
            options = List.copyOf(options);
            targetSlots = List.copyOf(targetSlots);
        }

        private static Execution destiny(UUID sourceSlotId, BoardFortuneDefinition definition, long applyTick) {
            return new Execution(sourceSlotId, List.of(definition), Stage.REVEALED, 0, null, List.of(sourceSlotId), applyTick, DESTINY_REVEAL_TICKS);
        }

        private static Execution choosing(UUID sourceSlotId, List<BoardFortuneDefinition> options, long deadlineTick, int durationTicks) {
            return new Execution(sourceSlotId, options, Stage.CHOOSING, -1, null, List.of(), deadlineTick, Math.max(1, durationTicks));
        }

        private Execution resolved(int selectedIndex, DivinationTarget target, List<UUID> targetSlots, long applyTick) {
            return new Execution(this.sourceSlotId, this.options, Stage.REVEALED, selectedIndex, target, targetSlots, applyTick, this.durationTicks);
        }

        private BoardFortuneDefinition selectedDefinition() {
            return this.selectedIndex >= 0 && this.selectedIndex < this.options.size() ? this.options.get(this.selectedIndex) : null;
        }

    }

}