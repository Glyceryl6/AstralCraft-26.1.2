package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.AstralCardEffects;
import com.astral_craft.common.gameplay.AstralPartyCards;
import com.astral_craft.common.gameplay.CardDefinition;
import com.astral_craft.common.gameplay.CardTargetMode;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class HandcardLuxuriousFeast extends BaseHandCard {

    public static final CardDefinition DEFINITION = AstralPartyCards.register(CardDefinition.create("handcard_luxurious_feast", CardType.EFFECT, CardTargetMode.ALLY, 3, false));

    public HandcardLuxuriousFeast(Properties properties) {
        super(properties, DEFINITION);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        AstralCardEffects.targetPlayerOrSelf(user, targets).ifPresent(center -> AstralCardEffects.healPlayersAround(user.level(), center, 3, 3));
        return true;
    }

}