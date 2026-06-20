package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class AstralCardBackBootstrap {

    public static final ResourceKey<CardBackDefinition> DEFAULT = key("default");

    public static void bootstrap(BootstrapContext<CardBackDefinition> context) {
        context.register(DEFAULT, CardBackDefinition.builtinDefault());
    }

    public static ResourceKey<CardBackDefinition> key(String path) {
        return ResourceKey.create(AstralDataPackRegistryKeys.CARD_BACKS, AstralCraft.prefix(path));
    }

}