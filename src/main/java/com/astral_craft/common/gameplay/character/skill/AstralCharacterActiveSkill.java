package com.astral_craft.common.gameplay.character.skill;

@FunctionalInterface
public interface AstralCharacterActiveSkill {

    boolean use(CharacterSkillContext context);

}
