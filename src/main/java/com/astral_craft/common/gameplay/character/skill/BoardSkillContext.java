package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record BoardSkillContext(ServerPlayer player, ServerLevel level, AstralCharacterEntity actor,
                                BoardSession session, BoardParticipant participant,
                                CharacterDefinition definition, BoardSkillDefinition skill) {
}
