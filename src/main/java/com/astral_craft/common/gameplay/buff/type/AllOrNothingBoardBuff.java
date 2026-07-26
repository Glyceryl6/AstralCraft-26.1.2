package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class AllOrNothingBoardBuff extends BoardBuff {

    public AllOrNothingBoardBuff(int color) {
        super(color);
    }

    @Override
    public int attackModifier(BoardBuffInstance instance) {
        return 5 * instance.level();
    }

    @Override
    public boolean preventsEvade() {
        return true;
    }

    @Override
    public boolean knocksDownOwnerWhenAttackFails() {
        return true;
    }

    @Override
    public boolean consumedAfterAttack() {
        return true;
    }
}
