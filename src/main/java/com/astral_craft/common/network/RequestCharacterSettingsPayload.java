package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestCharacterSettingsPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestCharacterSettingsPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("request_character_settings"));
    public static final StreamCodec<ByteBuf, RequestCharacterSettingsPayload> STREAM_CODEC = StreamCodec.unit(new RequestCharacterSettingsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}