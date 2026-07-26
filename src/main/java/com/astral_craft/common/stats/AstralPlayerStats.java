package com.astral_craft.common.stats;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.StatBundle;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.character.CharacterStatsDefinition;
import com.astral_craft.common.registry.AstralBoardBuffs;
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
        Map<BoardBuff, BoardBuffInstance> buffs,
        List<TimedStatModifier> modifiers) {

    public static final AstralPlayerStats DEFAULT = new AstralPlayerStats(
            1, 0, 0, 10, 10, 0, 0, 1, 1, 0, 0, 0, Map.of(), List.of());

    public static AstralPlayerStats initial(CharacterStatsDefinition definition) {
        CharacterStatsDefinition stats = definition == null ? CharacterStatsDefinition.defaultStats() : definition;
        int health = Math.max(1, stats.health());
        return new AstralPlayerStats(stats.attack(), stats.defense(), 0, health, health,
                Math.max(0, stats.initialStarCoins()), 0, 1, 1, 0, 0, 0, Map.of(), List.of());
    }

    private static final Codec<Map<BoardBuff, BoardBuffInstance>> BUFF_MAP_CODEC = Codec.unboundedMap(BoardBuff.CODEC, BoardBuffInstance.CODEC);
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
            BUFF_MAP_CODEC.optionalFieldOf("board_buffs", Map.of()).forGetter(AstralPlayerStats::buffs),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("buffs", Map.of()).forGetter(stats -> Map.of()),
            TimedStatModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(AstralPlayerStats::modifiers)
    ).apply(instance, AstralPlayerStats::fromCodec));

    public static final StreamCodec<ByteBuf, AstralPlayerStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private static AstralPlayerStats fromCodec(int baseAttack, int baseDefense, int baseSpeed, int maxHealth, int health,
                                               int starCoins, int stars, int cardPlaysPerTurn, int cardPlaysRemaining,
                                               int skillCooldownReduction, int nextMoveFixed, int nextMoveExtraDice,
                                               Map<BoardBuff, BoardBuffInstance> buffs, Map<String, Integer> legacyBuffs,
                                               List<TimedStatModifier> modifiers) {
        Map<BoardBuff, BoardBuffInstance> migrated = new HashMap<>(buffs);
        legacyBuffs.forEach((name, level) -> {
            BoardBuff buff = AstralBoardBuffs.REGISTRY.getValue(AstralCraft.prefix(name));
            if (buff != null && level > 0) migrated.putIfAbsent(buff, new BoardBuffInstance(BoardBuffInstance.PERMANENT, level - 1, 0, false));
        });
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice,
                migrated, modifiers);
    }

    public AstralPlayerStats {
        buffs = copyBuffs(buffs);
        modifiers = List.copyOf(modifiers);
        maxHealth = Math.max(1, maxHealth);
        health = Math.clamp(health, 0, maxHealth);
        cardPlaysPerTurn = Math.max(0, cardPlaysPerTurn);
        cardPlaysRemaining = Math.max(0, cardPlaysRemaining);
    }

    public int attack() {
        int value = this.baseAttack + this.modifierSum("attack");
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += entry.getKey().attackModifier(entry.getValue());
        return Math.max(0, value);
    }

    public int defense() {
        int value = this.baseDefense + this.modifierSum("defense");
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += entry.getKey().defenseModifier(entry.getValue());
        return Math.max(0, value);
    }

    public int speed() {
        int value = this.baseSpeed + this.modifierSum("speed");
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += entry.getKey().speedModifier(entry.getValue());
        return Math.max(0, value);
    }

    public int moveDiceBonus() {
        int value = 0;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) {
            value += entry.getKey().moveDiceModifier(entry.getValue());
        }
        return Math.max(0, value);
    }

    public int incomingDamageBonus() {
        int value = 0;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += entry.getKey().incomingDamageModifier(entry.getValue());
        return Math.max(0, value);
    }

    public int turnStartHealing() {
        int value = 0;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += Math.max(0, entry.getKey().turnStartHealing(entry.getValue()));
        return value;
    }

    public int turnStartDamage() {
        int value = 0;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += Math.max(0, entry.getKey().turnStartDamage(entry.getValue()));
        return value;
    }

    public int roundRewardBonus() {
        int value = 0;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) value += entry.getKey().roundRewardBonus(entry.getValue());
        return Math.max(0, value);
    }

    public int resolveIncomingDamage(int damage) {
        int reduction = 0;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : this.buffs.entrySet()) reduction += entry.getKey().damageReduction(entry.getValue());
        return Math.max(0, damage - reduction);
    }

    public AstralPlayerStats consumeMoveRollBuffs() {
        Map<BoardBuff, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            if (!entry.getKey().consumedAfterMoveRoll(entry.getValue())) continue;
            BoardBuffInstance retained = entry.getValue().withoutAcquiredLevels();
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public AstralPlayerStats consumeIncomingDamageBuffs() {
        Map<BoardBuff, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            if (!entry.getKey().consumedAfterIncomingDamage(entry.getValue())) continue;
            BoardBuffInstance retained = entry.getValue().withoutAcquiredLevels();
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public int buff(BoardBuff buff) {
        BoardBuffInstance instance = this.buffs.get(buff);
        return instance == null ? 0 : instance.level();
    }

    public BoardBuffInstance buffInstance(BoardBuff buff) {
        return this.buffs.get(buff);
    }

    public boolean hasBuff(BoardBuff buff) {
        return this.buffs.containsKey(buff);
    }

    public boolean anyBuffPreventsEvade() {
        return this.buffs.entrySet().stream().anyMatch(entry -> entry.getKey().preventsEvade(entry.getValue()));
    }

    public boolean anyBuffKnocksDownOwnerWhenAttackFails() {
        return this.buffs.entrySet().stream().anyMatch(entry -> entry.getKey().knocksDownOwnerWhenAttackFails(entry.getValue()));
    }

    public AstralPlayerStats heal(int amount) {
        return this.withHealth(this.health + Math.max(0, amount));
    }

    public AstralPlayerStats damage(int amount) {
        return this.withHealth(this.health - Math.max(0, amount));
    }

    public AstralPlayerStats addCoins(int amount) {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health,
                Math.max(0, this.starCoins + amount), this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.skillCooldownReduction, this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats spendCoins(int amount) {
        return this.addCoins(-Math.max(0, amount));
    }

    public AstralPlayerStats addStars(int amount) {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                Math.max(0, this.stars + amount), this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.skillCooldownReduction, this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addBaseAttack(int amount) {
        return this.copy(this.baseAttack + amount, this.baseDefense, this.baseSpeed, this.maxHealth, this.health,
                this.starCoins, this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.skillCooldownReduction, this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addTemporary(String stat, int amount, int turns) {
        List<TimedStatModifier> next = new ArrayList<>(this.modifiers);
        next.add(new TimedStatModifier(stat, amount, turns));
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.skillCooldownReduction,
                this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, next);
    }

    public AstralPlayerStats addBuff(BoardBuff buff, int duration, int amplifier) {
        return buff == null ? this : buff.apply(this, new BoardBuffInstance(duration, amplifier));
    }

    public AstralPlayerStats addPermanentBuff(BoardBuff buff, int levels) {
        return buff == null || levels <= 0 ? this
                : buff.apply(this, new BoardBuffInstance(BoardBuffInstance.PERMANENT, levels - 1));
    }

    public AstralPlayerStats addIntrinsicBuff(BoardBuff buff, int levels) {
        return buff == null || levels <= 0 ? this
                : buff.apply(this, new BoardBuffInstance(BoardBuffInstance.PERMANENT, levels - 1, true));
    }

    public AstralPlayerStats changeBuffLevel(BoardBuff buff, int amount) {
        if (buff == null || amount == 0) return this;
        BoardBuffInstance current = this.buffs.get(buff);
        if (current == null) return amount > 0 ? this.addPermanentBuff(buff, amount) : this;
        BoardBuffInstance next = current.withAcquiredLevels(current.acquiredLevels() + amount);
        return next == null ? this.removeBuff(buff) : this.setBuff(buff, next);
    }

    public AstralPlayerStats setBuff(BoardBuff buff, int duration, int amplifier) {
        return this.setBuff(buff, new BoardBuffInstance(duration, amplifier));
    }

    public AstralPlayerStats setBuff(BoardBuff buff, BoardBuffInstance instance) {
        if (buff == null || instance == null) return this;
        Map<BoardBuff, BoardBuffInstance> next = new HashMap<>(this.buffs);
        next.put(buff, instance);
        return this.withBuffs(next);
    }

    public AstralPlayerStats removeBuff(BoardBuff buff) {
        if (buff == null || !this.buffs.containsKey(buff)) return this;
        Map<BoardBuff, BoardBuffInstance> next = new HashMap<>(this.buffs);
        next.remove(buff);
        return this.withBuffs(next);
    }

    public AstralPlayerStats consumeAttackBuffs() {
        Map<BoardBuff, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            if (!entry.getKey().consumedAfterAttack(entry.getValue())) continue;
            BoardBuffInstance retained = entry.getValue().withoutAcquiredLevels();
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public AstralPlayerStats clearAcquiredBuffs() {
        Map<BoardBuff, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            BoardBuffInstance retained = entry.getKey().afterKnockout(entry.getValue());
            if (retained == entry.getValue()) continue;
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public AstralPlayerStats applyChip(StatBundle stats) {
        AstralPlayerStats next = this.copy(this.baseAttack + stats.attack(), this.baseDefense + stats.defense(),
                this.baseSpeed + stats.speed(), this.maxHealth + stats.maxHealth(), this.health + stats.health(),
                this.starCoins + stats.starCoins(), this.stars, this.cardPlaysPerTurn + stats.cardPlays(),
                this.cardPlaysRemaining + stats.cardPlays(), this.skillCooldownReduction + stats.skillCooldownReduction(),
                this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, this.modifiers);
        if (stats.healStacks() > 0) next = next.addPermanentBuff(AstralBoardBuffs.HEAL.get(), stats.healStacks());
        if (stats.starlightStacks() > 0) next = next.addPermanentBuff(AstralBoardBuffs.STARLIGHT.get(), stats.starlightStacks());
        if (stats.markStacks() > 0) next = next.addPermanentBuff(AstralBoardBuffs.MARK.get(), stats.markStacks());
        return next;
    }

    public AstralPlayerStats useCardPlay() {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, Math.max(0, this.cardPlaysRemaining - 1),
                this.skillCooldownReduction, this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addCardPlaysThisTurn(int amount) {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining + Math.max(0, amount),
                this.skillCooldownReduction, this.nextMoveFixed, this.nextMoveExtraDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats setNextMoveFixed(int value) {
        int safeValue = Math.max(0, value);
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.skillCooldownReduction, safeValue,
                safeValue > 0 ? 0 : this.nextMoveExtraDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addNextMoveDice(int dice) {
        int safeDice = Math.max(0, dice);
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.skillCooldownReduction,
                safeDice > 0 ? 0 : this.nextMoveFixed, this.nextMoveExtraDice + safeDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats setNextMoveExtraDice(int dice) {
        int safeDice = Math.max(0, dice);
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.skillCooldownReduction,
                safeDice > 0 ? 0 : this.nextMoveFixed, safeDice, this.buffs, this.modifiers);
    }

    public AstralPlayerStats clearNextMoveDiceEffects() {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.skillCooldownReduction,
                0, 0, this.buffs, this.modifiers);
    }

    public AstralPlayerStats beginTurn() {
        AstralPlayerStats next = this;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : new ArrayList<>(this.buffs.entrySet())) {
            BoardBuffInstance current = next.buffInstance(entry.getKey());
            if (current != null) next = entry.getKey().onTurnStart(next, current);
        }
        return next.copy(next.baseAttack, next.baseDefense, next.baseSpeed, next.maxHealth, next.health, next.starCoins,
                next.stars, next.cardPlaysPerTurn, next.cardPlaysPerTurn, next.skillCooldownReduction,
                next.nextMoveFixed, next.nextMoveExtraDice, next.buffs, next.tickModifiers());
    }

    public AstralPlayerStats endTurn() {
        AstralPlayerStats next = this;
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : new ArrayList<>(this.buffs.entrySet())) {
            BoardBuffInstance current = next.buffInstance(entry.getKey());
            if (current != null) next = entry.getKey().onTurnEnd(next, current);
        }
        Map<BoardBuff, BoardBuffInstance> ticked = new HashMap<>();
        for (Map.Entry<BoardBuff, BoardBuffInstance> entry : next.buffs.entrySet()) {
            BoardBuffInstance instance = entry.getValue();
            if (instance.acquiredLevels() <= 0 || instance.permanent()) ticked.put(entry.getKey(), instance.activate());
            else if (instance.fresh()) ticked.put(entry.getKey(), instance.activate());
            else if (instance.duration() > 1) ticked.put(entry.getKey(), instance.tickDown());
            else {
                BoardBuffInstance retained = instance.withoutAcquiredLevels();
                if (retained != null) ticked.put(entry.getKey(), retained);
            }
        }
        return next.withBuffs(ticked);
    }

    public AstralPlayerStats withHealth(int value) {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth,
                Math.clamp(value, 0, this.maxHealth), this.starCoins, this.stars, this.cardPlaysPerTurn,
                this.cardPlaysRemaining, this.skillCooldownReduction, this.nextMoveFixed, this.nextMoveExtraDice,
                this.buffs, this.modifiers);
    }

    private AstralPlayerStats withBuffs(Map<BoardBuff, BoardBuffInstance> buffs) {
        return this.copy(this.baseAttack, this.baseDefense, this.baseSpeed, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.skillCooldownReduction,
                this.nextMoveFixed, this.nextMoveExtraDice, buffs, this.modifiers);
    }

    private int modifierSum(String stat) {
        int value = 0;
        for (TimedStatModifier modifier : this.modifiers) if (modifier.active() && modifier.stat().equals(stat)) value += modifier.amount();
        return value;
    }

    private List<TimedStatModifier> tickModifiers() {
        List<TimedStatModifier> next = new ArrayList<>();
        for (TimedStatModifier modifier : this.modifiers) {
            TimedStatModifier ticked = modifier.turns() > 0 ? modifier.tickDown() : modifier;
            if (ticked.active()) next.add(ticked);
        }
        return next;
    }

    private AstralPlayerStats copy(int baseAttack, int baseDefense, int baseSpeed, int maxHealth, int health,
                                   int starCoins, int stars, int cardPlaysPerTurn, int cardPlaysRemaining,
                                   int skillCooldownReduction, int nextMoveFixed, int nextMoveExtraDice,
                                   Map<BoardBuff, BoardBuffInstance> buffs, List<TimedStatModifier> modifiers) {
        return new AstralPlayerStats(baseAttack, baseDefense, baseSpeed, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, skillCooldownReduction, nextMoveFixed, nextMoveExtraDice,
                buffs, modifiers);
    }

    private static Map<BoardBuff, BoardBuffInstance> copyBuffs(Map<BoardBuff, BoardBuffInstance> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<BoardBuff, BoardBuffInstance> result = new HashMap<>();
        input.forEach((buff, instance) -> {
            if (buff != null && instance != null) result.put(buff, instance);
        });
        return Map.copyOf(result);
    }
}
