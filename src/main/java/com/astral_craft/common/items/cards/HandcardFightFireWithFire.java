package com.astral_craft.common.items.cards;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
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

public class HandcardFightFireWithFire extends BaseHandCard implements BoardBotEffect {

    private static final Identifier BUFF_ID = AstralCraft.prefix("fight_fire_with_fire");
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);

    public HandcardFightFireWithFire(Properties properties) {
        super(properties);
    }

    @Override
    public boolean allowsSelfTarget() {
        return true;
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, applyBuff(AstralStats.get(user).damage(2)));
        return true;
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        context.damageUser(2);
        context.updateUser(HandcardFightFireWithFire::applyBuff);
        return 0;
    }

    private static AstralPlayerStats applyBuff(AstralPlayerStats stats) {
        return stats.addBuff(AstralBoardBuffs.turnStartHeal(BUFF_ID, 3).duration(2).build());
    }

}