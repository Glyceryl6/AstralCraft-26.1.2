package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.MapCodec;

public record NoopEventCondition() implements AstralEventCondition {

    public static final MapCodec<NoopEventCondition> CODEC = MapCodec.unit(new NoopEventCondition());

    @Override
    public String typeId() {
        return AstralCraft.prefix("noop").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return true;
    }

}