package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class HasteBoardBuff extends BoardBuff {

    public HasteBoardBuff(int color) {
        super(Properties.of(color).stacking().permanent().consumeAfterMoveRoll());
    }

    @Override
    public int moveDiceModifier(BoardBuffInstance instance) {
        return Math.max(0, instance.value() * instance.level());
    }

}