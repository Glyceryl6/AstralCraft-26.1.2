package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardBerserk extends BaseHandCard implements BoardBotEffect {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 10);

    public HandcardBerserk(Properties properties) {
        super(properties);
    }

    @Override
    public boolean allowsSelfTarget() {
        return true;
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.target(targets).ifPresent(target -> AstralCardEffects.update(target, AstralStats.getOrDefault(target).addTemporary("attack", 3, 2).addBuff(BuffKinds.BERSERK, 1)));
        return !targets.isEmpty();
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        context.firstTarget().ifPresent(target -> context.updateTarget(target.slotUuid(),
                stats -> stats.addTemporary("attack", 3, 2).addBuff(BuffKinds.BERSERK, 1)));
        return 0;
    }

}
