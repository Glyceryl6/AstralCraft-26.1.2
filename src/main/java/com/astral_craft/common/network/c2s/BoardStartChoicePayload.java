package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardStartChoicePayload(UUID boardId, boolean stop) implements CustomPacketPayload {

    public static final Type<BoardStartChoicePayload> TYPE = new Type<>(AstralCraft.prefix("board_start_choice"));
    public static final StreamCodec<ByteBuf, BoardStartChoicePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardStartChoicePayload::boardId,
            ByteBufCodecs.BOOL, BoardStartChoicePayload::stop, BoardStartChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}