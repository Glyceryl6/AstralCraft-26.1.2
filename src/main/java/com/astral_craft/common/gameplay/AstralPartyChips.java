package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.Optional;

/** NeoForge-style registry for chip metadata. Built-in chips are declared in {@link AstralBuiltinChips}. */
public class AstralPartyChips {

    public static final ResourceKey<Registry<ChipDefinition>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_party_chips"));
    public static final DeferredRegister<ChipDefinition> CHIPS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<ChipDefinition> REGISTRY = CHIPS.makeRegistry(builder -> {});

    public static ChipDefinition register(ChipDefinition definition) {
        CHIPS.register(definition.id(), () -> definition);
        return definition;
    }

    public static DeferredRegister<ChipDefinition> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Optional<ChipDefinition> get(String id) {
        return Optional.ofNullable(REGISTRY.getValue(AstralCraft.prefix(id)));
    }

    public static Collection<DeferredHolder<ChipDefinition, ? extends ChipDefinition>> allHolders() {
        return CHIPS.getEntries();
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

}