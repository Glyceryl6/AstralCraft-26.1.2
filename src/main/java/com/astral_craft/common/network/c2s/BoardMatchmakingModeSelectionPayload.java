package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardMatchmakingMode;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardMatchmakingModeSelectionPayload(UUID boardId, BoardMatchmakingMode mode) implements CustomPacketPayload {

    public static final Type<BoardMatchmakingModeSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("board_matchmaking_mode_selection"));
    private static final StreamCodec<ByteBuf, BoardMatchmakingMode> MODE_CODEC = ByteBufCodecs.idMapper(
            index -> BoardMatchmakingMode.values()[Math.clamp(index, 0, BoardMatchmakingMode.values().length - 1)], BoardMatchmakingMode::ordinal);
    public static final StreamCodec<ByteBuf, BoardMatchmakingModeSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardMatchmakingModeSelectionPayload::boardId,
            MODE_CODEC, BoardMatchmakingModeSelectionPayload::mode,
            BoardMatchmakingModeSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
