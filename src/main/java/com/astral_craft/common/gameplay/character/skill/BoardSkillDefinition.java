package com.astral_craft.common.gameplay.character.skill;

import net.minecraft.resources.Identifier;

/** Board-game active skill metadata, deliberately independent from second-based MobEffect metadata. */
public class BoardSkillDefinition {

    protected final int pvpCooldown;
    protected final int pveCooldown;
    protected Identifier animation;

    protected BoardSkillDefinition(int pvpCooldown, int pveCooldown) {
        this.pvpCooldown = Math.max(0, pvpCooldown);
        this.pveCooldown = Math.max(0, pveCooldown);
    }

    public static BoardSkillDefinition cooldown(int cooldown) {
        return new BoardSkillDefinition(cooldown, cooldown);
    }

    public static BoardSkillDefinition cooldown(int pvpCooldown, int pveCooldown) {
        return new BoardSkillDefinition(pvpCooldown, pveCooldown);
    }

    public BoardSkillDefinition animation(Identifier animation) {
        this.animation = animation;
        return this;
    }

    public int pvpCooldown() {
        return this.pvpCooldown;
    }

    public int pveCooldown() {
        return this.pveCooldown;
    }

    public Identifier safeAnimation(Identifier fallback) {
        return this.animation == null ? fallback : this.animation;
    }

}