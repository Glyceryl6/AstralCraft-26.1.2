package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenBoardDiscardPayload(
        String boardId, String encodedCards, int requiredCount,
        int timeoutTicks, int timeoutDurationTicks,
        Identifier characterId, Identifier skinId) implements CustomPacketPayload {

    public static final Type<OpenBoardDiscardPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_discard"));
    public static final StreamCodec<ByteBuf, OpenBoardDiscardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardDiscardPayload::boardId,
            ByteBufCodecs.STRING_UTF8, OpenBoardDiscardPayload::encodedCards,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::requiredCount,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::timeoutDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardDiscardPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardDiscardPayload::skinId,
            OpenBoardDiscardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}