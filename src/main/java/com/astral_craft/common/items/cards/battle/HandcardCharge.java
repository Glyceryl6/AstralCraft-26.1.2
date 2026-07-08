package com.astral_craft.common.items.cards.battle;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.gameplay.handcard.CardTargetMode;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardCharge extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetMode.NONE, -1);

    public HandcardCharge(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, AstralStats.get(user).addTemporary("attack", 5, 1));
        user.addItem(new ItemStack(AstralItems.HANDCARD_POWERFUL_ATTACK.get()));
        return true;
    }
}
