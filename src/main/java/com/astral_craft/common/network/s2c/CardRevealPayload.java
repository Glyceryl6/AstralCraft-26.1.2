package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

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
        List<Integer> targetEntityIds,
        UUID revealId
) implements CustomPacketPayload {

    public static final Identifier ANIMATION_FLIP = AstralCraft.prefix("flip");
    public static final Identifier ANIMATION_APPROACH = AstralCraft.prefix("approach");

    public static final CustomPacketPayload.Type<CardRevealPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_reveal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardRevealPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CardRevealPayload decode(RegistryFriendlyByteBuf buffer) {
            return new CardRevealPayload(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer),
                    Identifier.STREAM_CODEC.decode(buffer),
                    Identifier.STREAM_CODEC.decode(buffer),
                    Identifier.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)).decode(buffer),
                    BoardNetworkCodecs.UUID_STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CardRevealPayload value) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.cardId());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.stack());
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.cardType());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, value.title());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, value.body());
            Identifier.STREAM_CODEC.encode(buffer, value.largeFrontTexture());
            Identifier.STREAM_CODEC.encode(buffer, value.largeBackTexture());
            Identifier.STREAM_CODEC.encode(buffer, value.animation());
            ByteBufCodecs.VAR_INT.encode(buffer, value.durationTicks());
            ByteBufCodecs.VAR_INT.encode(buffer, value.sourceEntityId());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)).encode(buffer, value.targetEntityIds());
            BoardNetworkCodecs.UUID_STREAM_CODEC.encode(buffer, value.revealId());
        }
    };

    public CardRevealPayload(String cardId, ItemStack stack, String cardType, Component title, Component body,
                             Identifier largeFrontTexture, Identifier largeBackTexture, Identifier animation,
                             int durationTicks, int sourceEntityId, List<Integer> targetEntityIds) {
        this(cardId, stack, cardType, title, body, largeFrontTexture, largeBackTexture, animation,
                durationTicks, sourceEntityId, targetEntityIds, UUID.randomUUID());
    }

    public CardRevealPayload {
        targetEntityIds = List.copyOf(targetEntityIds == null ? List.of() : targetEntityIds);
        revealId = revealId == null ? UUID.randomUUID() : revealId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
