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

public class HandcardOverflowingFortune extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.NONE, -1);

    public HandcardOverflowingFortune(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, AstralStats.get(user).addCardPlaysThisTurn(8).addCoins(888).addPermanentBuff(AstralBoardBuffs.STARLIGHT.get(), 88).addBaseAttack(8));
        for (ServerPlayer player : user.server.getPlayerList().getPlayers()) {
            AstralCardEffects.update(player, AstralStats.get(player).heal(88));
        }
        return true;
    }
}
