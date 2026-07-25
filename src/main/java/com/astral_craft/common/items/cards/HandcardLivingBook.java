package com.astral_craft.common.items.cards;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardLivingBook extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 5).withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("rin")), true, true));

    public HandcardLivingBook(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.damage(user, targets, 2);
        AstralCardEffects.target(targets).ifPresent(target -> AstralCardEffects.update(target, AstralStats.getOrDefault(target).addPermanentBuff(AstralBoardBuffs.MARK.get(), 1)));
        return !targets.isEmpty();
    }

    @Override
    public boolean waitForBoardDamageBeforeReopen() {
        return true;
    }

}