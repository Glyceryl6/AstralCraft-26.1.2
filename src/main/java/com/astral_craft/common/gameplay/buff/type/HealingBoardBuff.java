package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class HealingBoardBuff extends BoardBuff {

    protected final int amount;

    public HealingBoardBuff(int color, int amount) {
        super(color);
        this.amount = amount;
    }

    @Override
    public int turnStartHealing(BoardBuffInstance instance) {
        return this.amount * instance.level();
    }
}
