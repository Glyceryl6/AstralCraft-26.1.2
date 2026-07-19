package com.astral_craft.common.items.cards.battle;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;

public class HandcardAttackM extends BaseHandCard {
    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetTypes.NONE, -1).withCombatCost(1);

    public HandcardAttackM(Properties properties) {
        super(properties);
    }

}