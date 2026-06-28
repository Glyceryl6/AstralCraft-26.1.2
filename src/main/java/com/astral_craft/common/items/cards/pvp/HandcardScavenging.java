package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.gameplay.handcard.CardTargetMode;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardScavenging extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetMode.PANEL, 3, false);

    public HandcardScavenging(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return false;
    }
}
