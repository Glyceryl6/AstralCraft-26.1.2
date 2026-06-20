package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.data.provider.AstralCharacterDataCatalog;
import com.astral_craft.common.gameplay.character.SkinRarityDefinition;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class AstralSkinRarityBootstrap {

    public static void bootstrap(BootstrapContext<SkinRarityDefinition> context) {
        for (AstralCharacterDataCatalog.SkinRarityEntry entry : AstralCharacterDataCatalog.SKIN_RARITIES) {
            context.register(key(entry.id()), rarity(entry));
        }
    }

    public static ResourceKey<SkinRarityDefinition> key(String path) {
        return ResourceKey.create(AstralDataPackRegistryKeys.SKIN_RARITIES, AstralCraft.prefix(path));
    }

    public static SkinRarityDefinition rarity(AstralCharacterDataCatalog.SkinRarityEntry entry) {
        return new SkinRarityDefinition(entry.nameKey(), entry.borderColor(), entry.badgeColor(), entry.textColor());
    }

}