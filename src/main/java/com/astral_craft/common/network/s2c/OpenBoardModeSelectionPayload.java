package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardModeSelectionPayload(BlockPos origin) implements CustomPacketPayload {

    public static final Type<OpenBoardModeSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_mode_selection"));
    public static final StreamCodec<ByteBuf, OpenBoardModeSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, OpenBoardModeSelectionPayload::origin, OpenBoardModeSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}