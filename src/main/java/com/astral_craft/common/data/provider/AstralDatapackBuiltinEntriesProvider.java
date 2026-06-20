package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AstralDatapackBuiltinEntriesProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(AstralDataPackRegistryKeys.CHARACTERS, AstralCharacterBootstrap::bootstrap)
            .add(AstralDataPackRegistryKeys.CHARACTER_SKINS, AstralCharacterSkinBootstrap::bootstrap)
            .add(AstralDataPackRegistryKeys.SKIN_RARITIES, AstralSkinRarityBootstrap::bootstrap)
            .add(AstralDataPackRegistryKeys.CARD_BACKS, AstralCardBackBootstrap::bootstrap)
            .add(AstralDataPackRegistryKeys.EVENTS, AstralEventBootstrap::bootstrap);

    public AstralDatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(AstralCraft.MOD_ID));
    }

}