package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.registry.AstralEventEffectTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public interface AstralEventEffect {

    Codec<AstralEventEffect> CODEC = AstralEventEffectTypes.REGISTRY.byNameCodec()
            .dispatch(AstralEventEffect::codec, Function.identity());

    String typeId();

    MapCodec<? extends AstralEventEffect> codec();

    default void apply(ServerPlayer player, AstralEventDefinition definition) {
        if (player != null && definition != null) {
            this.apply(AstralEventContext.player(player, definition));
        }
    }

    void apply(AstralEventContext context);

}