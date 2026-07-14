package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UseBoardCardPayload(String boardId, int cardIndex) implements CustomPacketPayload {

    public static final Type<UseBoardCardPayload> TYPE = new Type<>(AstralCraft.prefix("use_board_card"));
    public static final StreamCodec<ByteBuf, UseBoardCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UseBoardCardPayload::boardId,
            ByteBufCodecs.VAR_INT, UseBoardCardPayload::cardIndex,
            UseBoardCardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
