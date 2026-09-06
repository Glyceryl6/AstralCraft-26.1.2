package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.character.AstralCharacterEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Runtime-only rules for the single-player tutorial, kept out of normal board logic as much as possible. */
public class BoardTutorialPolicy {

    public static final int UNLIMITED_DECISION_TICKS = Integer.MAX_VALUE;
    private static final Set<UUID> ACTIVE_BOARDS = new HashSet<>();

    public static void setEnabled(UUID boardId, boolean enabled) {
        if (boardId == null) return;
        if (enabled) ACTIVE_BOARDS.add(boardId);
        else ACTIVE_BOARDS.remove(boardId);
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

    public static boolean protectedEntity(AstralCharacterEntity entity) {
        if (entity == null) return false;
        BoardSession session = BoardSessionManager.findByEntity(entity).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantFor(entity).orElse(null);
        return protectedParticipant(session, participant);
    }

    public static boolean usesTutorialLoadout(BoardSession session, BoardParticipant participant) {
        return protectedParticipant(session, participant);
    }

    public static int decisionDurationTicks(BoardSession session, BoardParticipant participant, int baseTicks) {
        if (protectedParticipant(session, participant)) return UNLIMITED_DECISION_TICKS;
        return participant == null ? Math.max(1, baseTicks) : participant.decisionDurationTicks(baseTicks);
    }

    public static boolean blocksEventImpact(BoardSession session, BoardParticipant participant, BoardEventTargets.Impact impact) {
        if (!protectedParticipant(session, participant)) return false;
        return switch (impact) {
            case HAND_LOSS, COIN_LOSS, STATUS, HEALTH_LOSS, FORCED_RELOCATION -> true;
            case SAFE -> false;
        };
    }

    public static void clear(UUID boardId) {
        if (boardId != null) ACTIVE_BOARDS.remove(boardId);
    }

}