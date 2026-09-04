package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenBoardMatchmakingModeSelectionPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<OpenBoardMatchmakingModeSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_matchmaking_mode_selection"));
    public static final StreamCodec<ByteBuf, OpenBoardMatchmakingModeSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardMatchmakingModeSelectionPayload::boardId,
            OpenBoardMatchmakingModeSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
