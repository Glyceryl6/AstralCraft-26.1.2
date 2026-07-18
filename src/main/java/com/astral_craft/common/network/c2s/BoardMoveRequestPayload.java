package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardMoveRequestPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<BoardMoveRequestPayload> TYPE = new Type<>(AstralCraft.prefix("board_move_request"));
    public static final StreamCodec<ByteBuf, BoardMoveRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardMoveRequestPayload::boardId, BoardMoveRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}