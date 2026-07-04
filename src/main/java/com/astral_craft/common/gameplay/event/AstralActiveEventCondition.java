package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventConditionTypes;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface AstralActiveEventCondition extends AstralEventCondition {

    Codec<AstralActiveEventCondition> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AstralActiveEventCondition, T>> decode(DynamicOps<T> ops, T input) {
            return this.delegate().decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AstralActiveEventCondition input, DynamicOps<T> ops, T prefix) {
            return this.delegate().encode(input, ops, prefix);
        }

        private Codec<AstralActiveEventCondition> delegate() {
            return AstralEventConditionTypes.ACTIVE_REGISTRY.byNameCodec()
                    .dispatch(AstralActiveEventCondition::activeCodec, Function.identity());
        }
    };

    MapCodec<? extends AstralActiveEventCondition> activeCodec();

}
