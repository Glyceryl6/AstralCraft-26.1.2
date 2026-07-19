package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.character.skill.*;
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
    public static final Identifier DEFAULT_CUTIN_ANIMATION = AstralCraft.prefix("skill");

    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> INK_SHADOW = registerStatusSkill("ink_shadow");

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill) {
        return register(characterPath, activeSkill, List.of(), DEFAULT_CUTIN_ANIMATION);
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills) {
        return register(characterPath, activeSkill, passiveSkills, DEFAULT_CUTIN_ANIMATION);
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills, Identifier fallbackAnimation) {
        Identifier characterId = AstralCraft.prefix(characterPath);
        return SKILL_SETS.register(characterPath, () -> new AstralCharacterSkillSet(characterId, activeSkill, passiveSkills, fallbackAnimation));
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> registerStatusSkill(String characterPath) {
        return register(characterPath, AstralCharacterSkills::grantConfiguredStatusEffect);
    }

    public static Optional<AstralCharacterSkillSet> get(Identifier id) {
        return Optional.ofNullable(REGISTRY.getValue(id));
    }

    public static boolean grantConfiguredStatusEffect(CharacterSkillContext context) {
        if (context == null || context.skill() == null) return false;
        return context.skill().statusEffectId().filter(statusId -> {
            if (context.actor() instanceof AstralCharacterEntity character && character.isBoardPawn()) {
                return BoardSessionManager.addRoundStatusEffect(character, statusId,
                        AstralCharacterSkillService.durationRounds(context.skill()));
            }
            return AstralCharacterSkillEffects.add(context.actor(), statusId,
                    AstralCharacterSkillService.durationTicks(context.skill()), 0);
        }).isPresent();
    }

}