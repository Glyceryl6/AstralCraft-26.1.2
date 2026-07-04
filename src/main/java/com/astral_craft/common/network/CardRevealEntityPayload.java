package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * World-space card reveal payload.
 *
 * <p>This mirrors {@link CardRevealPayload}, but also carries the source entity identity so the client can render the
 * same card reveal above that entity instead of forcing another player's full-screen overlay.</p>
 */
public record CardRevealEntityPayload(
        int entityId,
        String entityUuid,
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

    public static final StreamCodec<ByteBuf, CardRevealEntityPayload> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public CardRevealEntityPayload decode(ByteBuf buffer) {
            int entityId = ByteBufCodecs.VAR_INT.decode(buffer);
            String entityUuid = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String cardId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String itemId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String cardType = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String titleKey = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String bodyKey = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String largeFrontTexture = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String largeBackTexture = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String animation = ByteBufCodecs.STRING_UTF8.decode(buffer);
            int durationTicks = ByteBufCodecs.VAR_INT.decode(buffer);
            return new CardRevealEntityPayload(entityId, entityUuid, cardId, itemId, cardType, titleKey, bodyKey,
                    largeFrontTexture, largeBackTexture, animation, durationTicks);
        }

        @Override
        public void encode(ByteBuf buffer, CardRevealEntityPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buffer, payload.entityId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.entityUuid());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cardId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.itemId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.cardType());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.titleKey());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.bodyKey());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.largeFrontTexture());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.largeBackTexture());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.animation());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks());
        }

    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}