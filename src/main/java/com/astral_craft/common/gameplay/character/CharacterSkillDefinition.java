package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CharacterSkillDefinition(
        String id,
        String nameKey,
        String descriptionKey,
        int cooldown,
        String pvpNameKey,
        String pvpDescriptionKey,
        int pvpCooldown,
        String pveNameKey,
        String pveDescriptionKey,
        int pveCooldown) {

    public static final Codec<CharacterSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkillDefinition::id),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(CharacterSkillDefinition::nameKey),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(CharacterSkillDefinition::descriptionKey),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillDefinition::cooldown),
            Codec.STRING.optionalFieldOf("pvp_name_key", "").forGetter(CharacterSkillDefinition::pvpNameKey),
            Codec.STRING.optionalFieldOf("pvp_description_key", "").forGetter(CharacterSkillDefinition::pvpDescriptionKey),
            Codec.INT.optionalFieldOf("pvp_cooldown", -1).forGetter(CharacterSkillDefinition::pvpCooldown),
            Codec.STRING.optionalFieldOf("pve_name_key", "").forGetter(CharacterSkillDefinition::pveNameKey),
            Codec.STRING.optionalFieldOf("pve_description_key", "").forGetter(CharacterSkillDefinition::pveDescriptionKey),
            Codec.INT.optionalFieldOf("pve_cooldown", -1).forGetter(CharacterSkillDefinition::pveCooldown)
    ).apply(instance, CharacterSkillDefinition::new));

    public CharacterSkillDefinition(String id, String nameKey, String descriptionKey, int cooldown) {
        this(id, nameKey, descriptionKey, cooldown, "", "", -1, "", "", -1);
    }

    public boolean hasModeSpecificText() {
        return this.hasPvpSpecificText() || this.hasPveSpecificText();
    }

    public boolean hasPvpSpecificText() {
        return !this.pvpNameKey.isBlank() || !this.pvpDescriptionKey.isBlank() || this.pvpCooldown >= 0;
    }

    public boolean hasPveSpecificText() {
        return !this.pveNameKey.isBlank() || !this.pveDescriptionKey.isBlank() || this.pveCooldown >= 0;
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

    public enum SkillMode {
        PVP,
        PVE
    }

}