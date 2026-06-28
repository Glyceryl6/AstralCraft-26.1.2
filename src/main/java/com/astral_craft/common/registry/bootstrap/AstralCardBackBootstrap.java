package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class AstralCardBackBootstrap {

    public static final ResourceKey<Registry<CardBackDefinition>> CARD_BACKS = ResourceKey.createRegistryKey(AstralCraft.prefix("card_backs"));
    public static final ResourceKey<CardBackDefinition> DEFAULT = key("default");

    public static void bootstrap(BootstrapContext<CardBackDefinition> context) {
        context.register(DEFAULT, CardBackDefinition.builtinDefault());
    }

    public static ResourceKey<CardBackDefinition> key(String path) {
        return ResourceKey.create(CARD_BACKS, AstralCraft.prefix(path));
    }

}