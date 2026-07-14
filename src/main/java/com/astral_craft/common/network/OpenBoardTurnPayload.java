package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardTurnPayload(
        String boardId, int characterEntityId, String encodedCards,
        int cardPlaysUsed, int maxCardPlays, int skillCooldownTurns,
        int decisionTicks, boolean currentTurn) implements CustomPacketPayload {

    public static final Type<OpenBoardTurnPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_turn"));
    public static final StreamCodec<ByteBuf, OpenBoardTurnPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardTurnPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::characterEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardTurnPayload::encodedCards,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::cardPlaysUsed,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::maxCardPlays,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::skillCooldownTurns,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::decisionTicks,
            ByteBufCodecs.BOOL, OpenBoardTurnPayload::currentTurn,
            OpenBoardTurnPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
