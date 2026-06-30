package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterActiveSkill;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterPassiveSkill;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillSet;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public class AstralCharacterSkills {

    public static final ResourceKey<Registry<AstralCharacterSkillSet>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("character_skill_sets"));
    public static final DeferredRegister<AstralCharacterSkillSet> SKILL_SETS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<AstralCharacterSkillSet> REGISTRY = SKILL_SETS.makeRegistry(_ -> {});

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill) {
        return register(characterPath, activeSkill, List.of());
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills) {
        Identifier characterId = AstralCraft.prefix(characterPath);
        return SKILL_SETS.register(characterPath, () -> new AstralCharacterSkillSet(characterId, activeSkill, passiveSkills, CharacterSkillDefinition.DEFAULT_ANIMATION_ID));
    }

    public static Optional<AstralCharacterSkillSet> get(Identifier id) {
        return Optional.ofNullable(REGISTRY.getValue(id));
    }

}