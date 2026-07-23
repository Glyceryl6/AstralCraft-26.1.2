package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.network.s2c.CloseBoardLotteryNumberPayload;
import com.astral_craft.common.network.s2c.OpenBoardLotteryNumberPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public record BoardSharedLotteryEventEffect(int timeoutTicks) implements BoardEventEffect {

    private static final Set<UUID> ACTIVE_BOARDS = ConcurrentHashMap.newKeySet();

    public static boolean active(UUID boardId) {
        return boardId != null && ACTIVE_BOARDS.contains(boardId);
    }

    public static final MapCodec<BoardSharedLotteryEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("timeout_ticks", 300).forGetter(BoardSharedLotteryEventEffect::timeoutTicks)
    ).apply(instance, BoardSharedLotteryEventEffect::new));

    public BoardSharedLotteryEventEffect() {
        this(300);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_shared_lottery").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        tasks.addLast(new SharedLotteryTask(context, Math.max(20, this.timeoutTicks)));
    }

    private static class SharedLotteryTask implements BoardEventTask {
        private final BoardEventContext context;
        private final Set<UUID> pendingSlots = new LinkedHashSet<>();
        private final int durationTicks;
        private final long deadlineTick;
        private boolean opened;
        private boolean closed;

        private SharedLotteryTask(BoardEventContext context, int durationTicks) {
            this.context = context;
            this.durationTicks = durationTicks;
            this.deadlineTick = AstralServerTickClock.now(context.level()) + durationTicks;
            ACTIVE_BOARDS.add(context.session().id());
            for (BoardParticipant participant : context.session().participants()) {
                List<Integer> available = availableNumbers(context.session(), participant);
                if (available.isEmpty()) continue;
                if (BoardSessionManager.isAutomated(context.level(), participant)) {
                    selectRandom(participant.slotUuid());
                } else {
                    this.pendingSlots.add(participant.slotUuid());
                }
            }
        }

        @Override
        public boolean tick() {
            if (!this.opened) {
                this.opened = true;
                this.broadcast();
            }

            boolean changed = false;
            for (UUID slotId : new ArrayList<>(this.pendingSlots)) {
                BoardParticipant participant = this.context.session().participant(slotId).orElse(null);
                if (participant == null || BoardSessionManager.isAutomated(this.context.level(), participant)) {
                    this.selectRandom(slotId);
                    changed = true;
                }
            }

            if (!this.pendingSlots.isEmpty() && AstralServerTickClock.now(this.context.level()) >= this.deadlineTick) {
                for (UUID slotId : new ArrayList<>(this.pendingSlots)) {
                    BoardParticipant participant = this.context.session().participant(slotId).orElse(null);
                    if (participant != null && !BoardSessionManager.isAutomated(this.context.level(), participant)) {
                        BoardSessionManager.updateParticipant(this.context.level(), this.context.session(), participant.recordTimedOutDecision());
                    }
                    this.selectRandom(slotId);
                }
                changed = true;
            }

            if (this.pendingSlots.isEmpty()) {
                this.close();
                return false;
            }

            if (changed) this.broadcast();
            return true;
        }

        @Override
        public void participantBecameAutomated(UUID slotId) {
            if (!this.pendingSlots.contains(slotId)) return;
            this.selectRandom(slotId);
            if (this.pendingSlots.isEmpty()) this.close();
            else this.broadcast();
        }

        @Override
        public boolean chooseLotteryNumber(ServerPlayer player, int number) {
            BoardParticipant participant = this.context.session().participantByController(player.getUUID()).orElse(null);
            if (participant == null || !this.pendingSlots.contains(participant.slotUuid())) return true;
            if (!availableNumbers(this.context.session(), participant).contains(number)) return true;
            BoardSessionManager.updateParticipant(this.context.level(), this.context.session(), participant.recordManualDecision());
            this.context.session().mechanics().selectLotteryNumber(participant.slotUuid(), number);
            this.pendingSlots.remove(participant.slotUuid());
            BoardSessionManager.markChanged(this.context.level());
            if (this.pendingSlots.isEmpty()) this.close();
            else this.broadcast();
            return true;
        }

        @Override
        public void close() {
            if (this.closed) return;
            this.closed = true;
            ACTIVE_BOARDS.remove(this.context.session().id());
            for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(this.context.level(), this.context.session())) {
                PacketDistributor.sendToPlayer(viewer, new CloseBoardLotteryNumberPayload(this.context.session().id()));
            }
            BoardSessionManager.markChanged(this.context.level());
        }

        private void selectRandom(UUID slotId) {
            BoardParticipant participant = this.context.session().participant(slotId).orElse(null);
            if (participant != null) {
                List<Integer> available = availableNumbers(this.context.session(), participant);
                if (!available.isEmpty()) {
                    int number = available.get(this.context.level().getRandom().nextInt(available.size()));
                    this.context.session().mechanics().selectLotteryNumber(slotId, number);
                }
            }

            this.pendingSlots.remove(slotId);
            BoardSessionManager.markChanged(this.context.level());
        }

        private void broadcast() {
            List<OpenBoardLotteryNumberPayload.Entry> entries = this.context.session().participants().stream().map(participant ->
                    new OpenBoardLotteryNumberPayload.Entry(BoardSessionManager.displayName(this.context.level(), participant),
                            participant.characterId(), participant.skinId(), !this.pendingSlots.contains(participant.slotUuid()))).toList();
            int remaining = (int) Math.max(1L, this.deadlineTick - AstralServerTickClock.now(this.context.level()));
            for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(this.context.level(), this.context.session())) {
                BoardParticipant local = this.context.session().participantByController(viewer.getUUID()).orElse(null);
                boolean canChoose = local != null && this.pendingSlots.contains(local.slotUuid());
                List<Integer> selected = local == null ? List.of()
                        : this.context.session().mechanics().lotteryNumbers(local.slotUuid());
                BoardParticipant fallback = this.context.session().participants().getFirst();
                Identifier characterId = local == null ? fallback.characterId() : local.characterId();
                Identifier skinId = local == null ? fallback.skinId() : local.skinId();
                PacketDistributor.sendToPlayer(viewer, new OpenBoardLotteryNumberPayload(this.context.session().id(), selected,
                        remaining, this.durationTicks, characterId, skinId, true, canChoose, entries));
            }
        }
    }

    private static List<Integer> availableNumbers(BoardSession session, BoardParticipant participant) {
        List<Integer> selected = session.mechanics().lotteryNumbers(participant.slotUuid());
        List<Integer> available = new ArrayList<>();
        for (int number = 1; number <= 12; number++) if (!selected.contains(number)) available.add(number);
        return available;
    }

}