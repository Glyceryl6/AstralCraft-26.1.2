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

public class HandcardSoulLink extends BaseHandCard {
    
    public static final CardDefinition DEFINITION = CardDefinition.create("handcard_soul_link", CardType.EFFECT, CardTargetMode.TWO_PLAYERS, 32, false);

    public HandcardSoulLink(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        if (targets.size() < 2) return false;
        SoulLinkManager.link(targets.get(0), targets.get(1), user.level().getGameTime() + 20L * 60L, SoulLinkStyle.rainbow(2.2F, 0.16F));
        if (targets.get(0) instanceof ServerPlayer first) {
            AstralCardEffects.update(first, AstralStats.get(first).addBuff(BuffKinds.CUSTOM, 1));
        }
        if (targets.get(1) instanceof ServerPlayer second) {
            AstralCardEffects.update(second, AstralStats.get(second).addBuff(BuffKinds.CUSTOM, 1));
        }
        return true;
    }

}