package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BoardLeavePayload(String boardId) implements CustomPacketPayload {

    public static final Type<BoardLeavePayload> TYPE = new Type<>(AstralCraft.prefix("board_leave"));
    public static final StreamCodec<ByteBuf, BoardLeavePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardLeavePayload::boardId, BoardLeavePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}