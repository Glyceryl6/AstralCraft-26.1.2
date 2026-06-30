package com.astral_craft.common.gameplay.character.skill;

import java.util.Locale;

public enum CharacterSkillCutinAudience {

    NONE("none"),
    OWNER_ONLY("self"),
    NEARBY("nearby");

    private final String id;

    CharacterSkillCutinAudience(String id) {
        this.id = id;
    }

    public String serializedName() {
        return this.id;
    }

    public boolean sendsToOwnerOnly() {
        return this == OWNER_ONLY;
    }

    public boolean sendsToNearbyPlayers() {
        return this == NEARBY;
    }

    public static CharacterSkillCutinAudience byName(String raw) {
        if (raw == null) return OWNER_ONLY;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "none", "off", "disabled" -> NONE;
            case "nearby", "tracking", "around", "public" -> NEARBY;
            default -> OWNER_ONLY;
        };
    }

    @Override
    public String toString() {
        return this.id;
    }

}