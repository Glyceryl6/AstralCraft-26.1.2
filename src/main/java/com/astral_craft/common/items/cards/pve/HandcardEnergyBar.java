package com.astral_craft.common.items.cards.pve;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardEnergyBar extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 32);

    public HandcardEnergyBar(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.targetPlayerOrSelf(user, targets).ifPresent(target -> AstralCardEffects.update(target, AstralStats.get(target).addBuff(AstralBoardBuffs.OVERCLOCK.get(), 2, 0)));
        return true;
    }
}
