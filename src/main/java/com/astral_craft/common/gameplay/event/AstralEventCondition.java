package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventConditionTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface AstralEventCondition {

    Codec<AstralEventCondition> CODEC = AstralEventConditionTypes.REGISTRY.byNameCodec()
            .dispatch(AstralEventCondition::codec, Function.identity());

    String typeId();

    MapCodec<? extends AstralEventCondition> codec();

    boolean test(AstralEventContext context);

}