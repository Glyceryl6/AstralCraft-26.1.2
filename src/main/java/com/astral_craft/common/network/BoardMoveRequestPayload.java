package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BoardMoveRequestPayload(String boardId) implements CustomPacketPayload {

    public static final Type<BoardMoveRequestPayload> TYPE = new Type<>(AstralCraft.prefix("board_move_request"));
    public static final StreamCodec<ByteBuf, BoardMoveRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardMoveRequestPayload::boardId, BoardMoveRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
