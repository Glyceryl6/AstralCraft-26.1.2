package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.type.AstralEventTriggers;
import com.mojang.serialization.MapCodec;

public class PlayerKilledEventCondition implements AstralEventCondition {

    public static final MapCodec<PlayerKilledEventCondition> CODEC = MapCodec.unit(new PlayerKilledEventCondition());

    @Override
    public String typeId() {
        return AstralCraft.prefix("player_killed").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return AstralEventTriggers.matches(AstralEventTriggers.PLAYER_KILLED, context == null ? null : context.trigger());
    }

}
