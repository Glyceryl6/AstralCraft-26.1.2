package com.astral_craft.common.items.cards;

import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
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

public class HandcardSmartDice extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, 6);
    public static final int MIN_DICE_VALUE = 1;
    public static final int MAX_DICE_VALUE = 6;

    public HandcardSmartDice(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onRevealFinished(ServerPlayer user, InteractionHand hand, ItemStack itemStack, CardDefinition definition) {
        PendingCardActionManager.beginNumberSelection(user, itemStack, MIN_DICE_VALUE, MAX_DICE_VALUE);
        PacketDistributor.sendToPlayer(user, new OpenCardNumberSelectionPayload(
                itemStack.copyWithCount(1), MIN_DICE_VALUE, MAX_DICE_VALUE));
        return true;
    }

    public static void applyNumberSelection(ServerPlayer user, CardNumberSelectionPayload payload) {
        PendingCardActionManager.PendingNumberSelection selection = PendingCardActionManager.consumeNumberSelection(
                user, payload.cardStack(), payload.value());
        if (selection == null || !(selection.cardStack().getItem() instanceof HandcardSmartDice)) return;
        AstralCardEffects.update(user, AstralStats.get(user).setNextMoveFixed(payload.value()));
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        context.updateUser(stats -> stats.setNextMoveFixed(
                context.level().getRandom().nextInt(MAX_DICE_VALUE - MIN_DICE_VALUE + 1) + MIN_DICE_VALUE));
        return 0;
    }

}
