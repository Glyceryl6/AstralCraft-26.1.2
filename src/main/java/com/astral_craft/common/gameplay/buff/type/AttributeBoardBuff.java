package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class AttributeBoardBuff extends BoardBuff {

    private final Attribute attribute;

    public AttributeBoardBuff(int color, Attribute attribute) {
        super(Properties.of(color));
        this.attribute = attribute;
    }

    @Override
    public int attackModifier(BoardBuffInstance instance) {
        return this.attribute == Attribute.ATTACK ? instance.value() * instance.level() : 0;
    }

    @Override
    public int defenseModifier(BoardBuffInstance instance) {
        return this.attribute == Attribute.DEFENSE ? instance.value() * instance.level() : 0;
    }

    @Override
    public int speedModifier(BoardBuffInstance instance) {
        return this.attribute == Attribute.SPEED ? instance.value() * instance.level() : 0;
    }

    @Override
    public int incomingDamageModifier(BoardBuffInstance instance) {
        return this.attribute == Attribute.INCOMING_DAMAGE ? instance.value() * instance.level() : 0;
    }

    public enum Attribute {
        ATTACK,
        DEFENSE,
        INCOMING_DAMAGE,
        SPEED
    }

}