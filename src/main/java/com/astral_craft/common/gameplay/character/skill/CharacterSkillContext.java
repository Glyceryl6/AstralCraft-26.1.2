package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public record CharacterSkillContext(
        ServerPlayer player,
        LivingEntity actor,
        ActiveCharacterState state,
        CharacterDefinition definition,
        ActiveCharacterSkillDefinition skill,
        CharacterSkillState skillState) {

    public CharacterProgressEntry progressEntry() {
        return CharacterProgressManager.progress(this.player).entry(this.definition.id());
    }

    public boolean potentialActivated() {
        return this.definition.supportsPotential() && this.progressEntry().potentialActivated();
    }

}
