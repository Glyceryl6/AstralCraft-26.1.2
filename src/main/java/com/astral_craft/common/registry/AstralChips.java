package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.StatBundle;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AstralChips {

    public static final ResourceKey<Registry<ChipDefinition>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_party_chips"));
    public static final DeferredRegister<ChipDefinition> CHIPS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<ChipDefinition> REGISTRY = CHIPS.makeRegistry(_ -> {});

    public static final ChipDefinition BOXING_GLOVES_BASIC = register("boxing_gloves_basic", ChipRarity.BLUE).pool(ChipPool.ATTACK).attack(1).build();
    public static final ChipDefinition BOXING_GLOVES_FAIR = register("boxing_gloves_fair", ChipRarity.PURPLE).pool(ChipPool.ATTACK).attack(2).build();
    public static final ChipDefinition BOXING_GLOVES_EXCELLENT = register("boxing_gloves_excellent", ChipRarity.GOLD).pool(ChipPool.ATTACK).attack(5).build();
    public static final ChipDefinition MOTORCYCLE_HELMET_REGULAR = register("motorcycle_helmet_regular", ChipRarity.BLUE).pool(ChipPool.SUSTAIN).defense(1).build();
    public static final ChipDefinition MOTORCYCLE_HELMET_FAIR = register("motorcycle_helmet_fair", ChipRarity.PURPLE).pool(ChipPool.SUSTAIN).defense(2).build();
    public static final ChipDefinition MOTORCYCLE_HELMET_EXCELLENT = register("motorcycle_helmet_excellent", ChipRarity.GOLD).pool(ChipPool.SUSTAIN).defense(3).build();
    public static final ChipDefinition SANDWICH_COOKIE_REGULAR = register("sandwich_cookie_regular", ChipRarity.BLUE).pool(ChipPool.SUSTAIN).maxHealth(2).health(2).build();
    public static final ChipDefinition SANDWICH_COOKIE_TASTY = register("sandwich_cookie_tasty", ChipRarity.PURPLE).pool(ChipPool.SUSTAIN).maxHealth(3).health(3).build();
    public static final ChipDefinition SANDWICH_COOKIE_DELICIOUS = register("sandwich_cookie_delicious", ChipRarity.GOLD).pool(ChipPool.SUSTAIN).maxHealth(5).health(5).build();
    public static final ChipDefinition SPEED_SKATES_BASIC = register("speed_skates_basic", ChipRarity.BLUE).speed(1).build();
    public static final ChipDefinition SPEED_SKATES_FAIR = register("speed_skates_fair", ChipRarity.PURPLE).speed(2).build();
    public static final ChipDefinition SPEED_SKATES_EXCELLENT = register("speed_skates_excellent", ChipRarity.GOLD).speed(4).build();
    public static final ChipDefinition LARGE_BACKPACK = register("large_backpack", ChipRarity.PURPLE).pool(ChipPool.CARDS).cardPlays(1).build();
    public static final ChipDefinition EXTRA_BATTERY = register("extra_battery", ChipRarity.PURPLE).pool(ChipPool.CARDS).skillCooldownReduction(1).build();
    public static final ChipDefinition SMARTWATCH = register("smartwatch", ChipRarity.PURPLE)
            .pool(ChipPool.CARDS).afterTurnEnd((level, participant) -> participant.hand().size() >= 5
                    ? participant : BoardSessionManager.randomPvpCardId(level).map(participant::addCard).orElse(participant)).build();
    public static final ChipDefinition PIGGY_BANK = register("piggy_bank", ChipRarity.BLUE)
            .pool(ChipPool.CARDS).afterEffectCardPlayed((_, participant) -> {
                int played = participant.chipProgress().effectCardsPlayed() + 1;
                boolean reward = played >= 2;
                BoardParticipant updated = participant.withChipProgress(
                        participant.chipProgress().withEffectCardsPlayed(reward ? 0 : played));
                return reward ? updated.withStats(updated.stats().addCoins(3)) : updated;
            }).build();
    public static final ChipDefinition BANK_CARD_LOW = register("bank_card_low", ChipRarity.BLUE)
            .pool(ChipPool.SUPPORT).keyword(AstralBoardBuffs.STARLIGHT).starlight(4).build();
    public static final ChipDefinition BANK_CARD_HIGH = register("bank_card_high", ChipRarity.PURPLE)
            .pool(ChipPool.SUPPORT).keyword(AstralBoardBuffs.STARLIGHT).starlight(7).build();
    public static final ChipDefinition MEDICAL_KIT_EMERGENCY = register("medical_kit_emergency", ChipRarity.BLUE)
            .pool(ChipPool.SUPPORT).keyword(AstralBoardBuffs.HEAL)
            .beforeTurnStart((_, participant) -> participant.withStats(
                    participant.stats().addPermanentBuff(AstralBoardBuffs.HEAL.get(), 1))).build();
    public static final ChipDefinition MEDICAL_KIT_FULL = register("medical_kit_full", ChipRarity.GOLD)
            .pool(ChipPool.SUPPORT).keyword(AstralBoardBuffs.HEAL)
            .beforeTurnStart((_, participant) -> participant.withStats(
                    participant.stats().addPermanentBuff(AstralBoardBuffs.HEAL.get(), 3))).build();
    public static final ChipDefinition MARKING_SPRAY = register("marking_spray", ChipRarity.BLUE)
            .pool(ChipPool.ATTACK).keyword(AstralBoardBuffs.MARK).build();
    public static final ChipDefinition STANDARD_SIGHT = register("standard_sight", ChipRarity.PURPLE)
            .pool(ChipPool.ATTACK).keyword(AstralBoardBuffs.MARK).attack(2).build();

    public static DeferredRegister<ChipDefinition> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Optional<ChipDefinition> get(String id) {
        return get(AstralCraft.prefix(id));
    }

    public static Optional<ChipDefinition> get(Identifier id) {
        return Optional.ofNullable(id == null ? null : REGISTRY.getValue(id));
    }

    public static List<ChipDefinition> values() {
        List<ChipDefinition> values = new ArrayList<>();
        for (DeferredHolder<ChipDefinition, ? extends ChipDefinition> holder : CHIPS.getEntries()) values.add(holder.get());
        return List.copyOf(values);
    }

    public static int[] rarityWeights(boolean normalDifficulty, int level) {
        if (normalDifficulty) {
            if (level >= 3) return new int[]{20, 50, 30};
            if (level == 2) return new int[]{40, 40, 20};
            return new int[]{60, 30, 10};
        }
        if (level >= 3) return new int[]{25, 50, 25};
        if (level == 2) return new int[]{40, 45, 15};
        return new int[]{60, 37, 3};
    }

    private static ChipBuilder register(String id, ChipRarity rarity) {
        return new ChipBuilder(id, rarity);
    }

    private static class ChipBuilder {

        private final String id;
        private final ChipRarity rarity;
        private ChipPool pool = ChipPool.GENERAL;
        private @Nullable Supplier<? extends BoardBuff> keyword;
        private @Nullable Identifier mapRestriction;
        private int attack;
        private int defense;
        private int speed;
        private int maxHealth;
        private int health;
        private int starCoins;
        private int cardPlays;
        private int skillCooldownReduction;
        private int healStacks;
        private int starlightStacks;
        private int markStacks;
        private ChipDefinition.@Nullable ParticipantEffect turnStartEffect;
        private ChipDefinition.@Nullable ParticipantEffect effectCardPlayedEffect;
        private ChipDefinition.@Nullable ParticipantEffect turnEndEffect;

        private ChipBuilder(String id, ChipRarity rarity) {
            this.id = id;
            this.rarity = rarity;
        }

        private ChipBuilder pool(ChipPool pool) {
            this.pool = pool;
            return this;
        }

        private ChipBuilder keyword(Supplier<? extends BoardBuff> keyword) {
            this.keyword = keyword;
            return this;
        }

        private ChipBuilder map(Identifier mapRestriction) {
            this.mapRestriction = mapRestriction;
            return this;
        }

        private ChipBuilder attack(int value) {
            this.attack = value;
            return this;
        }

        private ChipBuilder defense(int value) {
            this.defense = value;
            return this;
        }

        private ChipBuilder speed(int value) {
            this.speed = value;
            return this;
        }

        private ChipBuilder maxHealth(int value) {
            this.maxHealth = value;
            return this;
        }

        private ChipBuilder health(int value) {
            this.health = value;
            return this;
        }

        private ChipBuilder starCoins(int value) {
            this.starCoins = value;
            return this;
        }

        private ChipBuilder cardPlays(int value) {
            this.cardPlays = value;
            return this;
        }

        private ChipBuilder skillCooldownReduction(int value) {
            this.skillCooldownReduction = value;
            return this;
        }

        private ChipBuilder heal(int value) {
            this.healStacks = value;
            return this;
        }

        private ChipBuilder starlight(int value) {
            this.starlightStacks = value;
            return this;
        }

        private ChipBuilder mark(int value) {
            this.markStacks = value;
            return this;
        }

        private ChipBuilder beforeTurnStart(ChipDefinition.ParticipantEffect effect) {
            this.turnStartEffect = effect;
            return this;
        }

        private ChipBuilder afterEffectCardPlayed(ChipDefinition.ParticipantEffect effect) {
            this.effectCardPlayedEffect = effect;
            return this;
        }

        private ChipBuilder afterTurnEnd(ChipDefinition.ParticipantEffect effect) {
            this.turnEndEffect = effect;
            return this;
        }

        private ChipDefinition build() {
            StatBundle stats = new StatBundle(this.attack, this.defense, this.speed, this.maxHealth, this.health, this.starCoins,
                    this.cardPlays, this.skillCooldownReduction, this.healStacks, this.starlightStacks, this.markStacks);
            ChipDefinition definition = new ChipDefinition(this.id, ChipDefinition.nameKey(this.id),
                    ChipDefinition.effectKey(this.id), this.rarity, this.keyword, stats, this.pool,
                    this.mapRestriction, this.turnStartEffect, this.effectCardPlayedEffect, this.turnEndEffect);
            CHIPS.register(this.id, () -> definition);
            return definition;
        }

    }

}