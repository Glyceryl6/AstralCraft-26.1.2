package com.astral_craft.common.items.cards.pve;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardNumberSelectionHandler;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.CardNumberSelectionPayload;
import com.astral_craft.common.network.s2c.OpenCardNumberSelectionPayload;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class HandcardFateGuidance extends BaseHandCard implements BoardBotEffect, CardNumberSelectionHandler {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 6)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("hai_qing")), Boolean.TRUE, Boolean.TRUE));
    public static final int MIN_DICE_VALUE = 1;
    public static final int MAX_DICE_VALUE = 6;

    public HandcardFateGuidance(Properties properties) {
        super(properties);
    }

    @Override
    public boolean keepBoardUiOpenAfterReveal() {
        return true;
    }

    @Override
    public boolean onRevealFinished(ServerPlayer user, InteractionHand hand, ItemStack itemStack, CardDefinition definition) {
        PendingCardActionManager.beginNumberSelection(user, itemStack, MIN_DICE_VALUE, MAX_DICE_VALUE);
        PacketDistributor.sendToPlayer(user, new OpenCardNumberSelectionPayload(
                itemStack.copyWithCount(1), MIN_DICE_VALUE, MAX_DICE_VALUE));
        return true;
    }

    @Override
    public void applyNumberSelection(ServerPlayer user, CardNumberSelectionPayload payload) {
        PendingCardActionManager.PendingNumberSelection selection = PendingCardActionManager.consumeNumberSelection(
                user, payload.cardStack(), payload.value());
        if (selection == null || selection.cardStack().getItem() != this) return;
        applyEffects(user, payload.value());
        PendingCardActionManager.completeBoardCardUi(user);
    }

    private static void applyEffects(ServerPlayer user, int value) {
        AstralCardEffects.update(user, AstralStats.get(user)
                .setNextMoveFixed(value)
                .addCardPlaysThisTurn(1));
        BoardSessionManager.reduceSkillCooldown(user, 1);
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        int value = context.level().getRandom().nextInt(MAX_DICE_VALUE - MIN_DICE_VALUE + 1) + MIN_DICE_VALUE;
        context.updateUser(stats -> stats.setNextMoveFixed(value).addCardPlaysThisTurn(1));
        context.reduceUserSkillCooldown(1);
        return 0;
    }

}