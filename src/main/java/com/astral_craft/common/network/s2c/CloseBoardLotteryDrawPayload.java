package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CloseBoardLotteryDrawPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<CloseBoardLotteryDrawPayload> TYPE = new Type<>(AstralCraft.prefix("close_board_lottery_draw"));
    public static final StreamCodec<ByteBuf, CloseBoardLotteryDrawPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, CloseBoardLotteryDrawPayload::boardId, CloseBoardLotteryDrawPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}