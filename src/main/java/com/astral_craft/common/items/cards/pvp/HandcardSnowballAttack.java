package com.astral_craft.common.items.cards.pvp;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardBotEffect;
import com.astral_craft.common.gameplay.board.BoardBotEffectContext;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardSnowballAttack extends BaseHandCard implements BoardBotEffect {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS_AND_MOBS, 9);

    public HandcardSnowballAttack(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return AstralCardEffects.target(targets).map(target -> AstralCardEffects.snowballAttackProjectile(user, target, 1)).orElse(false);
    }

    @Override
    public int applyByBoardBot(BoardBotEffectContext context) {
        return context.targetSlotIds().isEmpty() ? 0 : context.snowball(context.targetSlotIds().getFirst(), 1);
    }

    @Override
    public boolean waitForBoardDamageBeforeReopen() {
        return true;
    }

}
