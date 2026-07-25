package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class DamageShieldBoardBuff extends BoardBuff {

    protected final int reduction;

    public DamageShieldBoardBuff(int color, int reduction) {
        super(color);
        this.reduction = Math.max(0, reduction);
    }

    @Override
    public int resolveIncomingDamage(int damage, BoardBuffInstance instance) {
        return Math.max(0, damage - this.reduction * instance.level());
    }

    @Override
    public boolean consumedAfterIncomingDamage(BoardBuffInstance instance) {
        return true;
    }
}
