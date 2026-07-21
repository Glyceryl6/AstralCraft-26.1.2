package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CloseBoardPlatformTargetPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<CloseBoardPlatformTargetPayload> TYPE = new Type<>(AstralCraft.prefix("close_board_platform_target"));
    public static final StreamCodec<ByteBuf, CloseBoardPlatformTargetPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, CloseBoardPlatformTargetPayload::boardId,
            CloseBoardPlatformTargetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
