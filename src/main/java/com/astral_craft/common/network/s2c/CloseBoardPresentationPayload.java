package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CloseBoardPresentationPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<CloseBoardPresentationPayload> TYPE = new Type<>(AstralCraft.prefix("close_board_presentation"));
    public static final StreamCodec<ByteBuf, CloseBoardPresentationPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, CloseBoardPresentationPayload::boardId, CloseBoardPresentationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}