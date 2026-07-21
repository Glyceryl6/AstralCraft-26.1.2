package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardMechanicsState;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardTrapEventEffect(BoardMechanicsState.BoardTrapType trapType) implements AstralEventEffect {

    public static final MapCodec<BoardTrapEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BoardMechanicsState.BoardTrapType.CODEC.fieldOf("trap_type").forGetter(BoardTrapEventEffect::trapType)
    ).apply(instance, BoardTrapEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_trap").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> BoardWorldObjectService.placeTrap(target.level(),
                target.session(), this.trapType, target.participant().slotUuid(), target.participant().currentNodeKey()));
    }
}
