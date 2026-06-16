package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.AstralCardEffects;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.gameplay.CardTargetMode;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardBite extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetMode.NONE, -1, false);

    public HandcardBite(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        int awakening = AstralStats.get(user).buff(BuffKinds.AWAKENING) + 1;
        AstralCardEffects.update(user, AstralStats.get(user).addBuff(BuffKinds.AWAKENING, 1).addTemporary("attack", Math.min(4, awakening), 1));
        return true;
    }
}
