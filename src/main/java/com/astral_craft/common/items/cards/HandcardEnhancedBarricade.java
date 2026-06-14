package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.CardDefinition;
import com.astral_craft.common.gameplay.CardTargetMode;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardEnhancedBarricade extends BaseHandCard {
    
    public static final CardDefinition DEFINITION = CardDefinition.create("handcard_enhanced_barricade", CardType.EFFECT, CardTargetMode.PANEL, 10, false);

    public HandcardEnhancedBarricade(Properties properties) {
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