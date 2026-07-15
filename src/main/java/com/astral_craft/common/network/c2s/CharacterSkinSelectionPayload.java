package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CharacterSkinSelectionPayload(Identifier characterId, Identifier skinId)
        implements CustomPacketPayload {

    public static final Type<CharacterSkinSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("character_skin_selection"));
    public static final StreamCodec<ByteBuf, CharacterSkinSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CharacterSkinSelectionPayload::characterId,
            Identifier.STREAM_CODEC, CharacterSkinSelectionPayload::skinId,
            CharacterSkinSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
