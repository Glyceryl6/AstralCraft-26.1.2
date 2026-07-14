package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardDiscardPayload(String boardId, String encodedCards, int requiredCount, int timeoutTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardDiscardPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_discard"));
    public static final StreamCodec<ByteBuf, OpenBoardDiscardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardDiscardPayload::boardId,
            ByteBufCodecs.STRING_UTF8, OpenBoardDiscardPayload::encodedCards,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::requiredCount,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::timeoutTicks,
            OpenBoardDiscardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
