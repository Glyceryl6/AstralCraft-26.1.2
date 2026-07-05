package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * World-space card reveal payload.
 *
 * <p>This mirrors {@link CardRevealPayload}, but also carries the source entity id so the client can render the
 * same card reveal above that entity instead of forcing another player's full-screen overlay.</p>
 */
public record CardRevealEntityPayload(
        int entityId,
        String cardId,
        String itemId,
        String cardType,
        String titleKey,
        String bodyKey,
        String largeFrontTexture,
        String largeBackTexture,
        String animation,
        int durationTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardRevealEntityPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_reveal_entity"));

    public static final StreamCodec<ByteBuf, CardRevealEntityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CardRevealEntityPayload::entityId,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::cardId,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::itemId,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::cardType,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::titleKey,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::bodyKey,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::largeFrontTexture,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::largeBackTexture,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::animation,
            ByteBufCodecs.VAR_INT,
            CardRevealEntityPayload::durationTicks,
            CardRevealEntityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}