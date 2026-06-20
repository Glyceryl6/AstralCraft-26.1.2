package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.data.provider.AstralCharacterDataCatalog;
import com.astral_craft.common.gameplay.character.CharacterSkinAddition;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class AstralCharacterSkinBootstrap {

    public static void bootstrap(BootstrapContext<CharacterSkinAddition> context) {
        for (AstralCharacterDataCatalog.CharacterEntry character : AstralCharacterDataCatalog.CHARACTERS) {
            for (AstralCharacterDataCatalog.SkinEntry skin : character.skins()) {
                context.register(key(character.id(), skin.id()), skin(character, skin));
            }
        }
    }

    public static ResourceKey<CharacterSkinAddition> key(String characterId, String skinId) {
        return ResourceKey.create(AstralDataPackRegistryKeys.CHARACTER_SKINS, AstralCraft.prefix(characterId + "/" + skinId));
    }

    public static CharacterSkinAddition skin(AstralCharacterDataCatalog.CharacterEntry character, AstralCharacterDataCatalog.SkinEntry skin) {
        return new CharacterSkinAddition(AstralCraft.prefix(character.id()), skin.id(),
                "character.astral_craft." + character.id() + ".skin." + skin.id(),
                texture(character.id(), skin.id()), false, skin.rarity());
    }

    private static Identifier texture(String characterId, String skinId) {
        return AstralCraft.prefix("entity/character/skin_" + characterId + "_" + skinId);
    }

}