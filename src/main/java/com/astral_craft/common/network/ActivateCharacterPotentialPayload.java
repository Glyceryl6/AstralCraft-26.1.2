package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActivateCharacterPotentialPayload(String characterId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivateCharacterPotentialPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("activate_character_potential"));
    public static final StreamCodec<ByteBuf, ActivateCharacterPotentialPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ActivateCharacterPotentialPayload::characterId, ActivateCharacterPotentialPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
