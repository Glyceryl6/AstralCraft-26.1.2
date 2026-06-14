package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.CardRevealPayload;
import com.astral_craft.common.network.CardTargetSelectionPayload;
import com.astral_craft.common.network.OpenTargetSelectionPayload;
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
import java.util.List;

public final class CardUseService {

    /** Keep this in step with CardRevealOverlay.defaultFlipDurationTicks(). */
    private static final int CARD_REVEAL_DURATION_TICKS = 52;
    private static final int CARD_APPROACH_REVEAL_DURATION_TICKS = 30;

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

        CardDefinition definition = card.definition();
        ItemStack stack = player.getItemInHand(hand);
        if (definition.combatOnly()) {
            return InteractionResult.PASS;
        }

        if (definition.needsTarget()) {
            String candidates = CardTargeting.encodeCandidates(serverPlayer, definition);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenTargetSelectionPayload(definition.id(), hand.ordinal(), definition.minTargets(), definition.maxTargets(), definition.range(), candidates));
            return InteractionResult.SUCCESS;
        }

        if (definition.shouldRevealOnUse()) {
            sendReveal(serverPlayer, definition, CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
            consume(serverPlayer, stack);
            PendingCardActionManager.schedule(serverPlayer, CARD_REVEAL_DURATION_TICKS, () -> card.applyFromSelection(serverPlayer, hand, List.of()));
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
        CardDefinition definition = card.definition();
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

        if (definition.shouldRevealOnUse()) {
            sendReveal(player, definition, CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
            consume(player, stack);
            List<LivingEntity> capturedTargets = List.copyOf(targets);
            PendingCardActionManager.schedule(player, CARD_REVEAL_DURATION_TICKS, () -> card.applyFromSelection(player, hand, capturedTargets));
        } else {
            boolean applied = card.applyFromSelection(player, hand, targets);
            if (applied) consume(player, stack);
        }
    }

    public static void sendReveal(ServerPlayer viewer, CardDefinition definition) {
        sendReveal(viewer, definition, CardRevealPayload.ANIMATION_FLIP, CARD_REVEAL_DURATION_TICKS);
    }

    /**
     * Exposed for special cards/skills that want the new quick far-to-near animation.
     */
    public static void sendApproachReveal(ServerPlayer viewer, CardDefinition definition) {
        sendReveal(viewer, definition, CardRevealPayload.ANIMATION_APPROACH, CARD_APPROACH_REVEAL_DURATION_TICKS);
    }

    public static void sendReveal(ServerPlayer viewer, CardDefinition definition, String animation, int durationTicks) {
        PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(
                definition.id(), AstralCraft.MOD_ID + ":" + definition.registryPath(),
                definition.nameKey(), definition.effectKey(), definition.largeFrontTexture(),
                definition.largeBackTexture(), animation, durationTicks));
    }

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

}