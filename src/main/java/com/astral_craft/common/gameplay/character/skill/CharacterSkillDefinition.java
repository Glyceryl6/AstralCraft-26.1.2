package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record CharacterSkillDefinition(String id, int cooldown, int durationSeconds, Identifier handler, String animationAction, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown) {

    public static final Codec<CharacterSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkillDefinition::id),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillDefinition::cooldown),
            Codec.INT.optionalFieldOf("duration_seconds", 0).forGetter(CharacterSkillDefinition::durationSeconds),
            Identifier.CODEC.optionalFieldOf("handler", AstralCraft.prefix("default")).forGetter(CharacterSkillDefinition::handler),
            Codec.STRING.optionalFieldOf("animation", "skill").forGetter(CharacterSkillDefinition::animationAction),
            Codec.BOOL.optionalFieldOf("has_pvp_variant", false).forGetter(CharacterSkillDefinition::hasPvpVariant),
            Codec.BOOL.optionalFieldOf("has_pve_variant", false).forGetter(CharacterSkillDefinition::hasPveVariant),
            Codec.INT.optionalFieldOf("pvp_cooldown", -1).forGetter(CharacterSkillDefinition::pvpCooldown),
            Codec.INT.optionalFieldOf("pve_cooldown", -1).forGetter(CharacterSkillDefinition::pveCooldown)
    ).apply(instance, CharacterSkillDefinition::new));

    public CharacterSkillDefinition {
        id = id == null || id.isBlank() ? "active" : id;
        cooldown = Math.max(0, cooldown);
        durationSeconds = Math.max(0, durationSeconds);
        handler = handler == null ? AstralCraft.prefix("default") : handler;
        animationAction = animationAction == null || animationAction.isBlank() ? "skill" : animationAction;
    }

    public CharacterSkillDefinition(String id, int cooldown) {
        this(id, cooldown, 0, AstralCraft.prefix("default"), "skill", false, false, -1, -1);
    }

    public CharacterSkillDefinition(String id, int cooldown, Identifier handler, String animationAction) {
        this(id, cooldown, 0, handler, animationAction, false, false, -1, -1);
    }

    public boolean hasModeSpecificText() {
        return this.hasPvpSpecificText() || this.hasPveSpecificText();
    }

    public boolean hasPvpSpecificText() {
        return this.hasPvpVariant || this.pvpCooldown >= 0;
    }

    public boolean hasPveSpecificText() {
        return this.hasPveVariant || this.pveCooldown >= 0;
    }

    public int cooldown(SkillMode mode) {
        if (mode == SkillMode.PVE && this.pveCooldown >= 0) return this.pveCooldown;
        if (mode == SkillMode.PVP && this.pvpCooldown >= 0) return this.pvpCooldown;
        return this.cooldown;
    }

    public Identifier safeHandler(Identifier fallback) {
        if (this.handler == null || this.handler.equals(AstralCraft.prefix("default"))) {
            return fallback;
        }
        return this.handler;
    }

    public String safeAnimationAction() {
        return this.animationAction == null || this.animationAction.isBlank() ? "skill" : this.animationAction;
    }

    public enum SkillMode {
        PVP,
        PVE
    }

}