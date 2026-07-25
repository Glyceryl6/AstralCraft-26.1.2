package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class AttributeBoardBuff extends BoardBuff {

    protected final int attack;
    protected final int defense;
    protected final int speed;
    protected final int incomingDamage;

    public AttributeBoardBuff(int color, int attack, int defense, int speed, int incomingDamage) {
        super(color);
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.incomingDamage = incomingDamage;
    }

    @Override
    public int modifyAttack(int value, BoardBuffInstance instance) {
        return value + this.attack * instance.level();
    }

    @Override
    public int modifyDefense(int value, BoardBuffInstance instance) {
        return value + this.defense * instance.level();
    }

    @Override
    public int modifySpeed(int value, BoardBuffInstance instance) {
        return value + this.speed * instance.level();
    }

    @Override
    public int modifyIncomingDamage(int value, BoardBuffInstance instance) {
        return value + this.incomingDamage * instance.level();
    }
}
