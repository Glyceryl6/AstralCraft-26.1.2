package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.type.AstralEventTriggers;
import com.mojang.serialization.MapCodec;

public class EntityHurtPlayerEventCondition implements AstralEventCondition {

    public static final MapCodec<EntityHurtPlayerEventCondition> CODEC = MapCodec.unit(new EntityHurtPlayerEventCondition());

    @Override
    public String typeId() {
        return AstralCraft.prefix("entity_hurt_player").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        return AstralEventTriggers.matches(AstralEventTriggers.ENTITY_HURT_PLAYER, context == null ? null : context.trigger());
    }

}
