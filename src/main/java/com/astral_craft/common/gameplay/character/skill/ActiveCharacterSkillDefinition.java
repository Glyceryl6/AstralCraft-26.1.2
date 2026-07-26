package com.astral_craft.common.gameplay.character.skill;

import net.minecraft.resources.Identifier;

import java.util.Optional;

/** Ordinary-player active skill metadata. Every registered character owns exactly one instance. */
public class ActiveCharacterSkillDefinition {

    protected int cooldown;
    protected int pvpCooldown = -1;
    protected int pveCooldown = -1;
    protected int durationSeconds;
    protected Identifier statusEffect;
    protected Identifier animation;
    protected boolean hasPvpVariant;
    protected boolean hasPveVariant;

    protected ActiveCharacterSkillDefinition(int cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public static ActiveCharacterSkillDefinition cooldown(int cooldown) {
        return new ActiveCharacterSkillDefinition(cooldown);
    }

    public static ActiveCharacterSkillDefinition cooldown(int pvpCooldown, int pveCooldown) {
        return new ActiveCharacterSkillDefinition(0).modeCooldowns(pvpCooldown, pveCooldown);
    }

    public ActiveCharacterSkillDefinition modeCooldowns(int pvpCooldown, int pveCooldown) {
        this.pvpCooldown = Math.max(0, pvpCooldown);
        this.pveCooldown = Math.max(0, pveCooldown);
        return this;
    }

    public ActiveCharacterSkillDefinition durationSeconds(int durationSeconds) {
        this.durationSeconds = Math.max(0, durationSeconds);
        return this;
    }

    public ActiveCharacterSkillDefinition statusEffect(Identifier statusEffect) {
        this.statusEffect = statusEffect;
        return this;
    }

    public ActiveCharacterSkillDefinition animation(Identifier animation) {
        this.animation = animation;
        return this;
    }

    public ActiveCharacterSkillDefinition pvpVariant() {
        this.hasPvpVariant = true;
        return this;
    }

    public ActiveCharacterSkillDefinition pveVariant() {
        this.hasPveVariant = true;
        return this;
    }

    public int cooldown(CharacterSkillView.SkillMode mode) {
        if (mode == CharacterSkillView.SkillMode.PVE && this.pveCooldown >= 0) return this.pveCooldown;
        if (mode == CharacterSkillView.SkillMode.PVP && this.pvpCooldown >= 0) return this.pvpCooldown;
        return this.cooldown;
    }

    public int durationSeconds() {
        return this.durationSeconds;
    }

    public Optional<Identifier> statusEffectId() {
        return Optional.ofNullable(this.statusEffect);
    }

    public Identifier safeAnimation(Identifier fallback) {
        return this.animation == null ? fallback : this.animation;
    }

    public CharacterSkillView view() {
        return new CharacterSkillView("active", true, this.cooldown, this.pvpCooldown, this.pveCooldown, this.hasPvpVariant, this.hasPveVariant);
    }

}