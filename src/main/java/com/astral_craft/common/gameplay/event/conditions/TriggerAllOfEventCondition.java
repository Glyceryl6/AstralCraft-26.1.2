package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventTriggerCondition;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record TriggerAllOfEventCondition(List<AstralEventTriggerCondition> conditions) implements AstralEventTriggerCondition {

    public static final MapCodec<TriggerAllOfEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventTriggerCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(TriggerAllOfEventCondition::conditions)
    ).apply(instance, TriggerAllOfEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("all_of").toString();
    }

    @Override
    public MapCodec<? extends AstralEventTriggerCondition> triggerCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        for (AstralEventTriggerCondition condition : this.conditions) {
            if (condition != null && !condition.test(context)) return false;
        }
        return true;
    }
}
