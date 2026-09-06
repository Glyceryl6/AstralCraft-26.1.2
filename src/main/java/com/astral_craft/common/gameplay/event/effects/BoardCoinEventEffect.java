package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardCoinEventEffect(int amount) implements AstralEventEffect {

    public static final MapCodec<BoardCoinEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("amount").forGetter(BoardCoinEventEffect::amount)
    ).apply(instance, BoardCoinEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_coins").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context, this.amount < 0
                ? BoardEventTargets.Impact.COIN_LOSS : BoardEventTargets.Impact.SAFE).ifPresent(target -> BoardWorldObjectService.changeCoins(
                target.level(), target.session(), target.participant().slotUuid(), this.amount));
    }
}
