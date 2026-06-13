package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.*;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardBerserk extends BaseHandCard {

    public static final CardDefinition DEFINITION = AstralPartyCards.register(CardDefinition.create("handcard_berserk", CardType.EFFECT, CardTargetMode.ANY_PLAYER, 10, false));

    public HandcardBerserk(Properties properties) {
        super(properties, DEFINITION);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.targetPlayer(targets).ifPresent(target -> AstralCardEffects.update(target, AstralStats.get(target).addTemporary("attack", 3, 2).addBuff(BuffKinds.BERSERK, 1)));
        return !targets.isEmpty();
    }

}