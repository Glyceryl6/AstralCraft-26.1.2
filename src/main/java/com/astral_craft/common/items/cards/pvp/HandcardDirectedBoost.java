package com.astral_craft.common.items.cards.pvp;

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

public class HandcardDirectedBoost extends BaseHandCard implements BoardBotEffect {

    private static final Identifier BUFF_ID = AstralCraft.prefix("directed_boost");

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);

    public HandcardDirectedBoost(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, applyBuff(AstralStats.get(user)));
        return true;
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        context.updateUser(HandcardDirectedBoost::applyBuff);
        return 0;
    }

    private static AstralPlayerStats applyBuff(AstralPlayerStats stats) {
        return stats.addBuff(AstralBoardBuffs.speed(BUFF_ID, 3).duration(1).build());
    }

}