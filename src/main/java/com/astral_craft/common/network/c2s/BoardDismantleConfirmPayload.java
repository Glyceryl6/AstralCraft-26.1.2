package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardDismantleConfirmPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<BoardDismantleConfirmPayload> TYPE = new Type<>(AstralCraft.prefix("board_dismantle_confirm"));
    public static final StreamCodec<ByteBuf, BoardDismantleConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardDismantleConfirmPayload::boardId,
            BoardDismantleConfirmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
