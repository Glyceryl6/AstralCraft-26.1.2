package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventEffectTypes;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public interface AstralEventEffect {

    Codec<AstralEventEffect> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AstralEventEffect, T>> decode(DynamicOps<T> ops, T input) {
            return this.delegate().decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AstralEventEffect input, DynamicOps<T> ops, T prefix) {
            return this.delegate().encode(input, ops, prefix);
        }

        private Codec<AstralEventEffect> delegate() {
            return AstralEventEffectTypes.REGISTRY.byNameCodec()
                    .dispatch(AstralEventEffect::codec, Function.identity());
        }
    };

    String typeId();

    MapCodec<? extends AstralEventEffect> codec();

    default void apply(ServerPlayer player, AstralEventDefinition definition) {
        if (player != null && definition != null) {
            this.apply(AstralEventContext.player(player, definition));
        }
    }

    void apply(AstralEventContext context);

}
