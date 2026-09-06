package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record BoardTutorialHintDismissPayload(UUID boardId, Identifier hintId) implements CustomPacketPayload {

    public static final Type<BoardTutorialHintDismissPayload> TYPE = new Type<>(AstralCraft.prefix("board_tutorial_hint_dismiss"));
    public static final StreamCodec<ByteBuf, BoardTutorialHintDismissPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardTutorialHintDismissPayload::boardId,
            Identifier.STREAM_CODEC, BoardTutorialHintDismissPayload::hintId,
            BoardTutorialHintDismissPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
