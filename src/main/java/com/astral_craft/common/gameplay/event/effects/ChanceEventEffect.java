package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChanceEventEffect(double chance, AstralEventEffect effect) implements AstralEventEffect {

    public static final MapCodec<ChanceEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(ChanceEventEffect::chance),
            AstralEventEffect.CODEC.fieldOf("effect").forGetter(ChanceEventEffect::effect)
    ).apply(instance, ChanceEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("chance").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        double normalized = Math.clamp(this.chance, 0.0D, 1.0D);
        if (normalized >= 1.0D || context.random().nextDouble() <= normalized) {
            this.effect.apply(context);
        }
    }

}