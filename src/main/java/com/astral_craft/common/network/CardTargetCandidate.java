package com.astral_craft.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CardTargetCandidate(int entityId, Component name, int distance) {

    public static final StreamCodec<RegistryFriendlyByteBuf, CardTargetCandidate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CardTargetCandidate::entityId,
            ComponentSerialization.TRUSTED_STREAM_CODEC, CardTargetCandidate::name,
            ByteBufCodecs.VAR_INT, CardTargetCandidate::distance, CardTargetCandidate::new);

}