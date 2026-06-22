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
import java.util.function.Function;

public class AstralCharacterBootstrap {

    public static void bootstrap(BootstrapContext<CharacterDefinition> context) {
        for (AstralCharacterDataCatalog.CharacterEntry entry : AstralCharacterDataCatalog.CHARACTERS) {
            context.register(key(entry.id), definition(entry));
        }
    }

    public static ResourceKey<CharacterDefinition> key(String path) {
        return ResourceKey.create(AstralDataPackRegistryKeys.CHARACTERS, AstralCraft.prefix(path));
    }

    public static CharacterDefinition definition(AstralCharacterDataCatalog.CharacterEntry entry) {
        String id = entry.id, prefix = "character.astral_craft." + id;
        return new CharacterDefinition(AstralCraft.prefix(id),
                prefix + ".name", prefix + ".title",
                AstralCraft.prefix("humanoid"),
                AstralCraft.prefix("entity/character/skin_" + id + "_default"),
                AstralCraft.prefix("astral_character"),
                AstralCraft.prefix("player"),
                AstralCraft.prefix("humanoid"),
                "idle", 6, 5,
                new CharacterStatsDefinition(entry.attack, entry.defense, entry.health),
                entry.skillSameIn2Mode ? List.of(skill(id, "active", entry.cooldown), skill(id, "passive", 0))
                        : List.of(skill(id, "active", entry.pvpCooldown, entry.pveCooldown), skill(id, "passive", -1, -1)),
                List.of(new CharacterProfileSection("", prefix + ".profile.basic.body")), List.of(), Boolean.TRUE,
                entry.implicitBondSkin, entry.unlockedByDefault, prefix + ".unlock_hint", entry.sortOrder);
    }

    private static CharacterSkillDefinition skill(String id, String type, int cooldown) {
        String key = "character.astral_craft." + id + ".skill." + type;
        return new CharacterSkillDefinition(type, key, key + ".desc", cooldown);
    }

    private static CharacterSkillDefinition skill(String id, String type, int pvpCooldown, int pveCooldown) {
        String key = "character.astral_craft." + id + ".skill." + type;
        Function<String, String> nameKey = s -> key + "." + s;
        Function<String, String> descKey = s -> key + "." + s + ".desc";
        return new CharacterSkillDefinition(type, "", "", 0,
                nameKey.apply("pvp"), descKey.apply("pvp"), pvpCooldown,
                nameKey.apply("pve"), descKey.apply("pve"), pveCooldown);
    }

}