package com.astral_craft.common.items.cards;

import com.astral_craft.common.components.CardType;
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

public class HandcardEyeForAnEye extends BaseHandCard {
    public static final CardDefinition DEFINITION = AstralPartyCards.register(CardDefinition.create("handcard_eye_for_an_eye", CardType.COUNTER, CardTargetMode.SELF, -1, false));

    public HandcardEyeForAnEye(Properties properties) {
        super(properties, DEFINITION);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return false;
    }
}
