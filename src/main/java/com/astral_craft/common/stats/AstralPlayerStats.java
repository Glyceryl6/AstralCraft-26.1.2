package com.astral_craft.common.stats;

import com.astral_craft.common.gameplay.BuffKind;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.StatBundle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AstralPlayerStats(
        int baseAttack,
        int baseDefense,
        int baseSpeed,
        int maxHealth,
        int health,
        int starCoins,
        int stars,
        int cardPlaysPerTurn,
        int cardPlaysRemaining,
        int skillCooldownReduction,
        int nextMoveFixed,
        int nextMoveExtraDice,
        Map<BuffKind, Integer> buffs,
        List<TimedStatModifier> modifiers) {

    public static final AstralPlayerStats DEFAULT = new AstralPlayerStats(
            1, 0, 0, 10, 10, 0, 0, 1, 1, 0, 0, 0, Map.of(), List.of());

    private static final Codec<Map<BuffKind, Integer>> BUFF_MAP_CODEC = Codec.unboundedMap(BuffKind.CODEC, Codec.INT);
    public static final Codec<AstralPlayerStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("base_attack", 1).forGetter(AstralPlayerStats::baseAttack),
            Codec.INT.optionalFieldOf("base_defense", 0).forGetter(AstralPlayerStats::baseDefense),
            Codec.INT.optionalFieldOf("base_speed", 0).forGetter(AstralPlayerStats::baseSpeed),
            Codec.INT.optionalFieldOf("max_health", 10).forGetter(AstralPlayerStats::maxHealth),
            Codec.INT.optionalFieldOf("health", 10).forGetter(AstralPlayerStats::health),
            Codec.INT.optionalFieldOf("star_coins", 0).forGetter(AstralPlayerStats::starCoins),
            Codec.INT.optionalFieldOf("stars", 0).forGetter(AstralPlayerStats::stars),
            Codec.INT.optionalFieldOf("card_plays_per_turn", 1).forGetter(AstralPlayerStats::cardPlaysPerTurn),
            Codec.INT.optionalFieldOf("card_plays_remaining", 1).forGetter(AstralPlayerStats::cardPlaysRemaining),
            Codec.INT.optionalFieldOf("skill_cooldown_reduction", 0).forGetter(AstralPlayerStats::skillCooldownReduction),
            Codec.INT.optionalFieldOf("next_move_fixed", 0).forGetter(AstralPlayerStats::nextMoveFixed),
            Codec.INT.optionalFieldOf("next_move_extra_dice", 0).forGetter(AstralPlayerStats::nextMoveExtraDice),
            BUFF_MAP_CODEC.optionalFieldOf("buffs", Map.of()).forGetter(AstralPlayerStats::buffs),
            TimedStatModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(AstralPlayerStats::modifiers)
    ).apply(instance, AstralPlayerStats::new));

    public static final StreamCodec<ByteBuf, AstralPlayerStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public AstralPlayerStats {
        buffs = copyBuffs(buffs);
        modifiers = List.copyOf(modifiers);
        maxHealth = Math.max(1, maxHealth);
        health = Math.clamp(health, 0, maxHealth);
        cardPlaysPerTurn = Math.max(0, cardPlaysPerTurn);
        cardPlaysRemaining = Math.max(0, cardPlaysRemaining);
    }

    public int attack() {
        return this.baseAttack + modifierSum("attack");
    }

    public int defense() {
        return this.baseDefense + modifierSum("defense");
    }

    public int speed() {
        return this.baseSpeed + modifierSum("speed");
    }

    public int incomingDamageBonus() {
        return Math.max(0, buff(BuffKinds.BERSERK));
    }

    public int buff(BuffKind kind) {
        return this.buffs.getOrDefault(kind, 0);
    }

    public AstralPlayerStats heal(int amount) {
        return withHealth(this.health + Math.max(0, amount));
    }

    public AstralPlayerStats damage(int amount) {
        return withHealth(this.health - Math.max(0, amount));
    }

    public AstralPlayerStats addCoins(int amount) {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, Math.max(0, starCoins + amount), stars, cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats spendCoins(int amount) {
        return addCoins(-Math.max(0, amount));
    }

    public AstralPlayerStats addStars(int amount) {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, Math.max(0, stars + amount), cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats addBaseAttack(int amount) {
        return new AstralPlayerStats(baseAttack + amount, baseDefense, baseSpeed, maxHealth, health, starCoins, stars, cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats addTemporary(String stat, int amount, int turns) {
        List<TimedStatModifier> next = new ArrayList<>(this.modifiers);
        next.add(new TimedStatModifier(stat, amount, turns));
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars, cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, next);
    }

    public AstralPlayerStats addBuff(BuffKind kind, int amount) {
        Map<BuffKind, Integer> next = new HashMap<>(this.buffs);
        int value = Math.max(0, next.getOrDefault(kind, 0) + amount);
        if (value == 0) next.remove(kind); else next.put(kind, value);
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars, cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, next, modifiers);
    }

    public AstralPlayerStats applyChip(StatBundle stats) {
        AstralPlayerStats next = new AstralPlayerStats(
                baseAttack + stats.attack(),
                baseDefense + stats.defense(),
                baseSpeed + stats.speed(),
                maxHealth + stats.maxHealth(),
                health + stats.health(),
                starCoins + stats.starCoins(),
                stars,
                cardPlaysPerTurn + stats.cardPlays(),
                cardPlaysRemaining + stats.cardPlays(),
                skillCooldownReduction + stats.skillCooldownReduction(),
                nextMoveFixed,
                nextMoveExtraDice,
                buffs,
                modifiers);
        next = next.addBuff(BuffKinds.HEAL, stats.healStacks());
        next = next.addBuff(BuffKinds.STARLIGHT, stats.starlightStacks());
        return next.addBuff(BuffKinds.MARK, stats.markStacks());
    }

    public AstralPlayerStats useCardPlay() {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars, cardPlaysPerTurn, Math.max(0, cardPlaysRemaining - 1), skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats addCardPlaysThisTurn(int amount) {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars, cardPlaysPerTurn, cardPlaysRemaining + Math.max(0, amount), skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats setNextMoveFixed(int value) {
        int safeValue = Math.max(0, value);
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, safeValue,
                safeValue > 0 ? 0 : nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats addNextMoveDice(int dice) {
        int safeDice = Math.max(0, dice);
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction,
                safeDice > 0 ? 0 : nextMoveFixed, nextMoveExtraDice + safeDice, buffs, modifiers);
    }

    public AstralPlayerStats setNextMoveExtraDice(int dice) {
        int safeDice = Math.max(0, dice);
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction,
                safeDice > 0 ? 0 : nextMoveFixed, safeDice, buffs, modifiers);
    }

    public AstralPlayerStats clearNextMoveDiceEffects() {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, 0, 0, buffs, modifiers);
    }

    public AstralPlayerStats beginTurn() {
        AstralPlayerStats next = this.heal(buff(BuffKinds.HEAL));
        return new AstralPlayerStats(next.baseAttack, next.baseDefense, next.baseSpeed, next.maxHealth, next.health, next.starCoins, next.stars, next.cardPlaysPerTurn, next.cardPlaysPerTurn, next.skillCooldownReduction, next.nextMoveFixed, next.nextMoveExtraDice, next.buffs, next.tickModifiers());
    }

    public AstralPlayerStats endTurn() {
        AstralPlayerStats next = this;
        int heal = next.buff(BuffKinds.HEAL);
        if (heal > 0) {
            next = next.setBuff(BuffKinds.HEAL, (heal + 1) / 2);
        }
        int mark = next.buff(BuffKinds.MARK);
        if (mark > 0) {
            next = next.setBuff(BuffKinds.MARK, mark - 1);
        }
        return next;
    }

    public AstralPlayerStats withHealth(int value) {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, Math.clamp(value, 0, maxHealth), starCoins, stars, cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, buffs, modifiers);
    }

    public AstralPlayerStats setBuff(BuffKind kind, int value) {
        Map<BuffKind, Integer> next = new HashMap<>(this.buffs);
        if (value <= 0) next.remove(kind); else next.put(kind, value);
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars, cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice, next, modifiers);
    }

    private int modifierSum(String stat) {
        int value = 0;
        for (TimedStatModifier modifier : this.modifiers) {
            if (modifier.active() && modifier.stat().equals(stat)) {
                value += modifier.amount();
            }
        }
        return value;
    }

    private List<TimedStatModifier> tickModifiers() {
        List<TimedStatModifier> next = new ArrayList<>();
        for (TimedStatModifier modifier : this.modifiers) {
            TimedStatModifier ticked = modifier.turns() > 0 ? modifier.tickDown() : modifier;
            if (ticked.active()) {
                next.add(ticked);
            }
        }
        return next;
    }

    private static Map<BuffKind, Integer> copyBuffs(Map<BuffKind, Integer> input) {
        if (input.isEmpty()) {
            return Map.of();
        }
        Map<BuffKind, Integer> result = new HashMap<>();
        input.forEach((kind, value) -> {
            if (value != null && value > 0) {
                result.put(kind, value);
            }
        });
        return Map.copyOf(result);
    }

}
