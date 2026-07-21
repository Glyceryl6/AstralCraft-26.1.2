package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventConditionTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface AstralEventTriggerCondition extends AstralEventCondition {

    Codec<AstralEventTriggerCondition> CODEC = AstralEventConditionTypes.TRIGGER_REGISTRY.byNameCodec()
            .dispatch(AstralEventTriggerCondition::triggerCodec, Function.identity());

    MapCodec<? extends AstralEventTriggerCondition> triggerCodec();

}