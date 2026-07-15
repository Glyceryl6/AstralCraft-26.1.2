package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardStartChoicePayload(
        String boardId,
        int health,
        int maximumHealth,
        int stars,
        int starCoins,
        int nextStarCost,
        int timeoutTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardStartChoicePayload> TYPE = new Type<>(AstralCraft.prefix("open_board_start_choice"));
    public static final StreamCodec<ByteBuf, OpenBoardStartChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardStartChoicePayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::health,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::maximumHealth,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::stars,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::starCoins,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::nextStarCost,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::timeoutTicks,
            OpenBoardStartChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}