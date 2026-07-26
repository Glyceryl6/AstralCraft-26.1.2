package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class MarkBoardBuff extends BoardBuff {

    public MarkBoardBuff(int color) {
        super(Properties.of(color).stacking().permanent().decayLevelsAtTurnEnd());
    }

    @Override
    public int incomingDamageModifier(BoardBuffInstance instance) {
        return 1;
    }

}