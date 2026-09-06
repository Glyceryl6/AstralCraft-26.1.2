package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class BoardEventTargets {

    public static Optional<Target> resolve(AstralEventContext context) {
        return resolve(context, Impact.SAFE);
    }

    public static Optional<Target> resolve(AstralEventContext context, Impact impact) {
        if (context == null || !(context.target() instanceof AstralCharacterEntity pawn)
                || !(pawn.level() instanceof ServerLevel level)) return Optional.empty();
        BoardSession session = BoardSessionManager.findByEntity(pawn).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantFor(pawn).orElse(null);
        if (!affected(session, participant, impact)) return Optional.empty();
        return Optional.of(new Target(level, session, participant, pawn));
    }

    public static boolean affected(BoardSession session, BoardParticipant participant, Impact impact) {
        return participant != null && !BoardTutorialPolicy.blocksEventImpact(session, participant, impact);
    }

    public enum Impact {
        SAFE,
        HAND_LOSS,
        COIN_LOSS,
        STATUS,
        HEALTH_LOSS,
        FORCED_RELOCATION
    }

    public record Target(ServerLevel level, BoardSession session, BoardParticipant participant, AstralCharacterEntity pawn) {}

}