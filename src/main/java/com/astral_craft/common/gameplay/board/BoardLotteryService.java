package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.network.s2c.CloseBoardLotteryDrawPayload;
import com.astral_craft.common.network.s2c.OpenBoardLotteryDrawPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class BoardLotteryService {

    private static final int ROLL_TICKS = 20 * 5;
    private static final int RESULT_TICKS = 20 * 4;
    private static final Map<UUID, DrawState> ACTIVE_DRAWS = new HashMap<>();

    public static boolean begin(ServerLevel level, BoardSession session, int roundNumber) {
        if (roundNumber <= 0 || roundNumber % 5 != 0 || ACTIVE_DRAWS.containsKey(session.id())) return false;
        int result = level.getRandom().nextInt(12) + 1;
        DrawState state = new DrawState(OpenBoardLotteryDrawPayload.Phase.ROLLING, result,
                session.mechanics().lotteryJackpot(), List.of(), 0,
                level.getGameTime() + ROLL_TICKS, ROLL_TICKS);
        ACTIVE_DRAWS.put(session.id(), state);
        broadcast(level, session, state);
        return true;
    }

    public static boolean active(UUID boardId) {
        return ACTIVE_DRAWS.containsKey(boardId);
    }

    public static boolean tick(ServerLevel level, BoardSession session) {
        DrawState state = ACTIVE_DRAWS.get(session.id());
        if (state == null) return false;
        if (level.getGameTime() < state.deadlineTick()) return true;
        if (state.phase() == OpenBoardLotteryDrawPayload.Phase.ROLLING) {
            settle(level, session, state);
        } else {
            finish(level, session);
        }

        return true;
    }

    public static void clear(ServerLevel level, BoardSession session) {
        if (ACTIVE_DRAWS.remove(session.id()) == null) return;
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardLotteryDrawPayload(session.id()));
        }
    }

    private static void settle(ServerLevel level, BoardSession session, DrawState state) {
        List<UUID> winnerSlots = session.mechanics().lotteryWinners(state.finalNumber()).stream()
                .filter(slotId -> session.participant(slotId).isPresent()).toList();
        List<String> winnerNames = winnerSlots.stream().map(session::participant).flatMap(Optional::stream)
                .map(participant -> BoardSessionManager.displayName(level, participant)).toList();
        int awardEach = winnerSlots.isEmpty() ? 0 : state.jackpot() / winnerSlots.size();
        if (winnerSlots.isEmpty()) {
            session.mechanics().increaseLotteryJackpot();
            for (ServerPlayer player : BoardSessionManager.humanPlayers(level, session)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.lottery.no_winner",
                        session.mechanics().lotteryJackpot()).withStyle(ChatFormatting.GRAY), false);
            }
        } else {
            for (UUID slotId : winnerSlots) BoardWorldObjectService.awardCoins(level, session, slotId, awardEach);
            session.mechanics().resetLotteryJackpot();
            for (ServerPlayer player : BoardSessionManager.humanPlayers(level, session)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.lottery.winner",
                        state.finalNumber(), winnerSlots.size(), awardEach).withStyle(ChatFormatting.GOLD), false);
            }
        }

        BoardSessionManager.markChanged(level);
        int displayedJackpot = winnerSlots.isEmpty() ? session.mechanics().lotteryJackpot() : state.jackpot();
        DrawState result = new DrawState(OpenBoardLotteryDrawPayload.Phase.RESULT, state.finalNumber(),
                displayedJackpot, winnerNames, awardEach, level.getGameTime() + RESULT_TICKS, RESULT_TICKS);
        ACTIVE_DRAWS.put(session.id(), result);
        broadcast(level, session, result);
    }

    private static void finish(ServerLevel level, BoardSession session) {
        ACTIVE_DRAWS.remove(session.id());
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CloseBoardLotteryDrawPayload(session.id()));
        }

        BoardSessionManager.resumeAfterLotteryDraw(level, session);
    }

    private static void broadcast(ServerLevel level, BoardSession session, DrawState state) {
        int remaining = (int) Math.max(0L, state.deadlineTick() - level.getGameTime());
        List<OpenBoardLotteryDrawPayload.Entry> entries = session.turnOrder().stream().map(session::participant)
                .flatMap(Optional::stream).map(participant -> new OpenBoardLotteryDrawPayload.Entry(
                        BoardSessionManager.displayName(level, participant),
                        session.mechanics().lotteryNumbers(participant.slotUuid()))).toList();
        OpenBoardLotteryDrawPayload payload = new OpenBoardLotteryDrawPayload(session.id(), state.phase(), state.finalNumber(),
                state.jackpot(), entries, state.winnerNames(), state.awardEach(), remaining, state.durationTicks());
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private record DrawState(OpenBoardLotteryDrawPayload.Phase phase, int finalNumber, int jackpot,
                             List<String> winnerNames, int awardEach, long deadlineTick, int durationTicks) {
        private DrawState {
            winnerNames = List.copyOf(winnerNames);
        }
    }

}