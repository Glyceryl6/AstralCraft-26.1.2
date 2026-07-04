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
import java.util.Set;

public class CardUseService {

    /** Keep this in step with CardRevealOverlay.defaultFlipDurationTicks(). */
    public static final int CARD_REVEAL_DURATION_TICKS = 43;
    public static final int CARD_APPROACH_REVEAL_DURATION_TICKS = 28;
    public static final int VIRTUAL_CARD_HAND_INDEX = -1;
    public static final int DECK_CARD_HAND_INDEX = -2;
    public static final double WORLD_REVEAL_SYNC_RANGE = 96.0D;

    public static InteractionResult use(BaseHandCard card, Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        return useStack(card, level, serverPlayer, hand, player.getItemInHand(hand), true, hand.ordinal());
    }

    public static InteractionResult useVirtualCard(ServerPlayer player, String rawCardId) {
        Identifier itemId = resolveCardItemId(rawCardId);
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (!(item instanceof BaseHandCard card)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return InteractionResult.FAIL;
        }

        return useStack(card, player.level(), player, InteractionHand.MAIN_HAND, new ItemStack(item), false, VIRTUAL_CARD_HAND_INDEX);
    }

    public static InteractionResult useDeckCard(ServerPlayer player, String rawCardId) {
        Identifier itemId = resolveCardItemId(rawCardId);
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (!(item instanceof BaseHandCard card)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return InteractionResult.FAIL;
        }

        boolean creative = player.getAbilities().instabuild;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!creative && !state.active()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.hand_card_deck.need_character"), true);
            return InteractionResult.SUCCESS;
        }

        if (!creative && !AstralHandCardManager.hand(player).has(itemId)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", rawCardId), true);
            return InteractionResult.FAIL;
        }

        return useStack(card, player.level(), player, InteractionHand.MAIN_HAND, new ItemStack(item), false, DECK_CARD_HAND_INDEX);
    }

    protected static Identifier resolveCardItemId(String rawCardId) {
        String raw = rawCardId == null ? "" : rawCardId.trim();
        if (raw.contains(":")) {
            return Identifier.parse(raw);
        }

        return AstralCraft.prefix(raw);
    }

    protected static InteractionResult useStack(BaseHandCard card, Level level, ServerPlayer serverPlayer, InteractionHand hand, ItemStack stack, boolean consumeStack, int targetSelectionHandIndex) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (PendingCardActionManager.isExclusiveBusy(serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (KnockdownManager.isRecovering(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.knockdown.no_cards"), true);
            return InteractionResult.SUCCESS;
        }

        CardDefinition definition = card.definition(stack);
        Component restrictionMessage = useRestrictionMessage(serverPlayer, definition);
        if (restrictionMessage != null) {
            serverPlayer.sendSystemMessage(restrictionMessage, true);
            return InteractionResult.SUCCESS;
        }

        if (definition.combatOnly()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.card.combat_only"), true);
            return InteractionResult.SUCCESS;
        }

        if (definition.needsTarget()) {
            int effectiveRange = CardRangeResolver.effectiveRange(serverPlayer, stack, definition);
            String candidates = CardTargeting.encodeCandidates(serverPlayer, stack, definition);
            swingAcceptedUse(serverPlayer, hand);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenTargetSelectionPayload(definition.id(), targetSelectionHandIndex, definition.minTargets(), definition.maxTargets(), effectiveRange, candidates));
            return InteractionResult.SUCCESS;
        }

        swingAcceptedUse(serverPlayer, hand);

        CardRevealOptions options = card.revealOptions(serverPlayer, hand, stack, definition, List.of());
        if (options.enabled()) {
            if (!PendingCardActionManager.scheduleExclusive(serverPlayer, revealLockTicks(options.durationTicks()), () -> card.applyFromSelection(serverPlayer, hand, List.of()))) {
                return InteractionResult.SUCCESS;
            }
            thisSendReveal(serverPlayer, stack, definition, options, List.of());
            consumeAfterAcceptedUse(serverPlayer, stack, consumeStack, targetSelectionHandIndex, definition.id());
        } else {
            boolean applied = card.applyFromSelection(serverPlayer, hand, List.of());
            if (applied) {
                consumeAfterAcceptedUse(serverPlayer, stack, consumeStack, targetSelectionHandIndex, definition.id());
            }
        }

        return InteractionResult.SUCCESS;
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
            stack = new ItemStack(item);
            if (deckCard && !player.getAbilities().instabuild && !AstralHandCardManager.hand(player).has(itemId)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.card_deck.missing_card", payload.cardId()), true);
                return;
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

        swingAcceptedUse(player, hand);

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
        CardRevealEntityPayload payload = new CardRevealEntityPayload(owner.getId(), owner.getUUID().toString(), cardId, itemId, cardType,
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

    protected static void swingAcceptedUse(ServerPlayer player, InteractionHand hand) {
        player.swing(hand, true);
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
            if (!player.getAbilities().instabuild) {
                AstralHandCardManager.remove(player, resolveCardItemId(definitionId), 1);
            }
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