package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.CharacterCodecLines;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenCharacterSettingsPayload(
        List<CharacterDefinition> characters,
        Identifier selectedCharacterId,
        String selectedSkinId,
        Identifier activeCharacterId,
        String activeSkinId,
        int level,
        int experience,
        int friendship,
        List<Identifier> unlockedCharacterIds,
        List<String> unlockedSkinIds,
        List<CharacterProgressView> progressEntries) implements CustomPacketPayload {

    private static final int MAXIMUM_SKIN_KEYS = 4096;
    public static final Type<OpenCharacterSettingsPayload> TYPE = new Type<>(AstralCraft.prefix("open_character_settings"));
    public static final StreamCodec<ByteBuf, OpenCharacterSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            CharacterCodecLines.STREAM_CODEC, OpenCharacterSettingsPayload::characters,
            Identifier.STREAM_CODEC, OpenCharacterSettingsPayload::selectedCharacterId,
            ByteBufCodecs.STRING_UTF8, OpenCharacterSettingsPayload::selectedSkinId,
            Identifier.STREAM_CODEC, OpenCharacterSettingsPayload::activeCharacterId,
            ByteBufCodecs.STRING_UTF8, OpenCharacterSettingsPayload::activeSkinId,
            ByteBufCodecs.VAR_INT, OpenCharacterSettingsPayload::level,
            ByteBufCodecs.VAR_INT, OpenCharacterSettingsPayload::experience,
            ByteBufCodecs.VAR_INT, OpenCharacterSettingsPayload::friendship,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(CharacterCodecLines.MAXIMUM_CHARACTERS)),
            OpenCharacterSettingsPayload::unlockedCharacterIds,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAXIMUM_SKIN_KEYS)),
            OpenCharacterSettingsPayload::unlockedSkinIds,
            CharacterProgressView.STREAM_CODEC.apply(ByteBufCodecs.list(CharacterCodecLines.MAXIMUM_CHARACTERS)),
            OpenCharacterSettingsPayload::progressEntries,
            OpenCharacterSettingsPayload::new);

    public OpenCharacterSettingsPayload {
        characters = List.copyOf(characters == null ? List.of() : characters);
        selectedSkinId = selectedSkinId == null || selectedSkinId.isBlank() ? "default" : selectedSkinId;
        activeSkinId = activeSkinId == null || activeSkinId.isBlank() ? "default" : activeSkinId;
        unlockedCharacterIds = List.copyOf(unlockedCharacterIds == null ? List.of() : unlockedCharacterIds);
        unlockedSkinIds = List.copyOf(unlockedSkinIds == null ? List.of() : unlockedSkinIds);
        progressEntries = List.copyOf(progressEntries == null ? List.of() : progressEntries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record CharacterProgressView(Identifier characterId, CharacterProgressEntry progress) {

        public static final StreamCodec<ByteBuf, CharacterProgressView> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, CharacterProgressView::characterId,
                CharacterProgressEntry.STREAM_CODEC, CharacterProgressView::progress,
                CharacterProgressView::new);
    }

}