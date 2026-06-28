package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.data.provider.AstralCharacterDataCatalog;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinRarityDefinition;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class AstralSkinRarityBootstrap {

    public static final ResourceKey<Registry<CharacterSkinRarityDefinition>> SKIN_RARITIES = ResourceKey.createRegistryKey(AstralCraft.prefix("skin_rarities"));

    public static void bootstrap(BootstrapContext<CharacterSkinRarityDefinition> context) {
        for (AstralCharacterDataCatalog.SkinRarityEntry entry : AstralCharacterDataCatalog.SKIN_RARITIES) {
            context.register(key(entry.id()), rarity(entry));
        }
    }

    public static ResourceKey<CharacterSkinRarityDefinition> key(String path) {
        return ResourceKey.create(SKIN_RARITIES, AstralCraft.prefix(path));
    }

    public static CharacterSkinRarityDefinition rarity(AstralCharacterDataCatalog.SkinRarityEntry entry) {
        return new CharacterSkinRarityDefinition(entry.nameKey(), entry.borderColor(), entry.badgeColor(), entry.textColor());
    }

}