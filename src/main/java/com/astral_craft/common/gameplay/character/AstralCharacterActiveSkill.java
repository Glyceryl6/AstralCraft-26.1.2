package com.astral_craft.common.gameplay.character;

@FunctionalInterface
public interface AstralCharacterActiveSkill {

    boolean use(CharacterSkillContext context);

}
