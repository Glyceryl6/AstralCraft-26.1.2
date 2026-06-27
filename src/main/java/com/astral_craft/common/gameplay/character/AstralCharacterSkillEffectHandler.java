package com.astral_craft.common.gameplay.character;

public interface AstralCharacterSkillEffectHandler {

    default void onEffectStart(CharacterSkillContext context, CharacterSkillEffect effect) {
    }

    default void onEffectTick(CharacterSkillContext context, CharacterSkillEffect effect) {
    }

    default void onEffectEnd(CharacterSkillContext context, CharacterSkillEffect effect) {
    }

}