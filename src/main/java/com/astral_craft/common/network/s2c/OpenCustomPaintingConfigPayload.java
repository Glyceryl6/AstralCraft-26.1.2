package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CustomPaintingData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenCustomPaintingConfigPayload(int entityId, CustomPaintingData data) implements CustomPacketPayload {

    public static final Type<OpenCustomPaintingConfigPayload> TYPE = new Type<>(AstralCraft.prefix("open_custom_painting_config"));
    public static final StreamCodec<ByteBuf, OpenCustomPaintingConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenCustomPaintingConfigPayload::entityId,
            CustomPaintingData.STREAM_CODEC, OpenCustomPaintingConfigPayload::data,
            OpenCustomPaintingConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}