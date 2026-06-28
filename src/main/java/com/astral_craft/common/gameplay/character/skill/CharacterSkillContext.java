package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import net.minecraft.server.level.ServerPlayer;

public record CharacterSkillContext(
        ServerPlayer player,
        ActiveCharacterState state,
        CharacterDefinition definition,
        CharacterSkillDefinition skill,
        CharacterSkillState skillState) {

    public CharacterProgressEntry progressEntry() {
        return CharacterProgressManager.progress(this.player).entry(this.definition.id());
    }

    public boolean potentialActivated() {
        return this.definition.supportsPotential() && this.progressEntry().potentialActivated();
    }

}
