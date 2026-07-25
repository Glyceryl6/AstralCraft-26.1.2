package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

/** Attribute buff whose incoming levels are added instead of replacing the existing level. */
public class StackingAttributeBoardBuff extends AttributeBoardBuff {

    protected final boolean permanentAfterMerge;
    protected final int maximumLevel;

    public StackingAttributeBoardBuff(int color, int attack, int defense, int speed, int incomingDamage) {
        this(color, attack, defense, speed, incomingDamage, false, 0);
    }

    public StackingAttributeBoardBuff(int color, int attack, int defense, int speed, int incomingDamage,
                                      boolean permanentAfterMerge, int maximumLevel) {
        super(color, attack, defense, speed, incomingDamage);
        this.permanentAfterMerge = permanentAfterMerge;
        this.maximumLevel = Math.max(0, maximumLevel);
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
