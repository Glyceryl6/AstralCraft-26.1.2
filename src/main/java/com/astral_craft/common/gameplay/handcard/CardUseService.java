package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.config.AstralGameplayConfig;
import com.astral_craft.common.gameplay.KnockdownManager;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgress;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.CardRevealEntityPayload;
import com.astral_craft.common.network.CardRevealPayload;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.CardTargetSelectionPayload;
import com.astral_craft.common.network.OpenTargetSelectionPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CardUseService {

    /** Keep this in step with CardRevealOverlay.defaultFlipDurationTicks(). */
    public static final int CARD_REVEAL_DURATION_TICKS = 43;
    public static final int CARD_APPROACH_REVEAL_DURATION_TICKS = 28;
    public static final int DECK_CARD_HAND_INDEX = -2;

    public static InteractionResult use(BaseHandCard card, Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        CardUseResult result = tryUseStack(card, level, serverPlayer, hand, player.getItemInHand(hand), CardUseContext.held(hand));
        return result.interactionResult();
    }

    public static boolean useDeckCard(ServerPlayer player, String rawCardId) {
        Identifier itemId;
        try {
            itemId = resolveCardItemId(rawCardId);
        } catch (RuntimeException exception) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return false;
        }

        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (!(item instanceof BaseHandCard card)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return false;
        }

        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.hand_card_deck.need_character"), true);
            return false;
        }

        ItemStack requestedStack = new ItemStack(item);
        ItemStack stack = AstralHandCardManager.firstInventoryCardStack(player, requestedStack);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return false;
        }

        return tryUseStack(card, player.level(), player, InteractionHand.MAIN_HAND, stack, CardUseContext.deck()).accept();
    }

    protected static Identifier resolveCardItemId(String rawCardId) {
        String raw = rawCardId == null ? "" : rawCardId.trim();
        if (raw.contains(":")) return Identifier.parse(raw);
        return AstralCraft.prefix(raw);
    }

    protected static CardUseResult tryUseStack(
            BaseHandCard card, Level level, ServerPlayer serverPlayer,
            InteractionHand hand, ItemStack stack, CardUseContext useContext) {
        if (level.isClientSide()) return CardUseResult.pass();
        if (stack.isEmpty() || stack.getItem() != card) return CardUseResult.consumed();
        if (PendingCardActionManager.isExclusiveBusy(serverPlayer)
                || PendingCardActionManager.hasPendingSelection(serverPlayer)) {
            return CardUseResult.consumed();
        }

        if (KnockdownManager.isRecovering(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.knockdown.no_cards"), true);
            return CardUseResult.consumed();
        }

        CardDefinition definition = card.definition(stack);
        Component restrictionMessage = useRestrictionMessage(serverPlayer, definition);
        if (restrictionMessage != null) {
            serverPlayer.sendSystemMessage(restrictionMessage, true);
            return CardUseResult.consumed();
        }

        if (definition.isCombatOnly()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.card.combat_only"), true);
            return CardUseResult.consumed();
        }

        if (definition.needsTarget()) {
            int effectiveRange = CardRangeResolver.effectiveRange(serverPlayer, stack, definition);
            List<CardTargetCandidate> candidates = CardTargeting.candidates(serverPlayer, stack, definition, card);
            if (candidates.isEmpty()) return CardUseResult.consumed();
            swingAcceptedUse(serverPlayer, hand, useContext);
            OpenTargetSelectionPayload openPayload = new OpenTargetSelectionPayload(
                    stack.copyWithCount(1), useContext.targetSelectionHandIndex(),
                    definition.minTargets(), definition.maxTargets(), effectiveRange, candidates);
            if (card.revealBeforeTargetSelection()) {
                List<ServerPlayer> revealViewers = card.revealViewers(serverPlayer, definition, List.of());
                if (!revealViewers.isEmpty()) {
                    if (!PendingCardActionManager.scheduleExclusive(serverPlayer, revealLockTicks(CARD_REVEAL_DURATION_TICKS), () -> {
                        PendingCardActionManager.beginTargetSelection(serverPlayer, openPayload.cardStack(), openPayload.handIndex());
                        PacketDistributor.sendToPlayer(serverPlayer, openPayload);
                    })) {
                        return CardUseResult.consumed();
                    }

                    sendReveal(serverPlayer, stack, definition, revealViewers,
                            CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
                    return CardUseResult.accepted();
                }
            }

            PendingCardActionManager.beginTargetSelection(serverPlayer, openPayload.cardStack(), openPayload.handIndex());
            PacketDistributor.sendToPlayer(serverPlayer, openPayload);
            return CardUseResult.accepted();
        }

        swingAcceptedUse(serverPlayer, hand, useContext);
        List<ServerPlayer> revealViewers = card.revealViewers(serverPlayer, definition, List.of());
        if (!revealViewers.isEmpty()) {
            ItemStack source = stack.copyWithCount(1);
            if (!PendingCardActionManager.scheduleExclusive(serverPlayer, revealLockTicks(CARD_REVEAL_DURATION_TICKS),
                    () -> card.onRevealFinished(serverPlayer, hand, source, definition))) {
                return CardUseResult.consumed();
            }

            consumeAfterAcceptedUse(serverPlayer, stack, useContext.consumeStack(), useContext.targetSelectionHandIndex());
            sendReveal(serverPlayer, source, definition, revealViewers,
                    CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
            return CardUseResult.accepted();
        }

        if (card.onRevealFinished(serverPlayer, hand, stack.copyWithCount(1), definition)) {
            consumeAfterAcceptedUse(serverPlayer, stack, useContext.consumeStack(), useContext.targetSelectionHandIndex());
            return CardUseResult.accepted();
        }

        return CardUseResult.consumed();
    }

    public static void applyTargetSelection(ServerPlayer player, CardTargetSelectionPayload payload) {
        if (PendingCardActionManager.isExclusiveBusy(player)) return;
        if (payload.cardStack().isEmpty()) return;
        if (PendingCardActionManager.consumeTargetSelection(
                player, payload.cardStack(), payload.handIndex()) == null) return;
        boolean deckCard = payload.handIndex() == DECK_CARD_HAND_INDEX;
        boolean heldCard = payload.handIndex() == InteractionHand.MAIN_HAND.ordinal()
                || payload.handIndex() == InteractionHand.OFF_HAND.ordinal();
        if (!deckCard && !heldCard) return;
        InteractionHand hand = payload.handIndex() == InteractionHand.OFF_HAND.ordinal()
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Item requestedItem = payload.cardStack().getItem();
        if (!(requestedItem instanceof BaseHandCard requestedCard)) return;
        ItemStack stack;
        BaseHandCard card;
        if (deckCard) {
            card = requestedCard;
            stack = AstralHandCardManager.firstInventoryCardStack(player, payload.cardStack());
            if (stack.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card",
                        BuiltInRegistries.ITEM.getKey(requestedItem).toString()), true);
                return;
            }
        } else {
            stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BaseHandCard heldHandCard) || stack.getItem() != requestedItem) return;
            card = heldHandCard;
        }

        CardDefinition definition = card.definition(stack);
        Component restrictionMessage = useRestrictionMessage(player, definition);
        if (restrictionMessage != null) {
            player.sendSystemMessage(restrictionMessage, true);
            return;
        }

        if (!definition.needsTarget()) return;
        if (payload.selectedEntityIds().size() < definition.minTargets()
                || payload.selectedEntityIds().size() > definition.maxTargets()) return;
        Set<Integer> uniqueEntityIds = new LinkedHashSet<>(payload.selectedEntityIds());
        if (uniqueEntityIds.size() != payload.selectedEntityIds().size()) return;
        List<LivingEntity> targets = new ArrayList<>(uniqueEntityIds.size());
        for (int entityId : uniqueEntityIds) {
            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof LivingEntity living) || !CardTargeting.isValidTarget(player, living, stack, definition, card)) return;
            targets.add(living);
        }

        swingAcceptedUse(player, hand, CardUseContext.fromHandIndex(payload.handIndex()));
        if (card.revealBeforeTargetSelection()) {
            if (card.applyFromSelection(player, hand, targets)) {
                consumeAfterAcceptedUse(player, stack, !deckCard, payload.handIndex());
            }
            return;
        }

        List<ServerPlayer> revealViewers = card.revealViewers(player, definition, targets);
        if (!revealViewers.isEmpty()) {
            List<LivingEntity> capturedTargets = List.copyOf(targets);
            if (!PendingCardActionManager.scheduleExclusive(player, revealLockTicks(CARD_REVEAL_DURATION_TICKS),
                    () -> card.applyFromSelection(player, hand, capturedTargets))) {
                return;
            }

            sendReveal(player, stack, definition, revealViewers,
                    CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
            consumeAfterAcceptedUse(player, stack, !deckCard, payload.handIndex());
        } else if (card.applyFromSelection(player, hand, targets)) {
            consumeAfterAcceptedUse(player, stack, !deckCard, payload.handIndex());
        }
    }

    public static void sendReveal(ServerPlayer viewer, ItemStack stack, ServerPlayer owner,
                                  CardDefinition definition, Identifier animation, int durationTicks) {
        PacketDistributor.sendToPlayer(viewer, cardRevealPayload(owner, stack, definition, animation, durationTicks));
    }

    public static void sendEntityRevealAround(ServerPlayer owner, ItemStack stack, CardDefinition definition,
                                              Identifier animation, int durationTicks) {
        CardType cardType = stack.getOrDefault(AstralDataComponents.CARD_TYPE, definition.type());
        sendEntityRevealAround(owner, definition.itemId(stack).toString(), revealStack(stack), cardType.getSerializedName(),
                revealTitle(stack, definition), revealBody(owner, stack, definition), definition.largeFrontTexture(stack),
                definition.largeBackTextureOverride().orElseGet(() -> CardBackPreferenceManager.selectedTexture(owner)),
                animation, durationTicks);
    }

    public static void sendEntityRevealAround(
            ServerPlayer owner, String cardId, ItemStack stack, String cardType, Component title, Component body,
            Identifier largeFrontTexture, Identifier largeBackTexture, Identifier animation, int durationTicks) {
        CardRevealEntityPayload payload = new CardRevealEntityPayload(owner.getId(), cardId, stack, cardType,
                title, body, largeFrontTexture, largeBackTexture, animation, durationTicks);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, payload);
    }

    protected static void sendReveal(ServerPlayer owner, ItemStack stack, CardDefinition definition,
                                     List<ServerPlayer> viewers, Identifier animation, int durationTicks) {
        CardRevealPayload payload = cardRevealPayload(owner, stack, definition, animation, durationTicks);
        for (ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }

        sendEntityRevealAround(owner, stack, definition, animation, durationTicks);
    }

    protected static CardRevealPayload cardRevealPayload(ServerPlayer owner, ItemStack stack,
                                                          CardDefinition definition, Identifier animation, int durationTicks) {
        CardType cardType = stack.getOrDefault(AstralDataComponents.CARD_TYPE, definition.type());
        return new CardRevealPayload(definition.itemId(stack).toString(), revealStack(stack),
                cardType.getSerializedName(), revealTitle(stack, definition), revealBody(owner, stack, definition),
                definition.largeFrontTexture(stack),
                definition.largeBackTextureOverride().orElseGet(() -> CardBackPreferenceManager.selectedTexture(owner)),
                animation, durationTicks);
    }

    protected static ItemStack revealStack(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    protected static Component revealTitle(ItemStack stack, CardDefinition definition) {
        return definition.displayName(stack);
    }

    protected static Component revealBody(ServerPlayer owner, ItemStack stack, CardDefinition definition) {
        return definition.effectText(stack, CardRangeResolver.effectiveRange(owner, stack, definition));
    }

    protected static int revealLockTicks(int requestedTicks) {
        return Math.max(Math.max(0, requestedTicks), AstralGameplayConfig.cardRevealLockTicks());
    }

    protected static void swingAcceptedUse(ServerPlayer player, InteractionHand hand, CardUseContext useContext) {
        if (useContext.swingArm()) player.swing(hand, true);
    }

    protected static Component useRestrictionMessage(ServerPlayer player, CardDefinition definition) {
        CardUseRestriction restriction = definition.restrictions();
        if (restriction.unrestricted()) return null;
        if (restriction.creativeBypass() && player.getAbilities().instabuild) return null;
        if (restriction.characters().isEmpty()) return null;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active()) return Component.translatable("message.astral_craft.hand_card_deck.need_character");
        Identifier selectedCharacter = state.characterId();
        CharacterProgress progress = CharacterProgressManager.progress(player);
        if (restriction.requireSelectedCharacterUnlocked() && CharacterManager.INSTANCE.contains(selectedCharacter)
                && !progress.isCharacterUnlocked(selectedCharacter)
                && !CharacterManager.INSTANCE.get(selectedCharacter).unlockedByDefault()) {
            return Component.translatable("message.astral_craft.card_restriction.character_locked");
        }

        if (!restriction.characters().contains(selectedCharacter)) {
            return Component.translatable("message.astral_craft.card_restriction.character");
        }

        return null;
    }

    protected record CardUseResult(InteractionResult interactionResult, boolean accept) {

        protected static CardUseResult pass() {
            return new CardUseResult(InteractionResult.PASS, false);
        }

        protected static CardUseResult consumed() {
            return new CardUseResult(InteractionResult.CONSUME, false);
        }

        protected static CardUseResult accepted() {
            return new CardUseResult(InteractionResult.CONSUME, true);
        }

    }

    protected record CardUseContext(boolean consumeStack, int targetSelectionHandIndex, boolean swingArm) {

        protected static CardUseContext held(InteractionHand hand) {
            return new CardUseContext(true, hand.ordinal(), true);
        }

        protected static CardUseContext deck() {
            return new CardUseContext(false, DECK_CARD_HAND_INDEX, false);
        }

        protected static CardUseContext fromHandIndex(int handIndex) {
            if (handIndex == DECK_CARD_HAND_INDEX) return deck();
            return new CardUseContext(true, handIndex, true);
        }

    }

    private static void consumeAfterAcceptedUse(ServerPlayer player, ItemStack stack, boolean consumeStack, int handIndex) {
        if (handIndex == DECK_CARD_HAND_INDEX) {
            AstralHandCardManager.removeFromInventory(player, stack, 1);
            return;
        }

        if (consumeStack) stack.consume(1, player);
    }

}