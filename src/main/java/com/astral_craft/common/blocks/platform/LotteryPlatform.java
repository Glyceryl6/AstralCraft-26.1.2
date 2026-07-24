package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.network.s2c.OpenBoardLotteryNumberPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LotteryPlatform extends BasePlatform {

    private static final int TIMEOUT_TICKS = 20 * 10;
    private final Map<UUID, LotteryChoiceState> choices = new HashMap<>();

    public LotteryPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public Component boardActionPrompt(Component actorName) {
        return Component.translatable("message.astral_craft.board.prompt.lucky_number", actorName);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        List<Integer> available = availableNumbers(context.session(), context.participant());
        if (available.isEmpty()) {
            context.participant().controllerUuid().map(context.level().getServer().getPlayerList()::getPlayer)
                    .ifPresent(player -> player.sendSystemMessage(Component.translatable(
                            "message.astral_craft.board.lottery.all_selected"), true));
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }

        if (BoardSessionManager.isAutomated(context.level(), context.participant())) {
            this.resolve(context.level(), context.session(), context.participant(),
                    available.get(context.level().getRandom().nextInt(available.size())));
        } else {
            this.open(context.level(), context.session(), context.participant());
        }
    }

    public static void choose(ServerPlayer player, UUID boardId, int number) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> BasePlatform.activeBoardEffect(boardId)
                .filter(LotteryPlatform.class::isInstance).map(LotteryPlatform.class::cast)
                .ifPresent(platform -> platform.chooseInternal(player, session, number)));
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        LotteryChoiceState state = this.choices.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }

        if (AstralServerTickClock.now(level) < state.deadlineTick()) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null) {
            this.choices.remove(session.id());
            this.deactivateBoardEffect(session.id());
            return;
        }

        List<Integer> available = availableNumbers(session, participant);
        if (!available.isEmpty()) {
            BoardSessionManager.updateParticipant(level, session, participant.recordTimedOutDecision());
            this.resolve(level, session, participant, available.get(level.getRandom().nextInt(available.size())));
        } else {
            this.resolve(level, session, participant, 0);
        }
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        LotteryChoiceState state = this.choices.get(session.id());
        if (state == null || !state.slotId().equals(slotId)) return;
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null) return;
        List<Integer> available = availableNumbers(session, participant);
        this.resolve(level, session, participant, available.isEmpty() ? 0
                : available.get(level.getRandom().nextInt(available.size())));
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.choices.remove(boardId);
    }

    private void open(ServerLevel level, BoardSession session, BoardParticipant participant) {
        ServerPlayer player = participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (player == null) {
            List<Integer> available = availableNumbers(session, participant);
            this.resolve(level, session, participant, available.isEmpty() ? 0
                    : available.get(level.getRandom().nextInt(available.size())));
            return;
        }

        int duration = participant.decisionDurationTicks(TIMEOUT_TICKS);
        this.choices.put(session.id(), new LotteryChoiceState(participant.slotUuid(),
                AstralServerTickClock.now(level) + duration, duration));
        this.activateBoardEffect(session);
        PacketDistributor.sendToPlayer(player, new OpenBoardLotteryNumberPayload(session.id(),
                session.mechanics().lotteryNumbers(participant.slotUuid()), duration, duration,
                participant.characterId(), participant.skinId()));
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, int number) {
        LotteryChoiceState state = this.choices.get(session.id());
        if (state == null) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        if (!availableNumbers(session, participant).contains(number)) return;
        BoardSessionManager.updateParticipant(player.level(), session, participant.recordManualDecision());
        this.resolve(player.level(), session, participant, number);
    }

    private void resolve(ServerLevel level, BoardSession session, BoardParticipant participant, int number) {
        this.choices.remove(session.id());
        this.deactivateBoardEffect(session.id());
        if (number >= 1 && number <= 12 && session.mechanics().selectLotteryNumber(participant.slotUuid(), number)) {
            BoardSessionManager.markChanged(level);
            MutableComponent component = Component.translatable("message.astral_craft.board.lottery.selected", number);
            participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                    .ifPresent(player -> player.sendSystemMessage(component, true));
        }

        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private static List<Integer> availableNumbers(BoardSession session, BoardParticipant participant) {
        List<Integer> selected = session.mechanics().lotteryNumbers(participant.slotUuid());
        List<Integer> available = new ArrayList<>();
        for (int number = 1; number <= 12; number++) {
            if (!selected.contains(number)) available.add(number);
        }

        return available;
    }

    private record LotteryChoiceState(UUID slotId, long deadlineTick, int durationTicks) {}

}