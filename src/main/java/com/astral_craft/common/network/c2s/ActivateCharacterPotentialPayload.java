package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActivateCharacterPotentialPayload(Identifier characterId) implements CustomPacketPayload {

    public static final Type<ActivateCharacterPotentialPayload> TYPE = new Type<>(AstralCraft.prefix("activate_character_potential"));
    public static final StreamCodec<ByteBuf, ActivateCharacterPotentialPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ActivateCharacterPotentialPayload::characterId,
            ActivateCharacterPotentialPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
