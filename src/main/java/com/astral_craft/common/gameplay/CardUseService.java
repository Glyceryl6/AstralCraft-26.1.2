package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.CardRevealPayload;
import com.astral_craft.common.network.CardTargetSelectionPayload;
import com.astral_craft.common.network.OpenTargetSelectionPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

    public static InteractionResult use(BaseHandCard card, Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (KnockdownManager.isRecovering(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.knockdown.no_cards"), true);
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        CardDefinition definition = card.definition(stack);
        if (definition.combatOnly()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.astral_craft.card.combat_only"), true);
            return InteractionResult.SUCCESS;
        }

        if (definition.needsTarget()) {
            String candidates = CardTargeting.encodeCandidates(serverPlayer, definition);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenTargetSelectionPayload(definition.id(), hand.ordinal(), definition.minTargets(), definition.maxTargets(), definition.range(), candidates));
            return InteractionResult.SUCCESS;
        }

        CardRevealOptions options = card.revealOptions(serverPlayer, hand, stack, definition, List.of());
        if (options.enabled()) {
            thisSendReveal(serverPlayer, stack, definition, options, List.of());
            consume(serverPlayer, stack);
            PendingCardActionManager.schedule(serverPlayer, options.durationTicks(), () -> card.applyFromSelection(serverPlayer, hand, List.of()));
        } else {
            boolean applied = card.applyFromSelection(serverPlayer, hand, List.of());
            if (applied) consume(serverPlayer, stack);
        }

        return InteractionResult.SUCCESS;
    }

    public static void applyTargetSelection(ServerPlayer player, CardTargetSelectionPayload payload) {
        InteractionHand hand = payload.handIndex() == InteractionHand.OFF_HAND.ordinal() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BaseHandCard card)) return;
        CardDefinition definition = card.definition(stack);
        if (!definition.id().equals(payload.cardId()) || !definition.needsTarget()) return;
        List<LivingEntity> targets = new ArrayList<>();
        for (String raw : payload.selectedEntityIds().split(",")) {
            if (raw.isBlank()) continue;
            try {
                Entity entity = player.level().getEntity(Integer.parseInt(raw));
                if (entity instanceof LivingEntity living && CardTargeting.isValidTarget(player, living, definition) && !targets.contains(living)) {
                    targets.add(living);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (targets.size() < definition.minTargets() || targets.size() > definition.maxTargets()) {
            return;
        }

        CardRevealOptions options = card.revealOptions(player, hand, stack, definition, targets);
        if (options.enabled()) {
            thisSendReveal(player, stack, definition, options, targets);
            consume(player, stack);
            List<LivingEntity> capturedTargets = List.copyOf(targets);
            PendingCardActionManager.schedule(player, options.durationTicks(), () -> card.applyFromSelection(player, hand, capturedTargets));
        } else {
            boolean applied = card.applyFromSelection(player, hand, targets);
            if (applied) consume(player, stack);
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

    protected static void thisSendReveal(ServerPlayer owner, ItemStack stack, CardDefinition definition, CardRevealOptions options, List<LivingEntity> targets) {
        for (ServerPlayer viewer : viewers(owner, options.audience(), targets)) {
            sendReveal(viewer, stack, owner, definition, options.animation(), options.durationTicks());
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

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

}