package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.OpenCardNumberSelectionPayload;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class HandcardSmartDice extends BaseHandCard {

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

    @Override
    public boolean applyNumberSelection(ServerPlayer user, ItemStack itemStack, int value) {
        if (value < MIN_DICE_VALUE || value > MAX_DICE_VALUE) return false;
        AstralCardEffects.update(user, AstralStats.get(user).setNextMoveFixed(value));
        return true;
    }

}