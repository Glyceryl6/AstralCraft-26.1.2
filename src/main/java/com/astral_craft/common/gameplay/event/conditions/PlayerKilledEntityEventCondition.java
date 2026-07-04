package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.type.AstralEventTriggers;
import com.mojang.serialization.MapCodec;

public class PlayerKilledEntityEventCondition implements AstralEventCondition {

    public static final MapCodec<PlayerKilledEntityEventCondition> CODEC = MapCodec.unit(new PlayerKilledEntityEventCondition());

    @Override
    public String typeId() {
        return AstralCraft.prefix("player_killed_entity").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return AstralEventTriggers.matches(AstralEventTriggers.PLAYER_KILLED_ENTITY, context == null ? null : context.trigger());
    }

}
