package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.resources.Identifier;

public record CharacterSkillEffect(Identifier statusId, Identifier characterId, int durationTicks, int amplifier) {

    public CharacterSkillEffect {
        statusId = statusId == null ? AstralStatusEffects.NO_STATUS_ID : statusId;
        characterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        durationTicks = Math.max(0, durationTicks);
        amplifier = Math.clamp(amplifier, 0, 255);
    }

    public static CharacterSkillEffect of(Identifier statusId, Identifier characterId, int durationTicks) {
        return new CharacterSkillEffect(statusId, characterId, durationTicks, 0);
    }

}