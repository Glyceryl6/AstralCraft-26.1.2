package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class StackingBoardBuff extends BoardBuff {

    protected final boolean permanentAfterMerge;
    protected final int maximumLevel;

    public StackingBoardBuff(int color, boolean permanentAfterMerge, int maximumLevel) {
        super(color);
        this.permanentAfterMerge = permanentAfterMerge;
        this.maximumLevel = maximumLevel;
    }

    @Override
    public BoardBuffInstance merge(BoardBuffInstance current, BoardBuffInstance incoming) {
        if (current == null) return incoming;
        int level = current.level() + incoming.level();
        if (this.maximumLevel > 0) level = ((level - 1) % this.maximumLevel) + 1;
        int duration = this.permanentAfterMerge || current.permanent() || incoming.permanent()
                ? BoardBuffInstance.PERMANENT : Math.max(current.duration(), incoming.duration());
        return new BoardBuffInstance(duration, level - 1);
    }
}
