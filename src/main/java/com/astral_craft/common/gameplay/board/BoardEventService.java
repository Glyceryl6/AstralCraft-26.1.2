package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.event.AstralEventDefinition;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.event.AstralEventManager;
import com.astral_craft.common.gameplay.event.AstralEventService;
import com.astral_craft.common.gameplay.event.effects.BoardEventEffect;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.registry.bootstrap.AstralEventBootstrap;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Selects one board event, owns its reveal lock and runs its data-generated task sequence. */
public class BoardEventService {

    private static final Map<UUID, EventExecution> PANEL_EVENTS = new HashMap<>();
    private static final Map<UUID, EventExecution> ROUND_EVENTS = new HashMap<>();

    public static boolean trigger(ServerLevel level, BoardSession session, BoardParticipant source) {
        if (level == null || session == null || source == null || PANEL_EVENTS.containsKey(session.id())) return false;
        List<AstralEventDefinition> candidates = AstralEventBootstrap.BOARD_EVENTS.stream()
                .map(ResourceKey::identifier).map(AstralEventManager.INSTANCE::get)
                .filter(Objects::nonNull).toList();
        if (candidates.isEmpty()) return false;
        return begin(level, session, source, candidates.get(level.getRandom().nextInt(candidates.size())));
    }

    public static boolean triggerById(ServerPlayer player, Identifier eventId) {
        if (player == null || !isBoardEvent(eventId)) return false;
        BoardSession session = BoardSessionManager.findByController(player).orElse(null);
        BoardParticipant source = session == null ? null : session.participantByController(player.getUUID()).orElse(null);
        AstralEventDefinition definition = AstralEventManager.INSTANCE.get(eventId);
        if (source == null || definition == null || BasePlatform.hasActiveBoardEffect(session.id())
                || BoardLotteryService.active(session.id()) || BoardBattleService.active(session.id())
                || session.encounter() != null || session.discard() != null || session.movement() != null) return false;
        return begin(player.level(), session, source, definition);
    }

    public static boolean isBoardEvent(Identifier eventId) {
        return eventId != null && AstralEventBootstrap.BOARD_EVENTS.stream()
                .anyMatch(key -> key.identifier().equals(eventId));
    }

    private static boolean begin(ServerLevel level, BoardSession session, BoardParticipant source,
                                 AstralEventDefinition definition) {
        if (PANEL_EVENTS.containsKey(session.id()) || ROUND_EVENTS.containsKey(session.id())
                || BoardLotteryService.active(session.id())) return false;
        int revealTicks = AstralEventService.DEFAULT_EVENT_REVEAL_DURATION_TICKS;
        BoardEventContext context = new BoardEventContext(level, session, source, definition);
        PANEL_EVENTS.put(session.id(), EventExecution.revealed(context,
                AstralServerTickClock.now(level) + revealTicks + 2L));
        broadcastReveal(context, revealTicks);
        BoardSessionManager.markChanged(level);
        return true;
    }

    public static boolean tick(ServerLevel level, BoardSession session) {
        EventExecution execution = PANEL_EVENTS.get(session.id());
        if (execution == null) return false;
        if (execution.tick()) return true;
        PANEL_EVENTS.remove(session.id());
        BoardSessionManager.markChanged(level);
        return false;
    }

    public static boolean beginRoundEffects(ServerLevel level, BoardSession session) {
        if (ROUND_EVENTS.containsKey(session.id())) return true;
        BoardParticipant source = session.currentParticipant().orElse(session.participants().isEmpty()
                ? null : session.participants().getFirst());
        if (source == null) return false;
        Deque<BoardEventTask> tasks = new ArrayDeque<>();
        for (Map.Entry<Identifier, Integer> entry : new ArrayList<>(session.mechanics().timedEvents().entrySet())) {
            AstralEventDefinition definition = AstralEventManager.INSTANCE.get(entry.getKey());
            if (definition != null && !definition.intervalEffects().isEmpty()) {
                enqueueEffects(new BoardEventContext(level, session, source, definition), definition.intervalEffects(), tasks);
            }

            session.mechanics().tickTimedEvent(entry.getKey());
        }

        BoardSessionManager.markChanged(level);
        if (tasks.isEmpty()) return false;
        ROUND_EVENTS.put(session.id(), EventExecution.immediate(tasks));
        return true;
    }

    public static boolean hasRoundExecution(UUID boardId) {
        return ROUND_EVENTS.containsKey(boardId);
    }

    public static boolean active(UUID boardId) {
        return boardId != null && (PANEL_EVENTS.containsKey(boardId) || ROUND_EVENTS.containsKey(boardId));
    }

    public static boolean tickRoundEffects(ServerLevel level, BoardSession session) {
        EventExecution execution = ROUND_EVENTS.get(session.id());
        if (execution == null) return false;
        if (execution.tick()) return true;
        ROUND_EVENTS.remove(session.id());
        BoardSessionManager.markChanged(level);
        return false;
    }

    public static void participantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        EventExecution execution = PANEL_EVENTS.get(session.id());
        if (execution != null) execution.participantBecameAutomated(slotId);
    }

    public static boolean chooseLotteryNumber(ServerPlayer player, UUID boardId, int number) {
        EventExecution execution = PANEL_EVENTS.get(boardId);
        return execution != null && execution.chooseLotteryNumber(player, number);
    }

    public static void clear(UUID boardId) {
        EventExecution panel = PANEL_EVENTS.remove(boardId);
        if (panel != null) panel.close();
        EventExecution round = ROUND_EVENTS.remove(boardId);
        if (round != null) round.close();
    }

    public static int shopOfferBonus(BoardSession session) {
        return session != null && session.mechanics().timedEventTurns(AstralEventBootstrap.BIG_SALES.identifier()) > 0 ? 2 : 0;
    }

    private static void enqueueEffects(BoardEventContext context, List<AstralEventEffect> effects,
                                       Deque<BoardEventTask> tasks) {
        for (AstralEventEffect effect : effects) {
            if (effect instanceof BoardEventEffect boardEffect) {
                boardEffect.enqueue(context, tasks);
            } else if (effect != null && !BoardMatchmakingService.tutorialProtected(context.session(), context.source())) {
                tasks.addLast(BoardEventTask.action(() -> effect.apply(context.astralContext(context.source())), 0));
            }
        }
    }

    private static void broadcastReveal(BoardEventContext context, int duration) {
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(context.level(), context.session())) {
            AstralEventDefinition definition = context.definition();
            PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(definition.id().toString(), ItemStack.EMPTY,
                    CardType.EVENT.getSerializedName(), Component.translatable(definition.nameKey()),
                    Component.translatable(definition.descriptionKey()), definition.texture(),
                    CardBackPreferenceManager.selectedTexture(viewer), CardRevealPayload.ANIMATION_APPROACH,
                    duration, -1, List.of(), false));
        }
    }

    private static class EventExecution {
        private final BoardEventContext context;
        private final Deque<BoardEventTask> tasks;
        private final long revealUntil;
        private boolean prepared;

        private EventExecution(BoardEventContext context, Deque<BoardEventTask> tasks, long revealUntil, boolean prepared) {
            this.context = context;
            this.tasks = tasks;
            this.revealUntil = revealUntil;
            this.prepared = prepared;
        }

        private static EventExecution revealed(BoardEventContext context, long revealUntil) {
            return new EventExecution(context, new ArrayDeque<>(), revealUntil, false);
        }

        private static EventExecution immediate(Deque<BoardEventTask> tasks) {
            return new EventExecution(null, tasks, 0L, true);
        }

        private boolean tick() {
            if (!this.prepared) {
                if (AstralServerTickClock.now(this.context.level()) < this.revealUntil) return true;
                enqueueEffects(this.context, this.context.definition().effects(), this.tasks);
                this.prepared = true;
            }
            BoardEventTask task = this.tasks.peekFirst();
            if (task == null) return false;
            if (task.tick()) return true;
            task.close();
            this.tasks.removeFirst();
            return !this.tasks.isEmpty();
        }

        private void participantBecameAutomated(UUID slotId) {
            BoardEventTask task = this.tasks.peekFirst();
            if (task != null) task.participantBecameAutomated(slotId);
        }

        private boolean chooseLotteryNumber(ServerPlayer player, int number) {
            BoardEventTask task = this.tasks.peekFirst();
            return task != null && task.chooseLotteryNumber(player, number);
        }

        private void close() {
            for (BoardEventTask task : this.tasks) task.close();
            this.tasks.clear();
        }
    }
}
