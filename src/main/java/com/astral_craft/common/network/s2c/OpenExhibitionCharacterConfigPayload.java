package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterCodecLines;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenExhibitionCharacterConfigPayload(
        int entityId,
        List<CharacterDefinition> characters,
        Identifier characterId,
        String skinId,
        float yaw,
        float scale,
        String customName,
        boolean showName,
        String speechText,
        boolean customSkinEnabled,
        boolean customSkinPlayer,
        String customSkinSource) implements CustomPacketPayload {

    public static final Type<OpenExhibitionCharacterConfigPayload> TYPE = new Type<>(AstralCraft.prefix("open_exhibition_character_config"));
    public static final StreamCodec<ByteBuf, OpenExhibitionCharacterConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenExhibitionCharacterConfigPayload::entityId,
            CharacterCodecLines.STREAM_CODEC, OpenExhibitionCharacterConfigPayload::characters,
            Identifier.STREAM_CODEC, OpenExhibitionCharacterConfigPayload::characterId,
            ByteBufCodecs.STRING_UTF8, OpenExhibitionCharacterConfigPayload::skinId,
            ByteBufCodecs.FLOAT, OpenExhibitionCharacterConfigPayload::yaw,
            ByteBufCodecs.FLOAT, OpenExhibitionCharacterConfigPayload::scale,
            ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH), OpenExhibitionCharacterConfigPayload::customName,
            ByteBufCodecs.BOOL, OpenExhibitionCharacterConfigPayload::showName,
            ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH), OpenExhibitionCharacterConfigPayload::speechText,
            ByteBufCodecs.BOOL, OpenExhibitionCharacterConfigPayload::customSkinEnabled,
            ByteBufCodecs.BOOL, OpenExhibitionCharacterConfigPayload::customSkinPlayer,
            ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH), OpenExhibitionCharacterConfigPayload::customSkinSource,
            OpenExhibitionCharacterConfigPayload::new);

    public OpenExhibitionCharacterConfigPayload {
        characters = List.copyOf(characters);
        skinId = skinId == null ? "" : skinId;
        customName = customName == null ? "" : customName;
        speechText = speechText == null ? "" : speechText;
        customSkinSource = customSkinSource == null ? "" : customSkinSource;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
