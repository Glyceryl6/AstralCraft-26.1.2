package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardMode;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BoardModeSelectionPayload(BlockPos origin, BoardMode mode) implements CustomPacketPayload {

    public static final Type<BoardModeSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("board_mode_selection"));
    private static final StreamCodec<ByteBuf, BoardMode> MODE_CODEC = ByteBufCodecs.idMapper(
            index -> BoardMode.values()[Math.clamp(index, 0, BoardMode.values().length - 1)], BoardMode::ordinal);
    public static final StreamCodec<ByteBuf, BoardModeSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, BoardModeSelectionPayload::origin,
            MODE_CODEC, BoardModeSelectionPayload::mode,
            BoardModeSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
