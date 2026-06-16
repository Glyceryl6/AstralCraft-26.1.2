package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CharacterSkinSelectionPayload(String characterId, String skinId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CharacterSkinSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("character_skin_selection"));
    public static final StreamCodec<ByteBuf, CharacterSkinSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CharacterSkinSelectionPayload::characterId,
            ByteBufCodecs.STRING_UTF8, CharacterSkinSelectionPayload::skinId,
            CharacterSkinSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}