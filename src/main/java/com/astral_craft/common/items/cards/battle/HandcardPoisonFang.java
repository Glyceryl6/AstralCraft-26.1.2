package com.astral_craft.common.items.cards.battle;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CardUseRestriction;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.astral_craft.common.items.BaseHandCard;

import java.util.List;

public class HandcardPoisonFang extends BaseHandCard {

    public static final CardDefinition DEFINITION = CardDefinition.create(CardType.ATTACK, CardTargetTypes.NONE, -1)
            .withRestrictions(new CardUseRestriction(List.of(AstralCraft.prefix("mamushi")), Boolean.TRUE, Boolean.TRUE));

    public HandcardPoisonFang(Properties properties) {
        super(properties);
    }

}