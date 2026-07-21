package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class BoardEventTargets {

    public static Optional<Target> resolve(AstralEventContext context) {
        if (context == null || !(context.target() instanceof AstralCharacterEntity pawn)
                || !(pawn.level() instanceof ServerLevel level)) return Optional.empty();
        BoardSession session = BoardSessionManager.findByEntity(pawn).orElse(null);
        BoardParticipant participant = session == null ? null : session.participantFor(pawn).orElse(null);
        return participant == null ? Optional.empty() : Optional.of(new Target(level, session, participant, pawn));
    }

    public record Target(ServerLevel level, BoardSession session, BoardParticipant participant,
                         AstralCharacterEntity pawn) {}
}
