package com.astral_craft.common.gameplay.character.impl;

import com.astral_craft.common.gameplay.character.AstralCharacter;
import com.astral_craft.common.gameplay.character.CharacterProgressionDefinition;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillContext;

public class InkShadowCharacter extends AstralCharacter {

    public InkShadowCharacter(Properties properties, CharacterProgressionDefinition progression) {
        super(properties, progression);
    }

    @Override
    public boolean useActiveSkill(CharacterSkillContext context) {
        return grantConfiguredStatusEffect(context);
    }
}
