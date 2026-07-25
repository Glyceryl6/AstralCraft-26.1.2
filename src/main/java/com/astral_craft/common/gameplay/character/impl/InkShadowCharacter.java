package com.astral_craft.common.gameplay.character.impl;

import com.astral_craft.common.gameplay.character.AstralCharacter;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillContext;

public class InkShadowCharacter extends AstralCharacter {

    public InkShadowCharacter(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasActiveSkill() {
        return true;
    }

    @Override
    public boolean useActiveSkill(CharacterSkillContext context) {
        return grantConfiguredStatusEffect(context);
    }

}