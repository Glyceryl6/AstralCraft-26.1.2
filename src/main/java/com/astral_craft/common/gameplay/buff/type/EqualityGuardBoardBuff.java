package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class EqualityGuardBoardBuff extends AttributeBoardBuff {

    public EqualityGuardBoardBuff(int color) {
        super(color, Attribute.INCOMING_DAMAGE);
    }

    @Override
    public int incomingDamageModifier(BoardBuffInstance instance) {
        return -10;
    }

    @Override
    public boolean consumedAfterIncomingDamage(BoardBuffInstance instance) {
        return true;
    }

}