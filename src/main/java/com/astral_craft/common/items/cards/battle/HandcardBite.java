package com.astral_craft.common.items.cards.battle;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardBite extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetTypes.NONE, -1)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("mamushi")), Boolean.TRUE, Boolean.TRUE));

    public HandcardBite(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        int awakening = AstralStats.get(user).buff(BuffKinds.AWAKENING) + 1;
        AstralCardEffects.update(user, AstralStats.get(user)
                .addBuff(BuffKinds.AWAKENING, 1)
                .addTemporary("attack", Math.min(4, awakening), 1));
        return true;
    }

}