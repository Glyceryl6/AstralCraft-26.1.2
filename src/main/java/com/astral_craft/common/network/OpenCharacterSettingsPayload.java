package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenCharacterSettingsPayload(String encodedCharacters, String selectedCharacterId, String selectedSkinId, int level, int experience, int friendship, String unlockedCharacterIds, String unlockedSkinIds, String encodedProgressEntries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCharacterSettingsPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_character_settings"));

    public static final StreamCodec<ByteBuf, OpenCharacterSettingsPayload> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public OpenCharacterSettingsPayload decode(ByteBuf buffer) {
            String encodedCharacters = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String selectedCharacterId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String selectedSkinId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            int level = ByteBufCodecs.VAR_INT.decode(buffer);
            int experience = ByteBufCodecs.VAR_INT.decode(buffer);
            int friendship = ByteBufCodecs.VAR_INT.decode(buffer);
            String unlockedCharacterIds = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String unlockedSkinIds = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String encodedProgressEntries = ByteBufCodecs.STRING_UTF8.decode(buffer);
            return new OpenCharacterSettingsPayload(encodedCharacters, selectedCharacterId, selectedSkinId, level, experience, friendship, unlockedCharacterIds, unlockedSkinIds, encodedProgressEntries);
        }

        @Override
        public void encode(ByteBuf buffer, OpenCharacterSettingsPayload payload) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.encodedCharacters());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.selectedCharacterId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.selectedSkinId());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.level());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.experience());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.friendship());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.unlockedCharacterIds());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.unlockedSkinIds());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.encodedProgressEntries());
        }

    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}