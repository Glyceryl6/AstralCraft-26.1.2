package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-side card reveal payload.
 *
 * <p>{@code animation} is sent as a string for packet compatibility, but it is interpreted as
 * a namespaced Identifier on the client. Legacy unnamespaced ids such as {@code flip} still work.</p>
 */
public record CardRevealPayload(
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

    public static final String ANIMATION_FLIP = AstralCraft.prefix("flip").toString();
    public static final String ANIMATION_APPROACH = AstralCraft.prefix("approach").toString();

    public static final CustomPacketPayload.Type<CardRevealPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_reveal"));

    public static final StreamCodec<ByteBuf, CardRevealPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::cardId,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::itemId,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::cardType,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::titleKey,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::bodyKey,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::largeFrontTexture,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::largeBackTexture,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::animation,
            ByteBufCodecs.VAR_INT,
            CardRevealPayload::durationTicks,
            CardRevealPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}