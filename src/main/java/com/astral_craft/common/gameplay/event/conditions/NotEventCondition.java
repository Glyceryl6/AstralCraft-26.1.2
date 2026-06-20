package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NotEventCondition(AstralEventCondition condition) implements AstralEventCondition {

    public static final MapCodec<NotEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventCondition.CODEC.fieldOf("condition").forGetter(NotEventCondition::condition)
    ).apply(instance, NotEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("not").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return this.condition == null || !this.condition.test(context);
    }
}
