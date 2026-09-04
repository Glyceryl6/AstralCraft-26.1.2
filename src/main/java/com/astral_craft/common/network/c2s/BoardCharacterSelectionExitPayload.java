package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardCharacterSelectionExitPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<BoardCharacterSelectionExitPayload> TYPE = new Type<>(AstralCraft.prefix("board_character_selection_exit"));
    public static final StreamCodec<ByteBuf, BoardCharacterSelectionExitPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardCharacterSelectionExitPayload::boardId,
            BoardCharacterSelectionExitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
