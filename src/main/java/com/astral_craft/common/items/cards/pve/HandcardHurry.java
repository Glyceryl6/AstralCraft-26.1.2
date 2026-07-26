package com.astral_craft.common.items.cards.pve;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class HandcardHurry extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.EFFECT, CardTargetTypes.PLAYERS, 32);

    public HandcardHurry(Properties properties) {
        super(properties);
    }

    @Override
    public boolean allowsSelfTarget() {
        return true;
    }

    @Override
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return AstralCardEffects.target(targets).map(target -> {
            if (target instanceof AstralCharacterEntity character && character.isBoardPawn()) {
                AstralCardEffects.update(target, AstralStats.getOrDefault(target).addBuff(
                        AstralBoardBuffs.HASTE.get(), BoardBuffInstance.PERMANENT, 0));
            } else {
                AstralCardEffects.update(target, AstralStats.getOrDefault(target).setNextMoveExtraDice(1));
            }
            return true;
        }).orElse(false);
    }

}