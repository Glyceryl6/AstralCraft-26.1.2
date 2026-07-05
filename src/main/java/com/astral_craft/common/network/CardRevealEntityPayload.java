package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * World-space card reveal payload.
 *
 * <p>This mirrors {@link CardRevealPayload}, but also carries the source entity id so the client can render the
 * same card reveal above that entity instead of forcing another player's full-screen overlay.</p>
 */
public record CardRevealEntityPayload(
        int entityId,
        String cardId,
        ItemStack stack,
        String cardType,
        Component title,
        Component body,
        Identifier largeFrontTexture,
        Identifier largeBackTexture,
        Identifier animation,
        int durationTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardRevealEntityPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_reveal_entity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardRevealEntityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CardRevealEntityPayload::entityId,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::cardId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            CardRevealEntityPayload::stack,
            ByteBufCodecs.STRING_UTF8,
            CardRevealEntityPayload::cardType,
            ComponentSerialization.TRUSTED_STREAM_CODEC,
            CardRevealEntityPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC,
            CardRevealEntityPayload::body,
            Identifier.STREAM_CODEC,
            CardRevealEntityPayload::largeFrontTexture,
            Identifier.STREAM_CODEC,
            CardRevealEntityPayload::largeBackTexture,
            Identifier.STREAM_CODEC,
            CardRevealEntityPayload::animation,
            ByteBufCodecs.VAR_INT,
            CardRevealEntityPayload::durationTicks,
            CardRevealEntityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
