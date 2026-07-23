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
        UUID revealId,
        boolean showRelationship
) implements CustomPacketPayload {

    public static final Identifier ANIMATION_FLIP = AstralCraft.prefix("flip");
    public static final Identifier ANIMATION_APPROACH = AstralCraft.prefix("approach");

    public static final CustomPacketPayload.Type<CardRevealPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_reveal"));

    private static final StreamCodec<RegistryFriendlyByteBuf, RelationshipPresentation> RELATIONSHIP_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RelationshipPresentation::sourceEntityId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), RelationshipPresentation::targetEntityIds,
            ByteBufCodecs.BOOL, RelationshipPresentation::visible,
            RelationshipPresentation::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, CardRevealPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CardRevealPayload::cardId,
            ItemStack.OPTIONAL_STREAM_CODEC, CardRevealPayload::stack,
            ByteBufCodecs.STRING_UTF8, CardRevealPayload::cardType,
            ComponentSerialization.TRUSTED_STREAM_CODEC, CardRevealPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, CardRevealPayload::body,
            Identifier.STREAM_CODEC, CardRevealPayload::largeFrontTexture,
            Identifier.STREAM_CODEC, CardRevealPayload::largeBackTexture,
            Identifier.STREAM_CODEC, CardRevealPayload::animation,
            ByteBufCodecs.VAR_INT, CardRevealPayload::durationTicks,
            RELATIONSHIP_STREAM_CODEC, payload -> new RelationshipPresentation(
                    payload.sourceEntityId(), payload.targetEntityIds(), payload.showRelationship()),
            BoardNetworkCodecs.UUID_STREAM_CODEC, CardRevealPayload::revealId,
            (cardId, stack, cardType, title, body, largeFrontTexture, largeBackTexture, animation,
             durationTicks, relationship, revealId) -> new CardRevealPayload(cardId, stack, cardType, title, body,
                    largeFrontTexture, largeBackTexture, animation, durationTicks, relationship.sourceEntityId(),
                    relationship.targetEntityIds(), revealId, relationship.visible()));

    public CardRevealPayload(String cardId, ItemStack stack, String cardType, Component title, Component body,
                             Identifier largeFrontTexture, Identifier largeBackTexture, Identifier animation,
                             int durationTicks, int sourceEntityId, List<Integer> targetEntityIds) {
        this(cardId, stack, cardType, title, body, largeFrontTexture, largeBackTexture, animation,
                durationTicks, sourceEntityId, targetEntityIds, UUID.randomUUID(), true);
    }

    public CardRevealPayload(String cardId, ItemStack stack, String cardType, Component title, Component body,
                             Identifier largeFrontTexture, Identifier largeBackTexture, Identifier animation,
                             int durationTicks, int sourceEntityId, List<Integer> targetEntityIds, UUID revealId) {
        this(cardId, stack, cardType, title, body, largeFrontTexture, largeBackTexture, animation,
                durationTicks, sourceEntityId, targetEntityIds, revealId, true);
    }

    public CardRevealPayload(String cardId, ItemStack stack, String cardType, Component title, Component body,
                             Identifier largeFrontTexture, Identifier largeBackTexture, Identifier animation,
                             int durationTicks, int sourceEntityId, List<Integer> targetEntityIds,
                             boolean showRelationship) {
        this(cardId, stack, cardType, title, body, largeFrontTexture, largeBackTexture, animation,
                durationTicks, sourceEntityId, targetEntityIds, UUID.randomUUID(), showRelationship);
    }

    public CardRevealPayload {
        targetEntityIds = List.copyOf(targetEntityIds == null ? List.of() : targetEntityIds);
        revealId = revealId == null ? UUID.randomUUID() : revealId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private record RelationshipPresentation(int sourceEntityId, List<Integer> targetEntityIds, boolean visible) {
        private RelationshipPresentation {
            targetEntityIds = List.copyOf(targetEntityIds == null ? List.of() : targetEntityIds);
        }
    }
}
