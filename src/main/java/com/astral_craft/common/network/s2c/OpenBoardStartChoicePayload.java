package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record OpenBoardStartChoicePayload(
        UUID boardId,
        int health,
        int maximumHealth,
        int stars,
        int starCoins,
        int nextStarCost,
        int timeoutTicks,
        int timeoutDurationTicks,
        boolean checkpoint,
        Identifier characterId,
        Identifier skinId) implements CustomPacketPayload {

    public static final Type<OpenBoardStartChoicePayload> TYPE = new Type<>(AstralCraft.prefix("open_board_start_choice"));
    public static final StreamCodec<ByteBuf, OpenBoardStartChoicePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardStartChoicePayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::health,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::maximumHealth,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::stars,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::starCoins,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::nextStarCost,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardStartChoicePayload::timeoutDurationTicks,
            ByteBufCodecs.BOOL, OpenBoardStartChoicePayload::checkpoint,
            Identifier.STREAM_CODEC, OpenBoardStartChoicePayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardStartChoicePayload::skinId,
            OpenBoardStartChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}