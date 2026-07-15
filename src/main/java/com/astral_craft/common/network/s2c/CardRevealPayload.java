package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client-side card reveal payload. */
public record CardRevealPayload(
        String cardId,
        ItemStack stack,
        String cardType,
        Component title,
        Component body,
        Identifier largeFrontTexture,
        Identifier largeBackTexture,
        Identifier animation,
        int durationTicks,
        int sourceEntityId,
        List<Integer> targetEntityIds
) implements CustomPacketPayload {

    public static final Identifier ANIMATION_FLIP = AstralCraft.prefix("flip");
    public static final Identifier ANIMATION_APPROACH = AstralCraft.prefix("approach");

    public static final CustomPacketPayload.Type<CardRevealPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_reveal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardRevealPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::cardId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            CardRevealPayload::stack,
            ByteBufCodecs.STRING_UTF8,
            CardRevealPayload::cardType,
            ComponentSerialization.TRUSTED_STREAM_CODEC,
            CardRevealPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC,
            CardRevealPayload::body,
            Identifier.STREAM_CODEC,
            CardRevealPayload::largeFrontTexture,
            Identifier.STREAM_CODEC,
            CardRevealPayload::largeBackTexture,
            Identifier.STREAM_CODEC,
            CardRevealPayload::animation,
            ByteBufCodecs.VAR_INT,
            CardRevealPayload::durationTicks,
            ByteBufCodecs.VAR_INT,
            CardRevealPayload::sourceEntityId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)),
            CardRevealPayload::targetEntityIds,
            CardRevealPayload::new);

    public CardRevealPayload {
        targetEntityIds = List.copyOf(targetEntityIds == null ? List.of() : targetEntityIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}