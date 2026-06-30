package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public class AstralCharacterSkillSet {

    protected final Identifier characterId;
    protected final AstralCharacterActiveSkill activeSkill;
    protected final List<AstralCharacterPassiveSkill> passiveSkills;
    protected final Identifier fallbackAnimation;

    public AstralCharacterSkillSet(Identifier characterId, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills, Identifier fallbackAnimation) {
        this.characterId = characterId == null ? AstralCraft.prefix("default") : characterId;
        this.activeSkill = activeSkill;
        this.passiveSkills = passiveSkills == null ? List.of() : List.copyOf(passiveSkills);
        this.fallbackAnimation = fallbackAnimation == null ? CharacterSkillDefinition.DEFAULT_ANIMATION_ID : fallbackAnimation;
    }

    public Identifier characterId() {
        return this.characterId;
    }

    public boolean useActive(CharacterSkillContext context) {
        if (this.activeSkill == null) {
            return false;
        }
        return this.activeSkill.use(context);
    }

    public void serverTick(CharacterSkillContext context) {
        for (AstralCharacterPassiveSkill passiveSkill : this.passiveSkills) {
            passiveSkill.serverTick(context);
        }
    }

    public boolean hasActiveSkill() {
        return this.activeSkill != null;
    }

    public Identifier fallbackAnimation() {
        return this.fallbackAnimation;
    }

}
