package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
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
        boolean showName,
        String speechText,
        boolean remove) implements CustomPacketPayload {

    public static final Type<ExhibitionCharacterConfigPayload> TYPE = new Type<>(AstralCraft.prefix("exhibition_character_config"));
    public static final StreamCodec<ByteBuf, ExhibitionCharacterConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExhibitionCharacterConfigPayload::entityId,
            Identifier.STREAM_CODEC, ExhibitionCharacterConfigPayload::characterId,
            ByteBufCodecs.STRING_UTF8, ExhibitionCharacterConfigPayload::skinId,
            ByteBufCodecs.FLOAT, ExhibitionCharacterConfigPayload::yaw,
            ByteBufCodecs.FLOAT, ExhibitionCharacterConfigPayload::scale,
            ByteBufCodecs.BOOL, ExhibitionCharacterConfigPayload::showName,
            ByteBufCodecs.STRING_UTF8, ExhibitionCharacterConfigPayload::speechText,
            ByteBufCodecs.BOOL, ExhibitionCharacterConfigPayload::remove,
            ExhibitionCharacterConfigPayload::new);

    public ExhibitionCharacterConfigPayload {
        skinId = skinId == null ? "" : skinId;
        speechText = speechText == null ? "" : speechText;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
