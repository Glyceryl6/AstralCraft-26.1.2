package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record BoardEventContext(ServerLevel level, BoardSession session, BoardParticipant source,
                                AstralEventDefinition definition) {

    public ServerPlayer triggerPlayer() {
        return this.source.controllerUuid().map(this.level.getServer().getPlayerList()::getPlayer).orElse(null);
    }

    public AstralEventContext astralContext(BoardParticipant participant) {
        AstralCharacterEntity pawn = BoardEntityService.entity(this.level, participant);
        Entity target = pawn == null ? this.triggerPlayer() : pawn;
        return new AstralEventContext(this.triggerPlayer(), target, this.level, this.definition,
                target == null ? BlockPos.ZERO : target.blockPosition(),
                null, null, null, 0.0F, false);
    }
}
