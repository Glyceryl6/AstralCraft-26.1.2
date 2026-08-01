package com.astral_craft.common.gameplay.battle;

/**
 * Numeric contribution from combat cards. Keep cards independent: each card can create one of these
 * values and pass it to the battle service instead of hard-coding card ids in the battle system.
 */
public record BattleCardContribution(
        int attackMin,
        int attackMax,
        int defenseMin,
        int defenseMax,
        float finalAttackMultiplier,
        boolean defenderCannotEvade,
        boolean attackerCannotBeCountered
) {
    public static final BattleCardContribution NONE = new BattleCardContribution(0, 0, 0, 0, 1.0F, false, false);

    public static BattleCardContribution attack(int min, int max) {
        return new BattleCardContribution(min, max, 0, 0, 1.0F, false, false);
    }

    public static BattleCardContribution defense(int min, int max) {
        return new BattleCardContribution(0, 0, min, max, 1.0F, false, false);
    }

    public BattleCardContribution plus(BattleCardContribution other) {
        return new BattleCardContribution(
                this.attackMin + other.attackMin,
                this.attackMax + other.attackMax,
                this.defenseMin + other.defenseMin,
                this.defenseMax + other.defenseMax,
                Math.max(this.finalAttackMultiplier, other.finalAttackMultiplier),
                this.defenderCannotEvade || other.defenderCannotEvade,
                this.attackerCannotBeCountered || other.attackerCannotBeCountered
        );
    }
}
