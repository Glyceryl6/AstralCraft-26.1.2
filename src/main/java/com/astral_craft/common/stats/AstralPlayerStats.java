package com.astral_craft.common.stats;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.character.CharacterStatsDefinition;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AstralPlayerStats(
        int baseAttack,
        int baseDefense,
        int maxHealth,
        int health,
        int starCoins,
        int stars,
        int cardPlaysPerTurn,
        int cardPlaysRemaining,
        int nextMoveFixed,
        Map<Identifier, BoardBuffInstance> buffs,
        List<TimedStatModifier> modifiers) {

    public static final AstralPlayerStats DEFAULT = new AstralPlayerStats(
            1, 0, 10, 10, 0, 0, 1, 1, 0, Map.of(), List.of());

    public static AstralPlayerStats initial(CharacterStatsDefinition definition) {
        CharacterStatsDefinition stats = definition == null ? CharacterStatsDefinition.defaultStats() : definition;
        int health = Math.max(1, stats.health());
        return new AstralPlayerStats(stats.attack(), stats.defense(), health, health,
                Math.max(0, stats.initialStarCoins()), 0, 1, 1, 0, Map.of(), List.of());
    }

    private static final Codec<Map<Identifier, BoardBuffInstance>> BUFF_MAP_CODEC =
            Codec.unboundedMap(Identifier.CODEC, BoardBuffInstance.CODEC);
    public static final Codec<AstralPlayerStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("base_attack", 1).forGetter(AstralPlayerStats::baseAttack),
            Codec.INT.optionalFieldOf("base_defense", 0).forGetter(AstralPlayerStats::baseDefense),
            Codec.INT.optionalFieldOf("max_health", 10).forGetter(AstralPlayerStats::maxHealth),
            Codec.INT.optionalFieldOf("health", 10).forGetter(AstralPlayerStats::health),
            Codec.INT.optionalFieldOf("star_coins", 0).forGetter(AstralPlayerStats::starCoins),
            Codec.INT.optionalFieldOf("stars", 0).forGetter(AstralPlayerStats::stars),
            Codec.INT.optionalFieldOf("card_plays_per_turn", 1).forGetter(AstralPlayerStats::cardPlaysPerTurn),
            Codec.INT.optionalFieldOf("card_plays_remaining", 1).forGetter(AstralPlayerStats::cardPlaysRemaining),
            Codec.INT.optionalFieldOf("next_move_fixed", 0).forGetter(AstralPlayerStats::nextMoveFixed),
            BUFF_MAP_CODEC.optionalFieldOf("buff_instances", Map.of()).forGetter(AstralPlayerStats::buffs),
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
        int value = this.baseAttack + this.modifierSum("attack");
        for (BoardBuffInstance instance : this.buffs.values()) value += instance.buff().attackModifier(instance);
        return Math.max(0, value);
    }

    public int defense() {
        int value = this.baseDefense + this.modifierSum("defense");
        for (BoardBuffInstance instance : this.buffs.values()) value += instance.buff().defenseModifier(instance);
        return Math.max(0, value);
    }

    public int speed() {
        int value = this.modifierSum("speed");
        for (BoardBuffInstance instance : this.buffs.values()) value += instance.buff().speedModifier(instance);
        return Math.max(0, value);
    }

    public int moveDiceBonus() {
        int value = 0;
        for (BoardBuffInstance instance : this.buffs.values()) value += instance.buff().moveDiceModifier(instance);
        return Math.max(0, value);
    }

    public int incomingDamageBonus() {
        int value = 0;
        for (BoardBuffInstance instance : this.buffs.values()) value += instance.buff().incomingDamageModifier(instance);
        return value;
    }

    public int turnStartHealing() {
        int value = 0;
        for (BoardBuffInstance instance : this.buffs.values()) value += Math.max(0, instance.buff().turnStartHealing(instance));
        return value;
    }

    public int turnStartDamage() {
        int value = 0;
        for (BoardBuffInstance instance : this.buffs.values()) value += Math.max(0, instance.buff().turnStartDamage(instance));
        return value;
    }

    public int roundRewardBonus() {
        int value = 0;
        for (BoardBuffInstance instance : this.buffs.values()) value += instance.buff().roundRewardBonus(instance);
        return Math.max(0, value);
    }

    public int resolveIncomingDamage(int damage) {
        return Math.max(0, damage + this.incomingDamageBonus());
    }

    public AstralPlayerStats consumeMoveRollBuffs() {
        Map<Identifier, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<Identifier, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            BoardBuffInstance instance = entry.getValue();
            if (!instance.buff().consumedAfterMoveRoll(instance)) continue;
            BoardBuffInstance retained = instance.withoutAcquiredLevels();
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public AstralPlayerStats consumeIncomingDamageBuffs() {
        Map<Identifier, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<Identifier, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            BoardBuffInstance instance = entry.getValue();
            if (!instance.buff().consumedAfterIncomingDamage(instance)) continue;
            BoardBuffInstance retained = instance.withoutAcquiredLevels();
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public int buff(BoardBuff buff) {
        if (buff == null) return 0;
        int levels = 0;
        for (BoardBuffInstance instance : this.buffs.values()) if (instance.buff() == buff) levels += instance.level();
        return levels;
    }

    public int buff(Identifier id) {
        int levels = 0;
        if (id == null) return 0;
        for (Map.Entry<Identifier, BoardBuffInstance> entry : this.buffs.entrySet()) {
            if (entry.getKey().equals(id) || entry.getValue().id().equals(id) || entry.getKey().toString().startsWith(id + "/")) {
                levels += entry.getValue().level();
            }
        }
        return levels;
    }

    public BoardBuffInstance buffInstance(BoardBuff buff) {
        if (buff == null) return null;
        for (BoardBuffInstance instance : this.buffs.values()) if (instance.buff() == buff) return instance;
        return null;
    }

    public BoardBuffInstance buffInstance(Identifier id) {
        return id == null ? null : this.buffs.get(id);
    }

    public boolean hasBuff(BoardBuff buff) {
        return this.buffInstance(buff) != null;
    }

    public boolean hasBuff(Identifier id) {
        return id != null && this.buffs.containsKey(id);
    }

    public Identifier nextBuffInstanceId(Identifier baseId) {
        if (!this.buffs.containsKey(baseId)) return baseId;
        for (int index = 2; index < 1024; index++) {
            Identifier candidate = Identifier.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "/" + index);
            if (!this.buffs.containsKey(candidate)) return candidate;
        }
        return Identifier.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "/" + System.currentTimeMillis());
    }

    public AstralPlayerStats heal(int amount) {
        return this.withHealth(this.health + Math.max(0, amount));
    }

    public AstralPlayerStats damage(int amount) {
        return this.withHealth(this.health - Math.max(0, amount));
    }

    public AstralPlayerStats addCoins(int amount) {
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health,
                Math.max(0, this.starCoins + amount), this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats spendCoins(int amount) {
        return this.addCoins(-Math.max(0, amount));
    }

    public AstralPlayerStats addStars(int amount) {
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                Math.max(0, this.stars + amount), this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addBaseAttack(int amount) {
        return this.copy(this.baseAttack + amount, this.baseDefense, this.maxHealth, this.health,
                this.starCoins, this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addBaseDefense(int amount) {
        return this.copy(this.baseAttack, this.baseDefense + amount, this.maxHealth, this.health,
                this.starCoins, this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addMaxHealth(int amount) {
        int added = Math.max(0, amount);
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth + added, this.health + added,
                this.starCoins, this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining,
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addCardPlaysPerTurn(int amount) {
        int added = Math.max(0, amount);
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health,
                this.starCoins, this.stars, this.cardPlaysPerTurn + added, this.cardPlaysRemaining + added,
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addTemporary(String stat, int amount, int turns) {
        List<TimedStatModifier> next = new ArrayList<>(this.modifiers);
        next.add(new TimedStatModifier(stat, amount, turns));
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.nextMoveFixed, this.buffs, next);
    }

    public AstralPlayerStats addBuff(BoardBuff buff, int duration, int amplifier) {
        if (buff == null) return this;
        Identifier id = AstralBoardBuffs.REGISTRY.getKey(buff);
        return id == null ? this : this.addBuff(AstralBoardBuffs.instance(id, buff).duration(duration).amplifier(amplifier).build());
    }

    public AstralPlayerStats addBuff(BoardBuffInstance instance) {
        return instance == null ? this : instance.buff().apply(this, instance);
    }

    public AstralPlayerStats addPermanentBuff(BoardBuff buff, int levels) {
        if (buff == null || levels <= 0) return this;
        Identifier id = AstralBoardBuffs.REGISTRY.getKey(buff);
        return id == null ? this : this.addBuff(AstralBoardBuffs.instance(id, buff).permanent().level(levels).build());
    }

    public AstralPlayerStats changeBuffLevel(BoardBuff buff, int amount) {
        if (buff == null || amount == 0) return this;
        Identifier id = AstralBoardBuffs.REGISTRY.getKey(buff);
        return id == null ? this : this.changeBuffLevel(id, amount);
    }

    public AstralPlayerStats changeBuffLevel(Identifier id, int amount) {
        if (id == null || amount == 0) return this;
        BoardBuffInstance current = this.buffs.get(id);
        if (current == null) return this;
        BoardBuffInstance next = current.withAcquiredLevels(current.acquiredLevels() + amount);
        return next == null ? this.removeBuff(id) : this.setBuff(next);
    }

    public AstralPlayerStats setBuff(BoardBuffInstance instance) {
        if (instance == null) return this;
        Map<Identifier, BoardBuffInstance> next = new HashMap<>(this.buffs);
        next.put(instance.id(), instance);
        return this.withBuffs(next);
    }

    public AstralPlayerStats removeBuff(BoardBuff buff) {
        if (buff == null) return this;
        Map<Identifier, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = next.values().removeIf(instance -> instance.buff() == buff);
        return changed ? this.withBuffs(next) : this;
    }

    public AstralPlayerStats removeBuff(Identifier id) {
        if (id == null || !this.buffs.containsKey(id)) return this;
        Map<Identifier, BoardBuffInstance> next = new HashMap<>(this.buffs);
        next.remove(id);
        return this.withBuffs(next);
    }

    public AstralPlayerStats clearAcquiredBuffs() {
        Map<Identifier, BoardBuffInstance> next = new HashMap<>(this.buffs);
        boolean changed = false;
        for (Map.Entry<Identifier, BoardBuffInstance> entry : new ArrayList<>(next.entrySet())) {
            BoardBuffInstance instance = entry.getValue();
            BoardBuffInstance retained = instance.buff().afterKnockout(instance);
            if (retained == instance) continue;
            if (retained == null) next.remove(entry.getKey());
            else next.put(entry.getKey(), retained);
            changed = true;
        }
        return changed ? this.withBuffs(next) : this;
    }

    public AstralPlayerStats useCardPlay() {
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, Math.max(0, this.cardPlaysRemaining - 1),
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats addCardPlaysThisTurn(int amount) {
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining + Math.max(0, amount),
                this.nextMoveFixed, this.buffs, this.modifiers);
    }

    public AstralPlayerStats setNextMoveFixed(int value) {
        int safeValue = Math.max(0, value);
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, safeValue, this.buffs, this.modifiers);
    }

    public AstralPlayerStats clearNextMoveDiceEffects() {
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, 0, this.buffs, this.modifiers);
    }

    public AstralPlayerStats beginTurn() {
        Map<Identifier, BoardBuffInstance> activated = new HashMap<>();
        this.buffs.forEach((id, instance) -> activated.put(id, instance.activate()));
        AstralPlayerStats next = this.withBuffs(activated);
        for (BoardBuffInstance original : new ArrayList<>(next.buffs.values())) {
            BoardBuffInstance current = next.buffInstance(original.id());
            if (current != null) next = current.buff().onTurnStart(next, current);
        }

        return next.copy(next.baseAttack, next.baseDefense, next.maxHealth, next.health, next.starCoins,
                next.stars, next.cardPlaysPerTurn, next.cardPlaysPerTurn, next.nextMoveFixed, next.buffs,
                next.tickModifiers());
    }

    public AstralPlayerStats endTurn() {
        AstralPlayerStats next = this;
        for (BoardBuffInstance original : new ArrayList<>(this.buffs.values())) {
            BoardBuffInstance current = next.buffInstance(original.id());
            if (current != null) next = current.buff().onTurnEnd(next, current);
        }

        Map<Identifier, BoardBuffInstance> ticked = new HashMap<>();
        for (Map.Entry<Identifier, BoardBuffInstance> entry : next.buffs.entrySet()) {
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
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, Math.clamp(value, 0, this.maxHealth),
                this.starCoins, this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.nextMoveFixed,
                this.buffs, this.modifiers);
    }

    private AstralPlayerStats withBuffs(Map<Identifier, BoardBuffInstance> buffs) {
        return this.copy(this.baseAttack, this.baseDefense, this.maxHealth, this.health, this.starCoins,
                this.stars, this.cardPlaysPerTurn, this.cardPlaysRemaining, this.nextMoveFixed, buffs, this.modifiers);
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

    private AstralPlayerStats copy(int baseAttack, int baseDefense, int maxHealth, int health, int starCoins,
                                   int stars, int cardPlaysPerTurn, int cardPlaysRemaining, int nextMoveFixed,
                                   Map<Identifier, BoardBuffInstance> buffs, List<TimedStatModifier> modifiers) {
        return new AstralPlayerStats(baseAttack, baseDefense, maxHealth, health, starCoins, stars,
                cardPlaysPerTurn, cardPlaysRemaining, nextMoveFixed, buffs, modifiers);
    }

    private static Map<Identifier, BoardBuffInstance> copyBuffs(Map<Identifier, BoardBuffInstance> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<Identifier, BoardBuffInstance> result = new HashMap<>();
        input.forEach((id, instance) -> {
            if (id != null && instance != null) result.put(id, instance.id().equals(id) ? instance
                    : new BoardBuffInstance(id, instance.buff(), instance.duration(), instance.amplifier(),
                    instance.intrinsicLevels(), instance.fresh(), instance.value(), instance.customName(),
                    instance.customIcon(), instance.customColor(), instance.consumeAfterIncomingDamage(),
                    instance.consumeAfterMoveRoll()));
        });
        return Map.copyOf(result);
    }

}
