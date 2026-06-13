package com.astral_craft.common.gameplay.battle;

public record BattleResolution(
        int initialDie,
        int evadeDie,
        boolean evaded,
        int attackerFinal,
        int defenderFinal,
        int damage,
        boolean forcedDefend
) {}
