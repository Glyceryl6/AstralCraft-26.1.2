package com.astral_craft.common.gameplay.character;

import net.minecraft.server.level.ServerPlayer;

public interface AstralCharacterSkill {

    boolean use(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill);

}