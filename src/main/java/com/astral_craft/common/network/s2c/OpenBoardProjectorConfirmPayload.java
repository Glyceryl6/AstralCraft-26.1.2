package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardProjectorConfirmPayload(BlockPos groundPos, Direction facing, boolean offhand, int width, int depth, int panelCount) implements CustomPacketPayload {

    public static final Type<OpenBoardProjectorConfirmPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_projector_confirm"));
    private static final StreamCodec<ByteBuf, Direction> DIRECTION_STREAM_CODEC = ByteBufCodecs.idMapper(
            Direction::from3DDataValue, Direction::get3DDataValue);
    public static final StreamCodec<ByteBuf, OpenBoardProjectorConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, OpenBoardProjectorConfirmPayload::groundPos,
            DIRECTION_STREAM_CODEC, OpenBoardProjectorConfirmPayload::facing,
            ByteBufCodecs.BOOL, OpenBoardProjectorConfirmPayload::offhand,
            ByteBufCodecs.VAR_INT, OpenBoardProjectorConfirmPayload::width,
            ByteBufCodecs.VAR_INT, OpenBoardProjectorConfirmPayload::depth,
            ByteBufCodecs.VAR_INT, OpenBoardProjectorConfirmPayload::panelCount,
            OpenBoardProjectorConfirmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}