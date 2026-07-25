package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.stats.AstralPlayerStats;

/** Healing stack whose level is halved, rounding up, after each owner turn. */
public class HalvingHealingBoardBuff extends HealingBoardBuff {

    public HalvingHealingBoardBuff(int color, int amount) {
        super(color, amount);
    }

    @Override
    public AstralPlayerStats onTurnEnd(AstralPlayerStats stats, BoardBuffInstance instance) {
        int nextLevel = (instance.level() + 1) / 2;
        return stats.setBuff(this, instance.duration(), nextLevel - 1);
    }
}
