package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.MapCodec;

public record BoardStatusEventEffect(BoardBuffInstance buff) implements AstralEventEffect {

    public static final MapCodec<BoardStatusEventEffect> CODEC = BoardBuffInstance.CODEC.fieldOf("buff")
            .xmap(BoardStatusEventEffect::new, BoardStatusEventEffect::buff);

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_status").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> BoardSessionManager.updateParticipant(
                target.level(), target.session(), target.participant().withStats(target.participant().stats().addBuff(this.buff))));
    }

}