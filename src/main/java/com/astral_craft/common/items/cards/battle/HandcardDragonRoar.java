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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardDragonRoar extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetTypes.PLAYERS_AND_MOBS, 32)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("mamushi")), Boolean.TRUE, Boolean.TRUE));

    public HandcardDragonRoar(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, AstralStats.get(user).addBuff(
                AstralBoardBuffs.attack(AstralCraft.prefix("dragon_roar_power"), 3).duration(1).build()));
        AstralCardEffects.target(targets).ifPresent(target ->
                AstralCardEffects.update(target, applyWeakness(AstralStats.getOrDefault(target))));
        return true;
    }

    private static AstralPlayerStats applyWeakness(AstralPlayerStats stats) {
        Identifier id = AstralCraft.prefix("dragon_roar_weakness");
        return stats.addBuff(AstralBoardBuffs.defense(id, -3).duration(1).build())
                .addBuff(AstralBoardBuffs.speed(id, -9).duration(1).build());
    }

}