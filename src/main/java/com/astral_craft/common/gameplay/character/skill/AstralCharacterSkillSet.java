package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public class AstralCharacterSkillSet {

    protected final Identifier characterId;
    protected final AstralCharacterActiveSkill activeSkill;
    protected final List<AstralCharacterPassiveSkill> passiveSkills;
    protected final AstralCharacterSkillEffectHandler effectHandler;
    protected final String fallbackAnimation;

    public AstralCharacterSkillSet(Identifier characterId, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills, String fallbackAnimation) {
        this(characterId, activeSkill, passiveSkills, null, fallbackAnimation);
    }

    public AstralCharacterSkillSet(Identifier characterId, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills, AstralCharacterSkillEffectHandler effectHandler, String fallbackAnimation) {
        this.characterId = characterId == null ? AstralCraft.prefix("default") : characterId;
        this.activeSkill = activeSkill;
        this.passiveSkills = passiveSkills == null ? List.of() : List.copyOf(passiveSkills);
        this.effectHandler = effectHandler == null ? new AstralCharacterSkillEffectHandler() {} : effectHandler;
        this.fallbackAnimation = fallbackAnimation == null || fallbackAnimation.isBlank() ? "skill" : fallbackAnimation;
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

    public void onEffectStart(CharacterSkillContext context, CharacterSkillEffect effect) {
        this.effectHandler.onEffectStart(context, effect);
    }

    public void onEffectTick(CharacterSkillContext context, CharacterSkillEffect effect) {
        this.effectHandler.onEffectTick(context, effect);
    }

    public void onEffectEnd(CharacterSkillContext context, CharacterSkillEffect effect) {
        this.effectHandler.onEffectEnd(context, effect);
    }

    public boolean hasActiveSkill() {
        return this.activeSkill != null;
    }

    public String fallbackAnimation() {
        return this.fallbackAnimation;
    }

}
