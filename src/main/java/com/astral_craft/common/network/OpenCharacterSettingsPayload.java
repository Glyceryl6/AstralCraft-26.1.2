package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenCharacterSettingsPayload(String encodedCharacters, String selectedCharacterId, String selectedSkinId, int level, int experience, int friendship) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCharacterSettingsPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_character_settings"));

    public static final StreamCodec<ByteBuf, OpenCharacterSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenCharacterSettingsPayload::encodedCharacters,
            ByteBufCodecs.STRING_UTF8, OpenCharacterSettingsPayload::selectedCharacterId,
            ByteBufCodecs.STRING_UTF8, OpenCharacterSettingsPayload::selectedSkinId,
            ByteBufCodecs.VAR_INT, OpenCharacterSettingsPayload::level,
            ByteBufCodecs.VAR_INT, OpenCharacterSettingsPayload::experience,
            ByteBufCodecs.VAR_INT, OpenCharacterSettingsPayload::friendship,
            OpenCharacterSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}