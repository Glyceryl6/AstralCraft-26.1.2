package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record CharacterSkillDefinition(CharacterSkillType id, int cooldown, int durationSeconds, Identifier handler, Identifier animation, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown, Identifier statusEffect) {

    public static final Codec<Identifier> ANIMATION_CODEC = Codec.STRING.comapFlatMap(raw -> {
        Identifier id = parseIdentifier(raw, null);
        return id == null ? DataResult.error(() -> "Invalid skill animation identifier: " + raw) : DataResult.success(id);
    }, Identifier::toString);

    public static final Codec<CharacterSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CharacterSkillType.CODEC.fieldOf("id").forGetter(CharacterSkillDefinition::id),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillDefinition::cooldown),
            Codec.INT.optionalFieldOf("duration_seconds", 0).forGetter(CharacterSkillDefinition::durationSeconds),
            Identifier.CODEC.optionalFieldOf("handler").forGetter(skill -> Optional.ofNullable(skill.handler())),
            ANIMATION_CODEC.optionalFieldOf("animation").forGetter(skill -> Optional.ofNullable(skill.animation())),
            Codec.BOOL.optionalFieldOf("has_pvp_variant", false).forGetter(CharacterSkillDefinition::hasPvpVariant),
            Codec.BOOL.optionalFieldOf("has_pve_variant", false).forGetter(CharacterSkillDefinition::hasPveVariant),
            Codec.INT.optionalFieldOf("pvp_cooldown", -1).forGetter(CharacterSkillDefinition::pvpCooldown),
            Codec.INT.optionalFieldOf("pve_cooldown", -1).forGetter(CharacterSkillDefinition::pveCooldown),
            Identifier.CODEC.optionalFieldOf("status_effect").forGetter(skill -> Optional.ofNullable(skill.statusEffect()))
    ).apply(instance, (id, cooldown, durationSeconds, handler, animation, hasPvpVariant, hasPveVariant, pvpCooldown, pveCooldown, statusEffect) ->
            new CharacterSkillDefinition(id, cooldown, durationSeconds, handler.orElse(null), animation.orElse(null),
                    hasPvpVariant, hasPveVariant, pvpCooldown, pveCooldown, statusEffect.orElse(null))));

    public CharacterSkillDefinition {
        id = id == null ? CharacterSkillType.ACTIVE : id;
        cooldown = Math.max(0, cooldown);
        durationSeconds = Math.max(0, durationSeconds);
    }

    public CharacterSkillDefinition(CharacterSkillType id, int cooldown) {
        this(id, cooldown, 0, null, null, false, false, -1, -1, null);
    }

    public CharacterSkillDefinition(String id, int cooldown) {
        this(CharacterSkillType.byName(id), cooldown, 0, null, null, false, false, -1, -1, null);
    }

    public CharacterSkillDefinition(String id, int cooldown, Identifier handler, String animation) {
        this(CharacterSkillType.byName(id), cooldown, 0, handler, parseIdentifier(animation, null), false, false, -1, -1, null);
    }

    public CharacterSkillDefinition(String id, int cooldown, int durationSeconds, Identifier handler, String animation, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown) {
        this(CharacterSkillType.byName(id), cooldown, durationSeconds, handler, parseIdentifier(animation, null), hasPvpVariant, hasPveVariant, pvpCooldown, pveCooldown, null);
    }

    public CharacterSkillDefinition(String id, int cooldown, int durationSeconds, Identifier handler, String animation, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown, Identifier statusEffect) {
        this(CharacterSkillType.byName(id), cooldown, durationSeconds, handler, parseIdentifier(animation, null), hasPvpVariant, hasPveVariant, pvpCooldown, pveCooldown, statusEffect);
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
        return this.handler == null ? fallback : this.handler;
    }

    public Identifier safeAnimation(Identifier fallback) {
        return this.animation == null ? fallback : this.animation;
    }

    public Optional<Identifier> statusEffectId() {
        return Optional.ofNullable(this.statusEffect);
    }

    public String serializedId() {
        return this.id.serializedName();
    }

    public static Identifier parseIdentifier(String raw, Identifier fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return raw.contains(":") ? Identifier.parse(raw) : AstralCraft.prefix(raw);
        } catch (Exception exception) {
            return fallback;
        }
    }

    public enum SkillMode {
        PVP,
        PVE
    }

}
