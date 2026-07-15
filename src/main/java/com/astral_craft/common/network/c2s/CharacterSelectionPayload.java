package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CharacterSelectionPayload(Identifier characterId) implements CustomPacketPayload {

    public static final Type<CharacterSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("character_selection"));
    public static final StreamCodec<ByteBuf, CharacterSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CharacterSelectionPayload::characterId,
            CharacterSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
