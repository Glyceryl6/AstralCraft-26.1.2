package com.astral_craft.common.gameplay.character;

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
