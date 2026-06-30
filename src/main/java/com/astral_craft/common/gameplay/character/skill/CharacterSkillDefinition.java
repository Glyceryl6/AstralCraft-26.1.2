package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralStatusEffects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record CharacterSkillDefinition(CharacterSkillType id, int cooldown, int durationSeconds, Identifier handler, Identifier animation, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown, Identifier statusEffect) {

    public static final Identifier DEFAULT_HANDLER_ID = AstralCraft.prefix("default");
    public static final Identifier DEFAULT_ANIMATION_ID = AstralCraft.prefix("skill");
    public static final Codec<Identifier> ANIMATION_CODEC = Codec.STRING.xmap(raw -> parseIdentifier(raw, DEFAULT_ANIMATION_ID), Identifier::toString);

    public static final Codec<CharacterSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CharacterSkillType.CODEC.fieldOf("id").forGetter(CharacterSkillDefinition::id),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillDefinition::cooldown),
            Codec.INT.optionalFieldOf("duration_seconds", 0).forGetter(CharacterSkillDefinition::durationSeconds),
            Identifier.CODEC.optionalFieldOf("handler", DEFAULT_HANDLER_ID).forGetter(CharacterSkillDefinition::handler),
            ANIMATION_CODEC.optionalFieldOf("animation", DEFAULT_ANIMATION_ID).forGetter(CharacterSkillDefinition::animation),
            Codec.BOOL.optionalFieldOf("has_pvp_variant", false).forGetter(CharacterSkillDefinition::hasPvpVariant),
            Codec.BOOL.optionalFieldOf("has_pve_variant", false).forGetter(CharacterSkillDefinition::hasPveVariant),
            Codec.INT.optionalFieldOf("pvp_cooldown", -1).forGetter(CharacterSkillDefinition::pvpCooldown),
            Codec.INT.optionalFieldOf("pve_cooldown", -1).forGetter(CharacterSkillDefinition::pveCooldown),
            Identifier.CODEC.optionalFieldOf("status_effect", AstralStatusEffects.NO_STATUS_ID).forGetter(CharacterSkillDefinition::statusEffect)
    ).apply(instance, CharacterSkillDefinition::new));

    public CharacterSkillDefinition {
        id = id == null ? CharacterSkillType.ACTIVE : id;
        cooldown = Math.max(0, cooldown);
        durationSeconds = Math.max(0, durationSeconds);
        handler = handler == null ? DEFAULT_HANDLER_ID : handler;
        animation = animation == null ? DEFAULT_ANIMATION_ID : animation;
        statusEffect = statusEffect == null ? AstralStatusEffects.NO_STATUS_ID : statusEffect;
    }

    public CharacterSkillDefinition(CharacterSkillType id, int cooldown) {
        this(id, cooldown, 0, DEFAULT_HANDLER_ID, DEFAULT_ANIMATION_ID, false, false, -1, -1, AstralStatusEffects.NO_STATUS_ID);
    }

    public CharacterSkillDefinition(String id, int cooldown) {
        this(CharacterSkillType.byName(id), cooldown, 0, DEFAULT_HANDLER_ID, DEFAULT_ANIMATION_ID, false, false, -1, -1, AstralStatusEffects.NO_STATUS_ID);
    }

    public CharacterSkillDefinition(String id, int cooldown, Identifier handler, String animation) {
        this(CharacterSkillType.byName(id), cooldown, 0, handler, parseIdentifier(animation, DEFAULT_ANIMATION_ID), false, false, -1, -1, AstralStatusEffects.NO_STATUS_ID);
    }

    public CharacterSkillDefinition(String id, int cooldown, int durationSeconds, Identifier handler, String animation, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown) {
        this(CharacterSkillType.byName(id), cooldown, durationSeconds, handler, parseIdentifier(animation, DEFAULT_ANIMATION_ID), hasPvpVariant, hasPveVariant, pvpCooldown, pveCooldown, AstralStatusEffects.NO_STATUS_ID);
    }

    public CharacterSkillDefinition(String id, int cooldown, int durationSeconds, Identifier handler, String animation, boolean hasPvpVariant, boolean hasPveVariant, int pvpCooldown, int pveCooldown, Identifier statusEffect) {
        this(CharacterSkillType.byName(id), cooldown, durationSeconds, handler, parseIdentifier(animation, DEFAULT_ANIMATION_ID), hasPvpVariant, hasPveVariant, pvpCooldown, pveCooldown, statusEffect);
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
        if (this.handler == null || this.handler.equals(DEFAULT_HANDLER_ID)) {
            return fallback;
        }
        return this.handler;
    }

    public Identifier safeAnimation() {
        return this.animation == null ? DEFAULT_ANIMATION_ID : this.animation;
    }

    public String animationAction() {
        return this.safeAnimation().getPath();
    }

    public Optional<Identifier> statusEffectId() {
        if (this.statusEffect == null || AstralStatusEffects.NO_STATUS_ID.equals(this.statusEffect)) {
            return Optional.empty();
        }
        return Optional.of(this.statusEffect);
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