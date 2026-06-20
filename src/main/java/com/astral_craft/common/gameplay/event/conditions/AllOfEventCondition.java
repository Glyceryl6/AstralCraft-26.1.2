package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record AllOfEventCondition(List<AstralEventCondition> conditions) implements AstralEventCondition {

    public static final MapCodec<AllOfEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(AllOfEventCondition::conditions)
    ).apply(instance, AllOfEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("all_of").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        for (AstralEventCondition condition : this.conditions) {
            if (condition != null && !condition.test(context)) return false;
        }
        return true;
    }
}
