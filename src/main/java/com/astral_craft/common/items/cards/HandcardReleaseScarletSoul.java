package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.AstralCardEffects;
import com.astral_craft.common.gameplay.CardDefinition;
import com.astral_craft.common.gameplay.CardTargetMode;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardReleaseScarletSoul extends BaseHandCard {
    
    public static final CardDefinition DEFINITION = CardDefinition.create("handcard_release_scarlet_soul", CardType.EFFECT, CardTargetMode.MONSTER, 6, false);

    public HandcardReleaseScarletSoul(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, AstralStats.get(user).damage(1));
        AstralCardEffects.damage(user, targets, 3);
        return true;
    }
}
