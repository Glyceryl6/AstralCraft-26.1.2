package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralActiveEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ActiveAnyOfEventCondition(List<AstralActiveEventCondition> conditions) implements AstralActiveEventCondition {

    public static final MapCodec<ActiveAnyOfEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralActiveEventCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(ActiveAnyOfEventCondition::conditions)
    ).apply(instance, ActiveAnyOfEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("any_of").toString();
    }

    @Override
    public MapCodec<? extends AstralActiveEventCondition> activeCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (this.conditions.isEmpty()) return true;
        for (AstralActiveEventCondition condition : this.conditions) {
            if (condition != null && condition.test(context)) return true;
        }
        return false;
    }
}
