package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.gameplay.handcard.CounterCardBehavior;
import com.astral_craft.common.gameplay.handcard.PendingCounterEffectManager;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HandcardRandomSelect extends BaseHandCard implements CounterCardBehavior {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.COUNTER, CardTargetTypes.NONE, -1);

    public HandcardRandomSelect(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean used = PendingCounterEffectManager.respond(serverPlayer, this);
        if (used) {
            consume(serverPlayer, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void resolveWorldCounter(PendingCounterEffectManager.WorldCounterContext context) {
        LivingEntity redirected = context.randomTarget();
        if (redirected == null) {
            context.responder().sendSystemMessage(Component.translatable("message.astral_craft.counter.random_failed")
                    .withStyle(ChatFormatting.YELLOW), true);
            context.block();
            return;
        }
        context.responder().sendSystemMessage(Component.translatable("message.astral_craft.counter.random",
                redirected.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        context.redirect(redirected);
    }

    @Override
    public void resolveBoardCounter(PendingCounterEffectManager.BoardCounterContext context) {
        context.redirectRandomly();
    }

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
