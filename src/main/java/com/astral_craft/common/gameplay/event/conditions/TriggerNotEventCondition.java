package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventTriggerCondition;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TriggerNotEventCondition(AstralEventTriggerCondition condition) implements AstralEventTriggerCondition {

    public static final MapCodec<TriggerNotEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventTriggerCondition.CODEC.fieldOf("condition").forGetter(TriggerNotEventCondition::condition)
    ).apply(instance, TriggerNotEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("not").toString();
    }

    @Override
    public MapCodec<? extends AstralEventTriggerCondition> triggerCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return this.condition == null || !this.condition.test(context);
    }
}
