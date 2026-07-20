package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardLotteryNumberPayload(UUID boardId, int number) implements CustomPacketPayload {

    public static final Type<BoardLotteryNumberPayload> TYPE = new Type<>(AstralCraft.prefix("board_lottery_number"));
    public static final StreamCodec<ByteBuf, BoardLotteryNumberPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardLotteryNumberPayload::boardId,
            ByteBufCodecs.VAR_INT, BoardLotteryNumberPayload::number, BoardLotteryNumberPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}