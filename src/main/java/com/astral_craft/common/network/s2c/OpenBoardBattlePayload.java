package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardBattlePayload(
        String boardId, int attackerEntityId, int defenderEntityId,
        String attackerName, String defenderName, String encodedCards,
        String role, int decisionTicks, int maximumCost, boolean resolved, String resultText)
        implements CustomPacketPayload {

    public static final Type<OpenBoardBattlePayload> TYPE = new Type<>(AstralCraft.prefix("open_board_battle"));
    public static final StreamCodec<ByteBuf, OpenBoardBattlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::attackerEntityId,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::defenderEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::attackerName,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::defenderName,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::encodedCards,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::role,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::decisionTicks,
            ByteBufCodecs.VAR_INT, OpenBoardBattlePayload::maximumCost,
            ByteBufCodecs.BOOL, OpenBoardBattlePayload::resolved,
            ByteBufCodecs.STRING_UTF8, OpenBoardBattlePayload::resultText,
            OpenBoardBattlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
