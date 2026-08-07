package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardScaleCoinsEventEffect(float multiplier) implements AstralEventEffect {

    public static final MapCodec<BoardScaleCoinsEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("multiplier").forGetter(BoardScaleCoinsEventEffect::multiplier)
    ).apply(instance, BoardScaleCoinsEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_scale_coins").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> {
            BoardParticipant participant = target.participant();
            int current = participant.stats().starCoins();
            int result = Math.max(0, Math.round(current * Math.max(0.0F, this.multiplier)));
            BoardSessionManager.updateParticipant(target.level(), target.session(),
                    participant.withStats(participant.stats().addCoins(result - current)));
        });
    }
}
