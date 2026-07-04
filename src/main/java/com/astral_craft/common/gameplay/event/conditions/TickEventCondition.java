package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.type.AstralEventTriggers;
import com.mojang.serialization.MapCodec;

public class TickEventCondition implements AstralEventCondition {

    public static final MapCodec<TickEventCondition> CODEC = MapCodec.unit(new TickEventCondition());

    @Override
    public String typeId() {
        return AstralCraft.prefix("tick").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return AstralEventTriggers.matches(AstralEventTriggers.TICK, context == null ? null : context.trigger());
    }

}
