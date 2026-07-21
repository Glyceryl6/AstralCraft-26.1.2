package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventConditionTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface AstralActiveEventCondition extends AstralEventCondition {

    Codec<AstralActiveEventCondition> CODEC = Codec.lazyInitialized(() -> AstralEventConditionTypes.ACTIVE_REGISTRY.byNameCodec()
            .dispatch(AstralActiveEventCondition::activeCodec, Function.identity()));

    MapCodec<? extends AstralActiveEventCondition> activeCodec();

}