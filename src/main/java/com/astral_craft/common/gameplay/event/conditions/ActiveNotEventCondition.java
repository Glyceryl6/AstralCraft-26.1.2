package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralActiveEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ActiveNotEventCondition(AstralActiveEventCondition condition) implements AstralActiveEventCondition {

    public static final MapCodec<ActiveNotEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralActiveEventCondition.CODEC.fieldOf("condition").forGetter(ActiveNotEventCondition::condition)
    ).apply(instance, ActiveNotEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("not").toString();
    }

    @Override
    public MapCodec<? extends AstralActiveEventCondition> activeCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return this.condition == null || !this.condition.test(context);
    }
}
