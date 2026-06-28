package com.astral_craft.common.gameplay.character.skill;

@FunctionalInterface
public interface AstralCharacterPassiveSkill {

    void serverTick(CharacterSkillContext context);

}
