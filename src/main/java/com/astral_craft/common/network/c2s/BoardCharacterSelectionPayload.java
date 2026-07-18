package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record BoardCharacterSelectionPayload(UUID boardId, Identifier characterId, Identifier skinId, boolean confirmed) implements CustomPacketPayload {

    public static final Type<BoardCharacterSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("board_character_selection"));
    public static final StreamCodec<ByteBuf, BoardCharacterSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardCharacterSelectionPayload::boardId,
            Identifier.STREAM_CODEC, BoardCharacterSelectionPayload::characterId,
            Identifier.STREAM_CODEC, BoardCharacterSelectionPayload::skinId,
            ByteBufCodecs.BOOL, BoardCharacterSelectionPayload::confirmed,
            BoardCharacterSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}