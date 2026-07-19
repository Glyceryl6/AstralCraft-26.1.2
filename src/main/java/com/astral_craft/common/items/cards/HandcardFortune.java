package com.astral_craft.common.items.cards;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardFortune extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 32)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("zhao")), Boolean.TRUE, Boolean.TRUE));

    public HandcardFortune(Properties properties) {
        super(properties);
    }

    @Override
    public boolean allowsSelfTarget() {
        return true;
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        LivingEntity target = targets.isEmpty() ? user : targets.getFirst();
        AstralCardEffects.update(target, AstralStats.getOrDefault(target).heal(2));
        AstralCardEffects.update(user, AstralStats.get(user).addCardPlaysThisTurn(1));
        return true;
    }

}