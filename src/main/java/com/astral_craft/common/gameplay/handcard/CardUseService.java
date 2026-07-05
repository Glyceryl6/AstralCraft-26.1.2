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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CardUseService {

    /** Keep this in step with CardRevealOverlay.defaultFlipDurationTicks(). */
    public static final int CARD_REVEAL_DURATION_TICKS = 43;
    public static final int CARD_APPROACH_REVEAL_DURATION_TICKS = 28;
    public static final int VIRTUAL_CARD_HAND_INDEX = -1;
    public static final int DECK_CARD_HAND_INDEX = -2;
    public static final double WORLD_REVEAL_SYNC_RANGE = 96.0D;
    protected static final long DECK_USE_HELD_SUPPRESS_TICKS = 6L;
    protected static final long DECK_USE_SWING_SUPPRESS_TICKS = 10L;
    protected static final long DECK_SCREEN_OPEN_TIMEOUT_TICKS = 20L * 60L * 20L;
    protected static final Map<UUID, Long> DECK_HELD_USE_SUPPRESSION = new ConcurrentHashMap<>();
    protected static final Map<UUID, Long> DECK_SWING_SUPPRESSION = new ConcurrentHashMap<>();
    protected static final Map<UUID, Long> OPEN_DECK_SCREENS = new ConcurrentHashMap<>();

    public static InteractionResult use(BaseHandCard card, Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (consumeSuppressedHeldUse(serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        CardUseResult result = tryUseStack(card, level, serverPlayer, hand, player.getItemInHand(hand), CardUseContext.held(hand));
        return result.interactionResult();
    }

    public static InteractionResult useVirtualCard(ServerPlayer player, String rawCardId) {
        Identifier itemId = resolveCardItemId(rawCardId);
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (!(item instanceof BaseHandCard card)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return InteractionResult.FAIL;
        }

        CardUseResult result = tryUseStack(card, player.level(), player, InteractionHand.MAIN_HAND, new ItemStack(item), CardUseContext.virtual());
        return result.interactionResult();
    }

    public static void useDeckCard(ServerPlayer player, String rawCardId) {
        Identifier itemId = resolveCardItemId(rawCardId);
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (!(item instanceof BaseHandCard card)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return;
        }

        suppressNextHeldUse(player);

        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.hand_card_deck.need_character"), true);
            return;
        }

        ItemStack stack = AstralHandCardManager.firstInventoryCardStack(player, itemId);
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return;
        }

        CardUseResult result = tryUseStack(card, player.level(), player, InteractionHand.MAIN_HAND, stack, CardUseContext.deck());
        if (!result.accept) {
            return;
        }
    }

    protected static Identifier resolveCardItemId(String rawCardId) {
        String raw = rawCardId == null ? "" : rawCardId.trim();
        if (raw.contains(":")) {
            return Identifier.parse(raw);
        }

        return AstralCraft.prefix(raw);
    }

    protected static CardUseResult tryUseStack(
            BaseHandCard card, Level level, ServerPlayer serverPlayer,
            InteractionHand hand, ItemStack stack, CardUseContext useContext) {
        if (level.isClientSide()) {
            return CardUseResult.pass();
        }

        if (PendingCardActionManager.isExclusiveBusy(serverPlayer)) {
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

        if (definition.combatOnly()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.card.combat_only"), true);
            return CardUseResult.consumed();
        }

        if (definition.needsTarget()) {
            int effectiveRange = CardRangeResolver.effectiveRange(serverPlayer, stack, definition);
            String candidates = CardTargeting.encodeCandidates(serverPlayer, stack, definition);
            swingAcceptedUse(serverPlayer, hand, useContext);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenTargetSelectionPayload(
                    definition.id(), useContext.targetSelectionHandIndex(),
                    definition.minTargets(), definition.maxTargets(), effectiveRange, candidates));
            return CardUseResult.accepted();
        }

        swingAcceptedUse(serverPlayer, hand, useContext);

        CardRevealOptions options = card.revealOptions(serverPlayer, hand, stack, definition, List.of());
        if (options.enabled()) {
            if (!PendingCardActionManager.scheduleExclusive(serverPlayer, revealLockTicks(options.durationTicks()),
                    () -> card.applyFromSelection(serverPlayer, hand, List.of()))) {
                return CardUseResult.consumed();
            }
            thisSendReveal(serverPlayer, stack, definition, options, List.of());
            consumeAfterAcceptedUse(serverPlayer, stack, useContext.consumeStack(), useContext.targetSelectionHandIndex(), definition.id());
            return CardUseResult.accepted();
        }

        boolean applied = card.applyFromSelection(serverPlayer, hand, List.of());
        if (applied) {
            consumeAfterAcceptedUse(serverPlayer, stack, useContext.consumeStack(), useContext.targetSelectionHandIndex(), definition.id());
            return CardUseResult.accepted();
        }

        return CardUseResult.consumed();
    }

    public static void applyTargetSelection(ServerPlayer player, CardTargetSelectionPayload payload) {
        if (PendingCardActionManager.isExclusiveBusy(player)) return;
        boolean virtualCard = payload.handIndex() == VIRTUAL_CARD_HAND_INDEX;
        boolean deckCard = payload.handIndex() == DECK_CARD_HAND_INDEX;
        InteractionHand hand = payload.handIndex() == InteractionHand.OFF_HAND.ordinal() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack;
        BaseHandCard card;
        if (virtualCard || deckCard) {
            Identifier itemId = resolveCardItemId(payload.cardId());
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            if (!(item instanceof BaseHandCard baseHandCard)) return;
            card = baseHandCard;
            if (deckCard) {
                suppressNextHeldUse(player);
                stack = AstralHandCardManager.firstInventoryCardStack(player, itemId);
                if (stack.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", payload.cardId()), true);
                    return;
                }
            } else {
                stack = new ItemStack(item);
            }
        } else {
            stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BaseHandCard baseHandCard)) return;
            card = baseHandCard;
        }

        CardDefinition definition = card.definition(stack);
        Component restrictionMessage = useRestrictionMessage(player, definition);
        if (restrictionMessage != null) {
            player.sendSystemMessage(restrictionMessage, true);
            return;
        }

        if (!definition.id().equals(payload.cardId()) || !definition.needsTarget()) return;
        List<LivingEntity> targets = new ArrayList<>();
        for (String raw : payload.selectedEntityIds().split(",")) {
            if (raw.isBlank()) continue;
            try {
                Entity entity = player.level().getEntity(Integer.parseInt(raw));
                if (entity instanceof LivingEntity living && CardTargeting.isValidTarget(player, living, stack, definition) && !targets.contains(living)) {
                    targets.add(living);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (targets.size() < definition.minTargets() || targets.size() > definition.maxTargets()) {
            return;
        }

        swingAcceptedUse(player, hand, CardUseContext.fromHandIndex(payload.handIndex()));

        CardRevealOptions options = card.revealOptions(player, hand, stack, definition, targets);
        if (options.enabled()) {
            List<LivingEntity> capturedTargets = List.copyOf(targets);
            if (!PendingCardActionManager.scheduleExclusive(player, revealLockTicks(options.durationTicks()), () -> card.applyFromSelection(player, hand, capturedTargets))) {
                return;
            }
            thisSendReveal(player, stack, definition, options, targets);
            if (!virtualCard) {
                consumeAfterAcceptedUse(player, stack, !deckCard, payload.handIndex(), definition.id());
            }
        } else {
            boolean applied = card.applyFromSelection(player, hand, targets);
            if (applied && !virtualCard) {
                consumeAfterAcceptedUse(player, stack, !deckCard, payload.handIndex(), definition.id());
            }
        }
    }

    public static void sendReveal(ServerPlayer viewer, CardDefinition definition) {
        sendReveal(viewer, ItemStack.EMPTY, viewer, definition, CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
    }

    /** Exposed for special cards/skills that want the new quick far-to-near animation. */
    public static void sendApproachReveal(ServerPlayer viewer, CardDefinition definition) {
        sendReveal(viewer, ItemStack.EMPTY, viewer, definition, CardRevealPayload.ANIMATION_APPROACH, CARD_APPROACH_REVEAL_DURATION_TICKS);
    }

    public static void sendReveal(ServerPlayer viewer, CardDefinition definition, String animation, int durationTicks) {
        sendReveal(viewer, ItemStack.EMPTY, viewer, definition, animation, durationTicks);
    }

    public static void sendReveal(ServerPlayer viewer, ItemStack stack, ServerPlayer owner, CardDefinition definition, String animation, int durationTicks) {
        CardType cardType = stack.isEmpty() ? definition.type() : stack.getOrDefault(AstralDataComponents.CARD_TYPE, definition.type());
        PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(definition.id(),
                AstralCraft.MOD_ID + ":" + definition.registryPath(),
                cardType.getSerializedName(), definition.nameKey(),
                definition.effectKey(), definition.largeFrontTexture(),
                CardBackPreferenceManager.selectedTexture(owner).toString(),
                animation, durationTicks));
    }

    public static void sendEntityRevealAround(ServerPlayer owner, ItemStack stack, CardDefinition definition, String animation, int durationTicks) {
        CardType cardType = stack.isEmpty() ? definition.type() : stack.getOrDefault(AstralDataComponents.CARD_TYPE, definition.type());
        sendEntityRevealAround(owner,
                definition.id(),
                AstralCraft.MOD_ID + ":" + definition.registryPath(),
                cardType.getSerializedName(),
                definition.nameKey(),
                definition.effectKey(),
                definition.largeFrontTexture(),
                CardBackPreferenceManager.selectedTexture(owner).toString(),
                animation,
                durationTicks);
    }

    public static void sendEntityRevealAround(ServerPlayer owner, String cardId, String itemId, String cardType, String titleKey,
                                              String bodyKey, String largeFrontTexture, String largeBackTexture,
                                              String animation, int durationTicks) {
        CardRevealEntityPayload payload = new CardRevealEntityPayload(owner.getId(), cardId, itemId, cardType,
                titleKey, bodyKey, largeFrontTexture, largeBackTexture, animation, durationTicks);
        double maxDistanceSqr = WORLD_REVEAL_SYNC_RANGE * WORLD_REVEAL_SYNC_RANGE;
        for (ServerPlayer viewer : owner.level().players()) {
            if (viewer.distanceToSqr(owner) <= maxDistanceSqr) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    protected static void thisSendReveal(ServerPlayer owner, ItemStack stack, CardDefinition definition, CardRevealOptions options, List<LivingEntity> targets) {
        for (ServerPlayer viewer : viewers(owner, options.audience(), targets)) {
            sendReveal(viewer, stack, owner, definition, options.animation(), options.durationTicks());
        }
        sendEntityRevealAround(owner, stack, definition, options.animation(), options.durationTicks());
    }

    protected static int revealLockTicks(int requestedTicks) {
        return Math.max(Math.max(0, requestedTicks), AstralGameplayConfig.cardRevealLockTicks());
    }

    public static void markDeckScreenOpen(ServerPlayer player) {
        if (player == null) return;
        OPEN_DECK_SCREENS.put(player.getUUID(), player.level().getGameTime() + DECK_SCREEN_OPEN_TIMEOUT_TICKS);
    }

    public static void markDeckScreenClosed(ServerPlayer player) {
        if (player == null) return;
        OPEN_DECK_SCREENS.remove(player.getUUID());
        DECK_SWING_SUPPRESSION.put(player.getUUID(), player.level().getGameTime() + DECK_USE_SWING_SUPPRESS_TICKS);
    }

    public static boolean shouldSuppressDeckSwing(ServerPlayer player) {
        if (player == null) return false;
        long gameTime = player.level().getGameTime();
        Long screenOpenUntil = OPEN_DECK_SCREENS.get(player.getUUID());
        if (screenOpenUntil != null) {
            if (gameTime <= screenOpenUntil) {
                return true;
            }

            OPEN_DECK_SCREENS.remove(player.getUUID(), screenOpenUntil);
        }

        Long suppressUntil = DECK_SWING_SUPPRESSION.get(player.getUUID());
        if (suppressUntil == null) return false;
        if (gameTime > suppressUntil) {
            DECK_SWING_SUPPRESSION.remove(player.getUUID(), suppressUntil);
            return false;
        }

        DECK_SWING_SUPPRESSION.remove(player.getUUID(), suppressUntil);
        return true;
    }

    protected static void suppressNextHeldUse(ServerPlayer player) {
        if (player == null) return;
        DECK_HELD_USE_SUPPRESSION.put(player.getUUID(), player.level().getGameTime() + DECK_USE_HELD_SUPPRESS_TICKS);
        DECK_SWING_SUPPRESSION.put(player.getUUID(), player.level().getGameTime() + DECK_USE_SWING_SUPPRESS_TICKS);
    }

    protected static boolean consumeSuppressedHeldUse(ServerPlayer player) {
        if (player == null) return false;
        Long suppressUntil = DECK_HELD_USE_SUPPRESSION.get(player.getUUID());
        if (suppressUntil == null) return false;
        long gameTime = player.level().getGameTime();
        if (gameTime > suppressUntil) {
            DECK_HELD_USE_SUPPRESSION.remove(player.getUUID(), suppressUntil);
            return false;
        }

        DECK_HELD_USE_SUPPRESSION.remove(player.getUUID(), suppressUntil);
        return true;
    }

    protected static void swingAcceptedUse(ServerPlayer player, InteractionHand hand, CardUseContext useContext) {
        if (useContext.swingArm()) {
            player.swing(hand, true);
        }
    }

    protected static Component useRestrictionMessage(ServerPlayer player, CardDefinition definition) {
        CardUseRestriction restriction = definition.restrictions();
        if (restriction.unrestricted()) return null;
        if (restriction.creativeBypass() && player.getAbilities().instabuild) return null;
        if (restriction.characters().isEmpty()) return null;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active()) {
            return Component.translatable("message.astral_craft.hand_card_deck.need_character");
        }

        Identifier selectedCharacter = state.characterId();
        CharacterProgress progress = CharacterProgressManager.progress(player);
        if (restriction.requireSelectedCharacterUnlocked() && CharacterManager.INSTANCE.contains(selectedCharacter)
                && !progress.isCharacterUnlocked(selectedCharacter) && !CharacterManager.INSTANCE.get(selectedCharacter).unlockedByDefault()) {
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

        protected static CardUseContext virtual() {
            return new CardUseContext(false, VIRTUAL_CARD_HAND_INDEX, true);
        }

        protected static CardUseContext deck() {
            return new CardUseContext(false, DECK_CARD_HAND_INDEX, false);
        }

        protected static CardUseContext fromHandIndex(int handIndex) {
            if (handIndex == DECK_CARD_HAND_INDEX) {
                return deck();
            }
            if (handIndex == VIRTUAL_CARD_HAND_INDEX) {
                return virtual();
            }

            return new CardUseContext(true, handIndex, true);
        }
    }

    protected static List<ServerPlayer> viewers(ServerPlayer owner, CardRevealAudience audience, List<LivingEntity> targets) {
        Set<ServerPlayer> viewers = new LinkedHashSet<>();
        switch (audience) {
            case NONE -> {}
            case SELF -> viewers.add(owner);
            case TARGET_PLAYERS -> {
                for (LivingEntity target : targets) {
                    if (target instanceof ServerPlayer player) {
                        viewers.add(player);
                    }
                }
            }

            case SELF_AND_TARGET_PLAYERS -> {
                viewers.add(owner);
                for (LivingEntity target : targets) {
                    if (target instanceof ServerPlayer player) {
                        viewers.add(player);
                    }
                }
            }

            case TRACKING_NEARBY -> {
                viewers.add(owner);
                for (ServerPlayer player : owner.level().players()) {
                    if (player.distanceToSqr(owner) <= 64.0D * 64.0D) {
                        viewers.add(player);
                    }
                }
            }
        }

        return new ArrayList<>(viewers);
    }

    private static void consumeAfterAcceptedUse(ServerPlayer player, ItemStack stack, boolean consumeStack, int handIndex, String definitionId) {
        if (handIndex == DECK_CARD_HAND_INDEX) {
            AstralHandCardManager.removeFromInventory(player, stack, 1);
            return;
        }

        if (consumeStack) {
            consume(player, stack);
        }
    }

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

}