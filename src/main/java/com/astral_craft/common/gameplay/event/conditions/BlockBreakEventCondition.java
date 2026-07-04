package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.type.AstralEventTriggers;
import com.mojang.serialization.MapCodec;

public class BlockBreakEventCondition implements AstralEventCondition {

    public static final MapCodec<BlockBreakEventCondition> CODEC = MapCodec.unit(new BlockBreakEventCondition());

    @Override
    public String typeId() {
        return AstralCraft.prefix("block_break").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return AstralEventTriggers.matches(AstralEventTriggers.BLOCK_BREAK, context == null ? null : context.trigger());
    }

}
