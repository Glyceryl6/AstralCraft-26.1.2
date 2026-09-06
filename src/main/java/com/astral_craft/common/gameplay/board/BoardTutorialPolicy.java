package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Runtime-only rules for the single-player tutorial, kept out of normal board logic as much as possible. */
public class BoardTutorialPolicy {

    public static final int UNLIMITED_DECISION_TICKS = Integer.MAX_VALUE;
    public static final Identifier ATTACK_ROLL_HINT = AstralCraft.prefix("attack_roll");
    private static final int DEFENSE_BONUS = 3;
    private static final Set<UUID> ACTIVE_BOARDS = new HashSet<>();
    private static final Map<UUID, Map<UUID, Set<Identifier>>> DISMISSED_HINTS = new HashMap<>();

    public static void setEnabled(UUID boardId, boolean enabled) {
        if (boardId == null) return;
        if (enabled) ACTIVE_BOARDS.add(boardId);
        else clear(boardId);
    }

    public static boolean enabled(UUID boardId) {
        return boardId != null && ACTIVE_BOARDS.contains(boardId);
    }

    public static boolean enabled(BoardSession session) {
        return session != null && enabled(session.id());
    }

    public static boolean protectedParticipant(BoardSession session, BoardParticipant participant) {
        return enabled(session) && participant != null && !participant.bot() && !participant.monster();
    }

    public static boolean usesTutorialLoadout(BoardSession session, BoardParticipant participant) {
        return protectedParticipant(session, participant);
    }

    public static AstralPlayerStats applyInitialStats(BoardSession session, BoardParticipant participant, AstralPlayerStats stats) {
        return protectedParticipant(session, participant) && stats != null ? stats.addBaseDefense(DEFENSE_BONUS) : stats;
    }

    public static int decisionDurationTicks(BoardSession session, BoardParticipant participant, int baseTicks) {
        if (protectedParticipant(session, participant)) return UNLIMITED_DECISION_TICKS;
        return participant == null ? Math.max(1, baseTicks) : participant.decisionDurationTicks(baseTicks);
    }

    public static boolean blocksEventImpact(BoardSession session, BoardParticipant participant, BoardEventTargets.Impact impact) {
        if (!protectedParticipant(session, participant)) return false;
        return switch (impact) {
            case HAND_LOSS, STATUS, FORCED_RELOCATION -> true;
            case SAFE, COIN_LOSS, HEALTH_LOSS -> false;
        };
    }

    public static void onHandTransferEvent(ServerLevel level, BoardSession session) {
        if (level == null || !enabled(session)) return;
        for (BoardParticipant participant : session.partyParticipants()) {
            if (!protectedParticipant(session, participant)) continue;
            participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(player ->
                    player.sendSystemMessage(Component.translatable(
                            "message.astral_craft.board.tutorial.hand_transfer_protected"), true));
        }
    }

    public static void dismissHint(UUID boardId, UUID playerId, Identifier hintId) {
        if (!enabled(boardId) || playerId == null || !ATTACK_ROLL_HINT.equals(hintId)) return;
        DISMISSED_HINTS.computeIfAbsent(boardId, ignored -> new HashMap<>())
                .computeIfAbsent(playerId, ignored -> new HashSet<>()).add(hintId);
    }

    public static boolean hintDismissed(BoardSession session, BoardParticipant participant, Identifier hintId) {
        if (!protectedParticipant(session, participant) || hintId == null) return false;
        UUID playerId = participant.controllerUuid().orElse(null);
        if (playerId == null) return false;
        return DISMISSED_HINTS.getOrDefault(session.id(), Map.of())
                .getOrDefault(playerId, Set.of()).contains(hintId);
    }

    public static void clear(UUID boardId) {
        if (boardId == null) return;
        ACTIVE_BOARDS.remove(boardId);
        DISMISSED_HINTS.remove(boardId);
    }
}
