package com.astral_craft.common.gameplay.character;

import net.minecraft.server.level.ServerPlayer;

public record CharacterSkillContext(
        ServerPlayer player,
        ActiveCharacterState state,
        CharacterDefinition definition,
        CharacterSkillDefinition skill,
        CharacterSkillState skillState) {
}
