package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record CharacterSkillDefinition(
        String id,
        String nameKey,
        String descriptionKey,
        int cooldown,
        int cooldownSeconds,
        Identifier handler,
        String animationAction,
        String pvpNameKey,
        String pvpDescriptionKey,
        int pvpCooldown,
        int pvpCooldownSeconds,
        String pveNameKey,
        String pveDescriptionKey,
        int pveCooldown,
        int pveCooldownSeconds) {

    public static final Codec<CharacterSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkillDefinition::id),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(CharacterSkillDefinition::nameKey),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(CharacterSkillDefinition::descriptionKey),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillDefinition::cooldown),
            Codec.INT.optionalFieldOf("cooldown_seconds", 0).forGetter(CharacterSkillDefinition::cooldownSeconds),
            Identifier.CODEC.optionalFieldOf("handler", AstralCraft.prefix("default")).forGetter(CharacterSkillDefinition::handler),
            Codec.STRING.optionalFieldOf("animation", "skill").forGetter(CharacterSkillDefinition::animationAction),
            Codec.STRING.optionalFieldOf("pvp_name_key", "").forGetter(CharacterSkillDefinition::pvpNameKey),
            Codec.STRING.optionalFieldOf("pvp_description_key", "").forGetter(CharacterSkillDefinition::pvpDescriptionKey),
            Codec.INT.optionalFieldOf("pvp_cooldown", -1).forGetter(CharacterSkillDefinition::pvpCooldown),
            Codec.INT.optionalFieldOf("pvp_cooldown_seconds", -1).forGetter(CharacterSkillDefinition::pvpCooldownSeconds),
            Codec.STRING.optionalFieldOf("pve_name_key", "").forGetter(CharacterSkillDefinition::pveNameKey),
            Codec.STRING.optionalFieldOf("pve_description_key", "").forGetter(CharacterSkillDefinition::pveDescriptionKey),
            Codec.INT.optionalFieldOf("pve_cooldown", -1).forGetter(CharacterSkillDefinition::pveCooldown),
            Codec.INT.optionalFieldOf("pve_cooldown_seconds", -1).forGetter(CharacterSkillDefinition::pveCooldownSeconds)
    ).apply(instance, CharacterSkillDefinition::new));

    public CharacterSkillDefinition(String id, String nameKey, String descriptionKey, int cooldown) {
        this(id, nameKey, descriptionKey, cooldown, 0, AstralCraft.prefix("default"), "skill", "", "", -1, -1, "", "", -1, -1);
    }

    public CharacterSkillDefinition(String id, String nameKey, String descriptionKey, int cooldown, int cooldownSeconds, Identifier handler, String animationAction) {
        this(id, nameKey, descriptionKey, cooldown, cooldownSeconds, handler, animationAction, "", "", -1, -1, "", "", -1, -1);
    }

    public boolean hasModeSpecificText() {
        return this.hasPvpSpecificText() || this.hasPveSpecificText();
    }

    public boolean hasPvpSpecificText() {
        return !this.pvpNameKey.isBlank() || !this.pvpDescriptionKey.isBlank() || this.pvpCooldown >= 0 || this.pvpCooldownSeconds >= 0;
    }

    public boolean hasPveSpecificText() {
        return !this.pveNameKey.isBlank() || !this.pveDescriptionKey.isBlank() || this.pveCooldown >= 0 || this.pveCooldownSeconds >= 0;
    }

    public String nameKey(SkillMode mode) {
        if (mode == SkillMode.PVE && !this.pveNameKey.isBlank()) return this.pveNameKey;
        if (mode == SkillMode.PVP && !this.pvpNameKey.isBlank()) return this.pvpNameKey;
        return this.nameKey;
    }

    public String descriptionKey(SkillMode mode) {
        if (mode == SkillMode.PVE && !this.pveDescriptionKey.isBlank()) return this.pveDescriptionKey;
        if (mode == SkillMode.PVP && !this.pvpDescriptionKey.isBlank()) return this.pvpDescriptionKey;
        return this.descriptionKey;
    }

    public int cooldown(SkillMode mode) {
        if (mode == SkillMode.PVE && this.pveCooldown >= 0) return this.pveCooldown;
        if (mode == SkillMode.PVP && this.pvpCooldown >= 0) return this.pvpCooldown;
        return this.cooldown;
    }

    public int cooldownSeconds(SkillMode mode) {
        if (mode == SkillMode.PVE && this.pveCooldownSeconds >= 0) return this.pveCooldownSeconds;
        if (mode == SkillMode.PVP && this.pvpCooldownSeconds >= 0) return this.pvpCooldownSeconds;
        return this.cooldownSeconds;
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
