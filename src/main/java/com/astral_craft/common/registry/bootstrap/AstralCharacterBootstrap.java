package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.data.provider.AstralCharacterDataCatalog;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterPotentialDefinition;
import com.astral_craft.common.gameplay.character.CharacterProfileSection;
import com.astral_craft.common.gameplay.character.CharacterStatsDefinition;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillDefinition;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillType;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public class AstralCharacterBootstrap {

    public static final ResourceKey<Registry<CharacterDefinition>> CHARACTERS = ResourceKey.createRegistryKey(AstralCraft.prefix("characters"));

    public static void bootstrap(BootstrapContext<CharacterDefinition> context) {
        for (AstralCharacterDataCatalog.CharacterEntry entry : AstralCharacterDataCatalog.CHARACTERS) {
            context.register(key(entry.id), definition(entry));
        }
    }

    public static ResourceKey<CharacterDefinition> key(String path) {
        return ResourceKey.create(CHARACTERS, AstralCraft.prefix(path));
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
                "idle", 6, 6,
                new CharacterStatsDefinition(entry.attack, entry.defense, entry.health),
                entry.skillSameIn2Mode ? List.of(skill(id, CharacterSkillType.ACTIVE, entry.cooldown), skill(id, CharacterSkillType.PASSIVE, 0))
                        : List.of(skill(id, CharacterSkillType.ACTIVE, entry.pvpCooldown, entry.pveCooldown), skill(id, CharacterSkillType.PASSIVE, -1, -1)),
                List.of(new CharacterProfileSection("", prefix + ".profile.basic.body")), List.of(), entry.hasPotential,
                entry.hasPotential ? new CharacterPotentialDefinition(true, entry.potentialRequiredLevel, entry.potentialRequiredFriendship,
                        entry.potentialRequiredExperience, entry.potentialMaterials) : CharacterPotentialDefinition.NONE,
                Boolean.TRUE, entry.implicitBondSkin, entry.unlockedByDefault, prefix + ".unlock_hint", entry.sortOrder);
    }

    private static CharacterSkillDefinition skill(String id, CharacterSkillType type, int cooldown) {
        return new CharacterSkillDefinition(type, cooldown, 0, AstralCraft.prefix(id), CharacterSkillDefinition.DEFAULT_ANIMATION_ID, false, false, -1, -1, AstralStatusEffects.NO_STATUS_ID);
    }

    private static CharacterSkillDefinition skill(String id, CharacterSkillType type, int pvpCooldown, int pveCooldown) {
        return new CharacterSkillDefinition(type, 0, 0, AstralCraft.prefix(id), CharacterSkillDefinition.DEFAULT_ANIMATION_ID, true, true, pvpCooldown, pveCooldown, AstralStatusEffects.NO_STATUS_ID);
    }

}