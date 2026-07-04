package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RandomChanceEventCondition(double chance) implements AstralEventGeneralCondition {

    public static final MapCodec<RandomChanceEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(RandomChanceEventCondition::chance)
    ).apply(instance, RandomChanceEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("random_chance").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        double normalized = Math.clamp(this.chance, 0.0D, 1.0D);
        return normalized >= 1.0D || context.random().nextDouble() <= normalized;
    }

}