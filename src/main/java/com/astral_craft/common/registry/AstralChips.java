package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import com.astral_craft.common.gameplay.chip.type.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class AstralChips {

    public static final ResourceKey<Registry<ChipDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(AstralCraft.prefix("astral_party_chips"));
    public static final DeferredRegister<ChipDefinition> CHIPS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<ChipDefinition> REGISTRY = CHIPS.makeRegistry(_ -> {});

    public static final DeferredHolder<ChipDefinition, AttackChip> BOXING_GLOVES_BASIC = register(
            "boxing_gloves_basic", id -> new AttackChip(id, ChipRarity.BLUE, 1));
    public static final DeferredHolder<ChipDefinition, AttackChip> BOXING_GLOVES_FAIR = register(
            "boxing_gloves_fair", id -> new AttackChip(id, ChipRarity.PURPLE, 2));
    public static final DeferredHolder<ChipDefinition, AttackChip> BOXING_GLOVES_EXCELLENT = register(
            "boxing_gloves_excellent", id -> new AttackChip(id, ChipRarity.GOLD, 5));
    public static final DeferredHolder<ChipDefinition, DefenseChip> MOTORCYCLE_HELMET_REGULAR = register(
            "motorcycle_helmet_regular", id -> new DefenseChip(id, ChipRarity.BLUE, 1));
    public static final DeferredHolder<ChipDefinition, DefenseChip> MOTORCYCLE_HELMET_FAIR = register(
            "motorcycle_helmet_fair", id -> new DefenseChip(id, ChipRarity.PURPLE, 2));
    public static final DeferredHolder<ChipDefinition, DefenseChip> MOTORCYCLE_HELMET_EXCELLENT = register(
            "motorcycle_helmet_excellent", id -> new DefenseChip(id, ChipRarity.GOLD, 3));
    public static final DeferredHolder<ChipDefinition, MaxHealthChip> SANDWICH_COOKIE_REGULAR = register(
            "sandwich_cookie_regular", id -> new MaxHealthChip(id, ChipRarity.BLUE, 2));
    public static final DeferredHolder<ChipDefinition, MaxHealthChip> SANDWICH_COOKIE_TASTY = register(
            "sandwich_cookie_tasty", id -> new MaxHealthChip(id, ChipRarity.PURPLE, 3));
    public static final DeferredHolder<ChipDefinition, MaxHealthChip> SANDWICH_COOKIE_DELICIOUS = register(
            "sandwich_cookie_delicious", id -> new MaxHealthChip(id, ChipRarity.GOLD, 5));
    public static final DeferredHolder<ChipDefinition, SpeedChip> SPEED_SKATES_BASIC = register(
            "speed_skates_basic", id -> new SpeedChip(id, ChipRarity.BLUE, 1));
    public static final DeferredHolder<ChipDefinition, SpeedChip> SPEED_SKATES_FAIR = register(
            "speed_skates_fair", id -> new SpeedChip(id, ChipRarity.PURPLE, 2));
    public static final DeferredHolder<ChipDefinition, SpeedChip> SPEED_SKATES_EXCELLENT = register(
            "speed_skates_excellent", id -> new SpeedChip(id, ChipRarity.GOLD, 4));
    public static final DeferredHolder<ChipDefinition, CardPlayChip> LARGE_BACKPACK = register(
            "large_backpack", id -> new CardPlayChip(id, ChipRarity.PURPLE, 1));
    public static final DeferredHolder<ChipDefinition, SkillCooldownChip> EXTRA_BATTERY = register(
            "extra_battery", id -> new SkillCooldownChip(id, ChipRarity.PURPLE, 1));
    public static final DeferredHolder<ChipDefinition, SmartwatchChip> SMARTWATCH = register(
            "smartwatch", id -> new SmartwatchChip(id, ChipRarity.PURPLE));
    public static final DeferredHolder<ChipDefinition, PiggyBankChip> PIGGY_BANK = register(
            "piggy_bank", id -> new PiggyBankChip(id, ChipRarity.BLUE));
    public static final DeferredHolder<ChipDefinition, BuffStackChip> BANK_CARD_LOW = register(
            "bank_card_low", id -> new BuffStackChip(id, ChipRarity.BLUE, ChipPool.SUPPORT,
                    AstralBoardBuffs.STARLIGHT_ID, 4));
    public static final DeferredHolder<ChipDefinition, BuffStackChip> BANK_CARD_HIGH = register(
            "bank_card_high", id -> new BuffStackChip(id, ChipRarity.PURPLE, ChipPool.SUPPORT,
                    AstralBoardBuffs.STARLIGHT_ID, 7));
    public static final DeferredHolder<ChipDefinition, TurnStartBuffChip> MEDICAL_KIT_EMERGENCY = register(
            "medical_kit_emergency", id -> new TurnStartBuffChip(id, ChipRarity.BLUE, ChipPool.SUPPORT,
                    AstralBoardBuffs.HEAL_ID, 1));
    public static final DeferredHolder<ChipDefinition, TurnStartBuffChip> MEDICAL_KIT_FULL = register(
            "medical_kit_full", id -> new TurnStartBuffChip(id, ChipRarity.GOLD, ChipPool.SUPPORT,
                    AstralBoardBuffs.HEAL_ID, 3));
    public static final DeferredHolder<ChipDefinition, KeywordChip> MARKING_SPRAY = register(
            "marking_spray", id -> new KeywordChip(id, ChipRarity.BLUE, ChipPool.ATTACK,
                    AstralBoardBuffs.MARK_ID));
    public static final DeferredHolder<ChipDefinition, AttackChip> STANDARD_SIGHT = register(
            "standard_sight", id -> new AttackChip(id, ChipRarity.PURPLE, 2, AstralBoardBuffs.MARK_ID));

    public static DeferredRegister<ChipDefinition> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Optional<ChipDefinition> get(String id) {
        return get(AstralCraft.prefix(id));
    }

    public static Optional<ChipDefinition> get(@Nullable Identifier id) {
        return Optional.ofNullable(id == null ? null : REGISTRY.getValue(id));
    }

    public static Collection<DeferredHolder<ChipDefinition, ? extends ChipDefinition>> allHolders() {
        return CHIPS.getEntries();
    }

    public static List<ChipDefinition> values() {
        List<ChipDefinition> values = new ArrayList<>(CHIPS.getEntries().size());
        for (DeferredHolder<ChipDefinition, ? extends ChipDefinition> holder : CHIPS.getEntries()) {
            values.add(holder.get());
        }
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

    private static <T extends ChipDefinition> DeferredHolder<ChipDefinition, T> register(
            String path, Function<Identifier, T> factory) {
        Identifier id = AstralCraft.prefix(path);
        return CHIPS.register(path, () -> factory.apply(id));
    }

}