package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardDropCoinsEventEffect(int amount) implements AstralEventEffect {

    public static final MapCodec<BoardDropCoinsEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("amount").forGetter(BoardDropCoinsEventEffect::amount)
    ).apply(instance, BoardDropCoinsEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_drop_coins").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> {
            int removed = -BoardWorldObjectService.changeCoins(target.level(), target.session(),
                    target.participant().slotUuid(), -Math.max(0, this.amount));
            if (removed > 0) BoardWorldObjectService.dropCoins(target.level(), target.session(),
                    target.participant().currentNodeKey(), removed);
        });
    }
}
