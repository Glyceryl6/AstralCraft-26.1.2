package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.data.provider.AstralCharacterDataCatalog;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProfileSection;
import com.astral_craft.common.gameplay.character.CharacterSkillDefinition;
import com.astral_craft.common.gameplay.character.CharacterStatsDefinition;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public class AstralCharacterBootstrap {

    public static void bootstrap(BootstrapContext<CharacterDefinition> context) {
        for (AstralCharacterDataCatalog.CharacterEntry entry : AstralCharacterDataCatalog.CHARACTERS) {
            context.register(key(entry.id()), definition(entry));
        }
    }

    public static ResourceKey<CharacterDefinition> key(String path) {
        return ResourceKey.create(AstralDataPackRegistryKeys.CHARACTERS, AstralCraft.prefix(path));
    }

    public static CharacterDefinition definition(AstralCharacterDataCatalog.CharacterEntry entry) {
        String id = entry.id();
        return new CharacterDefinition(AstralCraft.prefix(id),
                "character.astral_craft." + id + ".name",
                "character.astral_craft." + id + ".title",
                AstralCraft.prefix("humanoid"),
                AstralCraft.prefix("textures/entity/character/skin_" + id + "_default.png"),
                AstralCraft.prefix("astral_character"),
                AstralCraft.prefix("player"),
                AstralCraft.prefix("humanoid"),
                "idle", 6, 5,
                new CharacterStatsDefinition(entry.attack(), entry.defense(), entry.health()),
                List.of(skill("active", id, entry.activeCooldown()), skill("passive", id, 0)),
                List.of(new CharacterProfileSection("", "character.astral_craft." + id + ".profile.basic.body")),
                List.of(), entry.unlockedByDefault(), "character.astral_craft." + id + ".unlock_hint", entry.sortOrder());
    }

    private static CharacterSkillDefinition skill(String type, String id, int cooldown) {
        return new CharacterSkillDefinition(type,
                "character.astral_craft." + id + ".skill." + type,
                "character.astral_craft." + id + ".skill." + type + ".desc", cooldown);
    }

}