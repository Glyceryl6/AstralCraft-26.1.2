package com.astral_craft.common.items.cards.battle;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardDragonRoar extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetTypes.PLAYERS_AND_MOBS, 32)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("mamushi")), true, true));

    public HandcardDragonRoar(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, AstralStats.get(user).addBuff(
                AstralBoardBuffs.instance(AstralBoardBuffs.DRAGON_ROAR_POWER_ID, AstralBoardBuffs.ATTACK.get())
                        .duration(1).value(3).build()));
        AstralCardEffects.target(targets).ifPresent(target ->
                AstralCardEffects.update(target, applyWeakness(AstralStats.getOrDefault(target))));
        return true;
    }

    private static AstralPlayerStats applyWeakness(AstralPlayerStats stats) {
        return stats.addBuff(AstralBoardBuffs.partInstance(AstralBoardBuffs.DRAGON_ROAR_WEAKNESS_ID, "defense", AstralBoardBuffs.DEFENSE.get()).duration(1).value(-3).build())
                .addBuff(AstralBoardBuffs.partInstance(AstralBoardBuffs.DRAGON_ROAR_WEAKNESS_ID, "speed", AstralBoardBuffs.SPEED.get()).duration(1).value(-9).build());
    }

}
