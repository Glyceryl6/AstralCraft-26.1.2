package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RepeatEventEffect(int times, double chance, List<AstralEventEffect> effects) implements AstralEventEffect {

    public static final MapCodec<RepeatEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("times", 1).forGetter(RepeatEventEffect::times),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(RepeatEventEffect::chance),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(RepeatEventEffect::effects)
    ).apply(instance, RepeatEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("repeat").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        int safeTimes = Math.clamp(this.times, 0, 256);
        double safeChance = Math.clamp(this.chance, 0.0D, 1.0D);
        for (int i = 0; i < safeTimes; i++) {
            if (safeChance < 1.0D && context.random().nextDouble() > safeChance) {
                continue;
            }

            for (AstralEventEffect effect : this.effects) {
                if (effect != null) {
                    effect.apply(context);
                }
            }
        }
    }

}