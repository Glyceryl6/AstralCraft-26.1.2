package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
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
        boolean showName,
        String speechText) implements CustomPacketPayload {

    public static final Type<OpenExhibitionCharacterConfigPayload> TYPE = new Type<>(AstralCraft.prefix("open_exhibition_character_config"));
    public static final StreamCodec<ByteBuf, OpenExhibitionCharacterConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenExhibitionCharacterConfigPayload::entityId,
            CharacterCodecLines.STREAM_CODEC, OpenExhibitionCharacterConfigPayload::characters,
            Identifier.STREAM_CODEC, OpenExhibitionCharacterConfigPayload::characterId,
            ByteBufCodecs.STRING_UTF8, OpenExhibitionCharacterConfigPayload::skinId,
            ByteBufCodecs.FLOAT, OpenExhibitionCharacterConfigPayload::yaw,
            ByteBufCodecs.FLOAT, OpenExhibitionCharacterConfigPayload::scale,
            ByteBufCodecs.BOOL, OpenExhibitionCharacterConfigPayload::showName,
            ByteBufCodecs.STRING_UTF8, OpenExhibitionCharacterConfigPayload::speechText,
            OpenExhibitionCharacterConfigPayload::new);

    public OpenExhibitionCharacterConfigPayload {
        characters = List.copyOf(characters);
        skinId = skinId == null ? "" : skinId;
        speechText = speechText == null ? "" : speechText;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
