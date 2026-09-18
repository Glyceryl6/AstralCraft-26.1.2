package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardMatchmakingCancelPayload(UUID boardId, boolean returnToSelection) implements CustomPacketPayload {

    public static final Type<BoardMatchmakingCancelPayload> TYPE = new Type<>(AstralCraft.prefix("board_matchmaking_cancel"));
    public static final StreamCodec<ByteBuf, BoardMatchmakingCancelPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardMatchmakingCancelPayload::boardId,
            ByteBufCodecs.BOOL, BoardMatchmakingCancelPayload::returnToSelection, BoardMatchmakingCancelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}