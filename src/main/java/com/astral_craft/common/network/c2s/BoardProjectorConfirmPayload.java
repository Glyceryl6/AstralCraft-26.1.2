package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;

public record BoardProjectorConfirmPayload(BlockPos groundPos, Direction facing, boolean offhand) implements CustomPacketPayload {

    public static final Type<BoardProjectorConfirmPayload> TYPE = new Type<>(AstralCraft.prefix("board_projector_confirm"));
    private static final StreamCodec<ByteBuf, Direction> DIRECTION_STREAM_CODEC = ByteBufCodecs.idMapper(
            Direction::from3DDataValue, Direction::get3DDataValue);
    public static final StreamCodec<ByteBuf, BoardProjectorConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, BoardProjectorConfirmPayload::groundPos,
            DIRECTION_STREAM_CODEC, BoardProjectorConfirmPayload::facing,
            ByteBufCodecs.BOOL, BoardProjectorConfirmPayload::offhand,
            BoardProjectorConfirmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}