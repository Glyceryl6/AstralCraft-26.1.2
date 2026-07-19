package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** A negative hand index means the target deliberately declines to use a counter card. */
public record BoardCounterResponsePayload(UUID boardId, int handIndex) implements CustomPacketPayload {

    public static final Type<BoardCounterResponsePayload> TYPE = new Type<>(AstralCraft.prefix("board_counter_response"));
    public static final StreamCodec<ByteBuf, BoardCounterResponsePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardCounterResponsePayload::boardId,
            ByteBufCodecs.VAR_INT, BoardCounterResponsePayload::handIndex,
            BoardCounterResponsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
