package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class AllOrNothingBoardBuff extends AttributeBoardBuff {

    public AllOrNothingBoardBuff(int color) {
        super(color, 5, 0, 0, 0);
    }

    @Override
    public boolean preventsEvade(BoardBuffInstance instance) {
        return true;
    }

    @Override
    public boolean knocksDownOwnerWhenAttackFails(BoardBuffInstance instance) {
        return true;
    }

    @Override
    public boolean consumedAfterAttack(BoardBuffInstance instance) {
        return true;
    }
}
