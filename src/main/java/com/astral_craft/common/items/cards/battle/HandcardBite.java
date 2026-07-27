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

public class HandcardBite extends BaseHandCard {

    private static final Identifier AWAKENING_ID = AstralCraft.prefix("awakening");
    private static final Identifier AWAKENED_ATTACK_ID = AstralCraft.prefix("awakened_attack");

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetTypes.NONE, -1)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("mamushi")), Boolean.TRUE, Boolean.TRUE));

    public HandcardBite(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, applyBuffs(AstralStats.get(user)));
        return true;
    }

    private static AstralPlayerStats applyBuffs(AstralPlayerStats stats) {
        int awakening = stats.buff(AWAKENING_ID) + 1;
        return stats.addBuff(AstralBoardBuffs.state(AWAKENING_ID).permanent().build())
                .addBuff(AstralBoardBuffs.attack(AWAKENED_ATTACK_ID, Math.min(4, awakening)).duration(1).build());
    }

}
