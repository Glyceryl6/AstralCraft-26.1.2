package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.stats.AstralPlayerStats;

/** Stackable buff that loses a configurable number of levels at the end of each owner turn. */
public class DecayingStackingBoardBuff extends StackingBoardBuff {

    protected final int decayPerTurn;

    public DecayingStackingBoardBuff(int color, boolean permanentAfterMerge, int maximumLevel, int decayPerTurn) {
        super(color, permanentAfterMerge, maximumLevel);
        this.decayPerTurn = Math.max(1, decayPerTurn);
    }

    @Override
    public AstralPlayerStats onTurnEnd(AstralPlayerStats stats, BoardBuffInstance instance) {
        int nextLevel = instance.level() - this.decayPerTurn;
        return nextLevel <= 0 ? stats.removeBuff(this) : stats.setBuff(this, instance.duration(), nextLevel - 1);
    }
}
