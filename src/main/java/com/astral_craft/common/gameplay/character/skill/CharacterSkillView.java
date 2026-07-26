package com.astral_craft.common.gameplay.character.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Network/UI projection of a registered character skill. Runtime behaviour stays in the character class. */
public record CharacterSkillView(
        String id, boolean active, int cooldown, int pvpCooldown, int pveCooldown,
        boolean hasPvpVariant, boolean hasPveVariant) {

    public static final Codec<CharacterSkillView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkillView::id),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(CharacterSkillView::active),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillView::cooldown),
            Codec.INT.optionalFieldOf("pvp_cooldown", -1).forGetter(CharacterSkillView::pvpCooldown),
            Codec.INT.optionalFieldOf("pve_cooldown", -1).forGetter(CharacterSkillView::pveCooldown),
            Codec.BOOL.optionalFieldOf("has_pvp_variant", false).forGetter(CharacterSkillView::hasPvpVariant),
            Codec.BOOL.optionalFieldOf("has_pve_variant", false).forGetter(CharacterSkillView::hasPveVariant)
    ).apply(instance, CharacterSkillView::new));

    public CharacterSkillView {
        id = id == null || id.isBlank() ? (active ? "active" : "passive") : id;
        cooldown = Math.max(0, cooldown);
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

    public String serializedId() {
        return this.id;
    }

    public enum SkillMode {
        PVP,
        PVE
    }
}
