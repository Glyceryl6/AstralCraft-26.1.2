package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CustomPaintingData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CustomPaintingConfigPayload(int entityId, CustomPaintingData data) implements CustomPacketPayload {

    public static final Type<CustomPaintingConfigPayload> TYPE = new Type<>(AstralCraft.prefix("custom_painting_config"));
    public static final StreamCodec<ByteBuf, CustomPaintingConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CustomPaintingConfigPayload::entityId,
            CustomPaintingData.STREAM_CODEC, CustomPaintingConfigPayload::data,
            CustomPaintingConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}