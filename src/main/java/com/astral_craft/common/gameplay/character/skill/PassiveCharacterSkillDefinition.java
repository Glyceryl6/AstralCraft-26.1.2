package com.astral_craft.common.gameplay.character.skill;

import net.minecraft.resources.Identifier;

import java.util.Optional;

/** Ordinary-player passive skill metadata. A character may register any number of these. */
public class PassiveCharacterSkillDefinition {

    protected final String id;
    protected int durationSeconds;
    protected Identifier statusEffect;
    protected boolean hasPvpVariant;
    protected boolean hasPveVariant;

    protected PassiveCharacterSkillDefinition(String id) {
        this.id = id == null || id.isBlank() ? "passive" : id;
    }

    public static PassiveCharacterSkillDefinition of(String id) {
        return new PassiveCharacterSkillDefinition(id);
    }

    public PassiveCharacterSkillDefinition durationSeconds(int durationSeconds) {
        this.durationSeconds = Math.max(0, durationSeconds);
        return this;
    }

    public PassiveCharacterSkillDefinition statusEffect(Identifier statusEffect) {
        this.statusEffect = statusEffect;
        return this;
    }

    public PassiveCharacterSkillDefinition pvpVariant() {
        this.hasPvpVariant = true;
        return this;
    }

    public PassiveCharacterSkillDefinition pveVariant() {
        this.hasPveVariant = true;
        return this;
    }

    public String id() {
        return this.id;
    }

    public int durationSeconds() {
        return this.durationSeconds;
    }

    public Optional<Identifier> statusEffectId() {
        return Optional.ofNullable(this.statusEffect);
    }

    public CharacterSkillView view() {
        return new CharacterSkillView(this.id, false, 0, -1, -1, this.hasPvpVariant, this.hasPveVariant);
    }
}
