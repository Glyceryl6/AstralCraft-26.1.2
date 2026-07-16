package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenBoardTurnPayload(
        String boardId, int characterEntityId, String encodedCards,
        int cardPlaysUsed, int maxCardPlays, int skillCooldownTurns,
        int decisionTicks, int decisionDurationTicks,
        Identifier characterId, Identifier skinId, boolean currentTurn) implements CustomPacketPayload {

    public static final Type<OpenBoardTurnPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_turn"));
    public static final StreamCodec<ByteBuf, OpenBoardTurnPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardTurnPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::characterEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardTurnPayload::encodedCards,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::cardPlaysUsed,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::maxCardPlays,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::skillCooldownTurns,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::decisionTicks,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::decisionDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardTurnPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardTurnPayload::skinId,
            ByteBufCodecs.BOOL, OpenBoardTurnPayload::currentTurn,
            OpenBoardTurnPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}