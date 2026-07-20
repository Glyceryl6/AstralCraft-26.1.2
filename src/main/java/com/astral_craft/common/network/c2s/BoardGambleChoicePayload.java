package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardGambleChoicePayload(UUID boardId, boolean odd) implements CustomPacketPayload {

    public static final Type<BoardGambleChoicePayload> TYPE = new Type<>(AstralCraft.prefix("board_gamble_choice"));
    public static final StreamCodec<ByteBuf, BoardGambleChoicePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardGambleChoicePayload::boardId,
            ByteBufCodecs.BOOL, BoardGambleChoicePayload::odd, BoardGambleChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}