package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ExhibitionCharacterConfigPayload(
        int entityId,
        Identifier characterId,
        String skinId,
        float yaw,
        float scale,
        String customName,
        boolean showName,
        String speechText,
        boolean customSkinEnabled,
        boolean customSkinPlayer,
        String customSkinSource,
        boolean remove) implements CustomPacketPayload {

    public static final Type<ExhibitionCharacterConfigPayload> TYPE = new Type<>(AstralCraft.prefix("exhibition_character_config"));
    public static final StreamCodec<ByteBuf, ExhibitionCharacterConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExhibitionCharacterConfigPayload::entityId,
            Identifier.STREAM_CODEC, ExhibitionCharacterConfigPayload::characterId,
            ByteBufCodecs.STRING_UTF8, ExhibitionCharacterConfigPayload::skinId,
            ByteBufCodecs.FLOAT, ExhibitionCharacterConfigPayload::yaw,
            ByteBufCodecs.FLOAT, ExhibitionCharacterConfigPayload::scale,
            ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH), ExhibitionCharacterConfigPayload::customName,
            ByteBufCodecs.BOOL, ExhibitionCharacterConfigPayload::showName,
            ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH), ExhibitionCharacterConfigPayload::speechText,
            ByteBufCodecs.BOOL, ExhibitionCharacterConfigPayload::customSkinEnabled,
            ByteBufCodecs.BOOL, ExhibitionCharacterConfigPayload::customSkinPlayer,
            ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH), ExhibitionCharacterConfigPayload::customSkinSource,
            ByteBufCodecs.BOOL, ExhibitionCharacterConfigPayload::remove,
            ExhibitionCharacterConfigPayload::new);

    public ExhibitionCharacterConfigPayload {
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
