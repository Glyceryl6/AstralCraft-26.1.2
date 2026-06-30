package com.astral_craft.common.gameplay.character.skill;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum CharacterSkillType {

    ACTIVE("active"),
    PASSIVE("passive");

    public static final Codec<CharacterSkillType> CODEC = Codec.STRING.xmap(CharacterSkillType::byName, CharacterSkillType::serializedName);

    private final String id;

    CharacterSkillType(String id) {
        this.id = id;
    }

    public String serializedName() {
        return this.id;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isPassive() {
        return this == PASSIVE;
    }

    public static CharacterSkillType byName(String raw) {
        if (raw == null) return ACTIVE;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "passive".equals(value) ? PASSIVE : ACTIVE;
    }

    @Override
    public String toString() {
        return this.id;
    }

}