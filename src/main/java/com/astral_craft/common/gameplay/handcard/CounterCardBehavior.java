package com.astral_craft.common.gameplay.handcard;

/** Card-owned behavior for world and board counter resolution. */
public interface CounterCardBehavior {

    void resolveWorldCounter(PendingCounterEffectManager.WorldCounterContext context);

    void resolveBoardCounter(PendingCounterEffectManager.BoardCounterContext context);
}
