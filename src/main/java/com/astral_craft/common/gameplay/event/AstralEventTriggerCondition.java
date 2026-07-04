package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventConditionTypes;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface AstralEventTriggerCondition extends AstralEventCondition {

    Codec<AstralEventTriggerCondition> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AstralEventTriggerCondition, T>> decode(DynamicOps<T> ops, T input) {
            return this.delegate().decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AstralEventTriggerCondition input, DynamicOps<T> ops, T prefix) {
            return this.delegate().encode(input, ops, prefix);
        }

        private Codec<AstralEventTriggerCondition> delegate() {
            return AstralEventConditionTypes.TRIGGER_REGISTRY.byNameCodec()
                    .dispatch(AstralEventTriggerCondition::triggerCodec, Function.identity());
        }
    };

    MapCodec<? extends AstralEventTriggerCondition> triggerCodec();

}