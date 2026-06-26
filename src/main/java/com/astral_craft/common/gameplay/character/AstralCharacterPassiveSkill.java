package com.astral_craft.common.gameplay.character;

@FunctionalInterface
public interface AstralCharacterPassiveSkill {

    void serverTick(CharacterSkillContext context);

}
