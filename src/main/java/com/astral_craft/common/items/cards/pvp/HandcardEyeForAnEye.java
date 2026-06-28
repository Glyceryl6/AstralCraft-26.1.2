package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.gameplay.handcard.CardTargetMode;
import com.astral_craft.common.gameplay.handcard.PendingCounterEffectManager;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HandcardEyeForAnEye extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.COUNTER, CardTargetMode.SELF, -1, false);

    public HandcardEyeForAnEye(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean used = PendingCounterEffectManager.respond(serverPlayer, PendingCounterEffectManager.CounterAction.EYE_FOR_AN_EYE);
        if (used) {
            consume(serverPlayer, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
