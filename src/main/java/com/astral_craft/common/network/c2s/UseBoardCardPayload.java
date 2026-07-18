package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record UseBoardCardPayload(UUID boardId, int cardIndex) implements CustomPacketPayload {

    public static final Type<UseBoardCardPayload> TYPE = new Type<>(AstralCraft.prefix("use_board_card"));
    public static final StreamCodec<ByteBuf, UseBoardCardPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, UseBoardCardPayload::boardId,
            ByteBufCodecs.VAR_INT, UseBoardCardPayload::cardIndex, UseBoardCardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}