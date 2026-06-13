package com.astral_craft.common.gameplay.battle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * A lightweight world-space battle window. It does not freeze or teleport the entities; instead it
 * records a short engagement, lets both sides keep moving, and resolves when the defender chooses a
 * response or the timeout is reached.
 */
public final class BattleSession {

    private final UUID id;
    private final int attackerId;
    private final int defenderId;
    private final long startedGameTime;
    private final int timeoutTicks;
    private final double breakDistance;
    private BattleCardContribution attackerCards = BattleCardContribution.NONE;
    private BattleCardContribution defenderCards = BattleCardContribution.NONE;

    BattleSession(UUID id, LivingEntity attacker, LivingEntity defender, long startedGameTime, int timeoutTicks, double breakDistance) {
        this.id = id;
        this.attackerId = attacker.getId();
        this.defenderId = defender.getId();
        this.startedGameTime = startedGameTime;
        this.timeoutTicks = timeoutTicks;
        this.breakDistance = breakDistance;
    }

    public UUID id() {
        return this.id;
    }

    public int attackerId() {
        return this.attackerId;
    }

    public int defenderId() {
        return this.defenderId;
    }

    public void addAttackerCard(BattleCardContribution contribution) {
        this.attackerCards = this.attackerCards.plus(contribution);
    }

    public void addDefenderCard(BattleCardContribution contribution) {
        this.defenderCards = this.defenderCards.plus(contribution);
    }

    public boolean expired(ServerLevel level) {
        return level.getGameTime() - this.startedGameTime > this.timeoutTicks;
    }

    public boolean stillValid(ServerLevel level) {
        LivingEntity attacker = entity(level, this.attackerId);
        LivingEntity defender = entity(level, this.defenderId);
        if (attacker == null || defender == null || !attacker.isAlive() || !defender.isAlive()) {
            return false;
        }
        return attacker.distanceToSqr(defender) <= this.breakDistance * this.breakDistance;
    }

    public BattleResolution resolve(ServerLevel level, BattleAction defenderAction) {
        LivingEntity attacker = entity(level, this.attackerId);
        LivingEntity defender = entity(level, this.defenderId);
        if (attacker == null || defender == null) {
            return new BattleResolution(1, 1, false, 0, 0, 0, false);
        }

        return resolve(attacker, defender, defenderAction, level.getRandom());
    }

    private BattleResolution resolve(LivingEntity attacker, LivingEntity defender, BattleAction defenderAction, RandomSource random) {
        BattleParticipantSnapshot atk = BattleParticipantSnapshot.of(attacker);
        BattleParticipantSnapshot def = BattleParticipantSnapshot.of(defender);
        int initialDie = random.nextInt(6) + 1;
        boolean forcedDefend = this.attackerCards.defenderCannotEvade();
        int evadeDie = 0;
        boolean evaded = false;
        if (defenderAction == BattleAction.EVADE && !forcedDefend) {
            evadeDie = random.nextInt(6) + 1;
            evaded = initialDie < 6 ? evadeDie > initialDie : evadeDie == 6;
        }
        int attackBonus = rollInclusive(random, this.attackerCards.attackMin(), this.attackerCards.attackMax());
        int defenseBonus = defenderAction == BattleAction.DEFEND ? rollInclusive(random, this.defenderCards.defenseMin(), this.defenderCards.defenseMax()) : 0;
        int attackerFinal = Math.round((initialDie + atk.attack() + attackBonus) * this.attackerCards.finalAttackMultiplier());
        int defenderFinal = evaded ? Integer.MAX_VALUE / 4 : def.defense() + defenseBonus;
        int damage = evaded ? 0 : Math.max(1, attackerFinal - defenderFinal);
        return new BattleResolution(initialDie, evadeDie, evaded, attackerFinal, defenderFinal, damage, forcedDefend);
    }

    private static int rollInclusive(RandomSource random, int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        if (high <= low) {
            return low;
        }
        return low + random.nextInt(high - low + 1);
    }

    private static LivingEntity entity(ServerLevel level, int id) {
        return level.getEntity(id) instanceof LivingEntity living ? living : null;
    }
}
