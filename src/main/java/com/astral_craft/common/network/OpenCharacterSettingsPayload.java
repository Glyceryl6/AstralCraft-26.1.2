package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.charset.StandardCharsets;

public record OpenCharacterSettingsPayload(
        String encodedCharacters, String selectedCharacterId, String selectedSkinId, String activeCharacterId, String activeSkinId, int level, int experience, int friendship,
        String unlockedCharacterIds, String unlockedSkinIds, String encodedProgressEntries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCharacterSettingsPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_character_settings"));
    private static final int MAX_LARGE_STRING_BYTES = 1024 * 1024;

    public static final StreamCodec<ByteBuf, OpenCharacterSettingsPayload> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public OpenCharacterSettingsPayload decode(ByteBuf buffer) {
            String encodedCharacters = readLargeUtf(buffer);
            String selectedCharacterId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String selectedSkinId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String activeCharacterId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String activeSkinId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            int level = ByteBufCodecs.VAR_INT.decode(buffer);
            int experience = ByteBufCodecs.VAR_INT.decode(buffer);
            int friendship = ByteBufCodecs.VAR_INT.decode(buffer);
            String unlockedCharacterIds = readLargeUtf(buffer);
            String unlockedSkinIds = readLargeUtf(buffer);
            String encodedProgressEntries = readLargeUtf(buffer);
            return new OpenCharacterSettingsPayload(encodedCharacters, selectedCharacterId, selectedSkinId, activeCharacterId, activeSkinId,
                    level, experience, friendship, unlockedCharacterIds, unlockedSkinIds, encodedProgressEntries);
        }

        @Override
        public void encode(ByteBuf buffer, OpenCharacterSettingsPayload payload) {
            writeLargeUtf(buffer, payload.encodedCharacters());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.selectedCharacterId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.selectedSkinId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.activeCharacterId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.activeSkinId());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.level());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.experience());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.friendship());
            writeLargeUtf(buffer, payload.unlockedCharacterIds());
            writeLargeUtf(buffer, payload.unlockedSkinIds());
            writeLargeUtf(buffer, payload.encodedProgressEntries());
        }

    };

    private static String readLargeUtf(ByteBuf buffer) {
        int length = ByteBufCodecs.VAR_INT.decode(buffer);
        if (length < 0) {
            throw new DecoderException("Negative OpenCharacterSettingsPayload string length: " + length);
        }

        if (length > MAX_LARGE_STRING_BYTES) {
            throw new DecoderException("OpenCharacterSettingsPayload string too big: " + length + " bytes, max " + MAX_LARGE_STRING_BYTES);
        }

        if (length > buffer.readableBytes()) {
            throw new DecoderException("OpenCharacterSettingsPayload string length " + length + " is larger than readable bytes " + buffer.readableBytes());
        }

        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeLargeUtf(ByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_LARGE_STRING_BYTES) {
            throw new EncoderException("OpenCharacterSettingsPayload string too big: " + bytes.length + " bytes, max " + MAX_LARGE_STRING_BYTES);
        }

        ByteBufCodecs.VAR_INT.encode(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}