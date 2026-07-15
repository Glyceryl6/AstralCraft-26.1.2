package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record BoardDiscardPayload(String boardId, List<Integer> cardIndexes) implements CustomPacketPayload {

    public static final Type<BoardDiscardPayload> TYPE = new Type<>(AstralCraft.prefix("board_discard"));
    public static final StreamCodec<ByteBuf, BoardDiscardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardDiscardPayload::boardId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), BoardDiscardPayload::cardIndexes,
            BoardDiscardPayload::new);

    public BoardDiscardPayload {
        cardIndexes = List.copyOf(cardIndexes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
