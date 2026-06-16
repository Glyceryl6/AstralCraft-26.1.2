package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CharacterSelectionPayload(String characterId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CharacterSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("character_selection"));
    public static final StreamCodec<ByteBuf, CharacterSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CharacterSelectionPayload::characterId, CharacterSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}