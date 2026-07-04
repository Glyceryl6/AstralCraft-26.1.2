package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventTriggerCondition;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record TriggerAnyOfEventCondition(List<AstralEventTriggerCondition> conditions) implements AstralEventTriggerCondition {

    public static final MapCodec<TriggerAnyOfEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventTriggerCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(TriggerAnyOfEventCondition::conditions)
    ).apply(instance, TriggerAnyOfEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("any_of").toString();
    }

    @Override
    public MapCodec<? extends AstralEventTriggerCondition> triggerCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (this.conditions.isEmpty()) return true;
        for (AstralEventTriggerCondition condition : this.conditions) {
            if (condition != null && condition.test(context)) return true;
        }
        return false;
    }
}
