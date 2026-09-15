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
        double x,
        double y,
        double z,
        float yaw,
        float scale,
        String customName,
        boolean showName,
        String speechText,
        String speechImage,
        boolean faceLookingPlayer,
        boolean customSkinEnabled,
        boolean customSkinPlayer,
        String customSkinSource) implements CustomPacketPayload {

    public static final Type<OpenExhibitionCharacterConfigPayload> TYPE = new Type<>(AstralCraft.prefix("open_exhibition_character_config"));
    public static final StreamCodec<ByteBuf, OpenExhibitionCharacterConfigPayload> STREAM_CODEC = StreamCodec.ofMember(
            OpenExhibitionCharacterConfigPayload::encode, OpenExhibitionCharacterConfigPayload::new);

    private OpenExhibitionCharacterConfigPayload(ByteBuf buffer) {
        this(ByteBufCodecs.VAR_INT.decode(buffer), CharacterCodecLines.STREAM_CODEC.decode(buffer), Identifier.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH).decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH).decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_IMAGE_SOURCE_LENGTH).decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH).decode(buffer));
    }

    private void encode(ByteBuf buffer) {
        ByteBufCodecs.VAR_INT.encode(buffer, this.entityId);
        CharacterCodecLines.STREAM_CODEC.encode(buffer, this.characters);
        Identifier.STREAM_CODEC.encode(buffer, this.characterId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, this.skinId);
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        ByteBufCodecs.FLOAT.encode(buffer, this.yaw);
        ByteBufCodecs.FLOAT.encode(buffer, this.scale);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH).encode(buffer, this.customName);
        ByteBufCodecs.BOOL.encode(buffer, this.showName);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH).encode(buffer, this.speechText);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_IMAGE_SOURCE_LENGTH).encode(buffer, this.speechImage);
        ByteBufCodecs.BOOL.encode(buffer, this.faceLookingPlayer);
        ByteBufCodecs.BOOL.encode(buffer, this.customSkinEnabled);
        ByteBufCodecs.BOOL.encode(buffer, this.customSkinPlayer);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH).encode(buffer, this.customSkinSource);
    }

    public OpenExhibitionCharacterConfigPayload {
        characters = List.copyOf(characters);
        skinId = skinId == null ? "" : skinId;
        customName = customName == null ? "" : customName;
        speechText = speechText == null ? "" : speechText;
        speechImage = speechImage == null ? "" : speechImage;
        customSkinSource = customSkinSource == null ? "" : customSkinSource;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
