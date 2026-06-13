package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.*;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardOverflowingFortune extends BaseHandCard {

    public static final CardDefinition DEFINITION = AstralPartyCards.register(CardDefinition.create("handcard_overflowing_fortune", CardType.EFFECT, CardTargetMode.SELF, -1, false));

    public HandcardOverflowingFortune(Properties properties) {
        super(properties, DEFINITION);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.update(user, AstralStats.get(user)
                .addCardPlaysThisTurn(8)
                .addCoins(888)
                .addBuff(BuffKinds.STARLIGHT, 88)
                .addBaseAttack(8));
        for (ServerPlayer player : user.server.getPlayerList().getPlayers()) {
            AstralCardEffects.update(player, AstralStats.get(player).heal(88));
        }
        return true;
    }

}