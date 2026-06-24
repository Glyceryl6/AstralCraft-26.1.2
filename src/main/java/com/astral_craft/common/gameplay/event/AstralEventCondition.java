package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventConditionTypes;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface AstralEventCondition {

    Codec<AstralEventCondition> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AstralEventCondition, T>> decode(DynamicOps<T> ops, T input) {
            return this.delegate().decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AstralEventCondition input, DynamicOps<T> ops, T prefix) {
            return this.delegate().encode(input, ops, prefix);
        }

        private Codec<AstralEventCondition> delegate() {
            return AstralEventConditionTypes.REGISTRY.byNameCodec()
                    .dispatch(AstralEventCondition::codec, Function.identity());
        }
    };

    String typeId();

    MapCodec<? extends AstralEventCondition> codec();

    boolean test(AstralEventContext context);

}
