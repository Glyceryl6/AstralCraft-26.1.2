package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CharacterPotentialDefinition(
        boolean enabled,
        String descriptionKey,
        String effectKey,
        int requiredLevel,
        int requiredFriendship,
        int requiredExperience) {

    public static final CharacterPotentialDefinition NONE = new CharacterPotentialDefinition(false, "", "", 0, 0, 0);

    public static final Codec<CharacterPotentialDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CharacterPotentialDefinition::enabled),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(CharacterPotentialDefinition::descriptionKey),
            Codec.STRING.optionalFieldOf("effect_key", "").forGetter(CharacterPotentialDefinition::effectKey),
            Codec.INT.optionalFieldOf("required_level", 1).forGetter(CharacterPotentialDefinition::requiredLevel),
            Codec.INT.optionalFieldOf("required_friendship", 1).forGetter(CharacterPotentialDefinition::requiredFriendship),
            Codec.INT.optionalFieldOf("required_experience", 0).forGetter(CharacterPotentialDefinition::requiredExperience)
    ).apply(instance, CharacterPotentialDefinition::new));

    public CharacterPotentialDefinition {
        if (!enabled) {
            descriptionKey = "";
            effectKey = "";
            requiredLevel = 0;
            requiredFriendship = 0;
            requiredExperience = 0;
        } else {
            descriptionKey = descriptionKey == null ? "" : descriptionKey;
            effectKey = effectKey == null ? "" : effectKey;
            requiredLevel = Math.max(1, requiredLevel);
            requiredFriendship = Math.max(1, requiredFriendship);
            requiredExperience = Math.max(0, requiredExperience);
        }
    }

    public static CharacterPotentialDefinition of(String descriptionKey, String effectKey, int requiredLevel, int requiredFriendship, int requiredExperience) {
        return new CharacterPotentialDefinition(true, descriptionKey, effectKey, requiredLevel, requiredFriendship, requiredExperience);
    }

    public static CharacterPotentialDefinition defaultRequirement() {
        return new CharacterPotentialDefinition(true, "", "", 1, 1, 0);
    }

    public CharacterPotentialDefinition withLocalizationKeys(String descriptionKey, String effectKey) {
        if (!this.enabled) return NONE;
        return new CharacterPotentialDefinition(true, descriptionKey, effectKey, this.requiredLevel, this.requiredFriendship, this.requiredExperience);
    }

    public boolean canActivate(CharacterProgressEntry entry) {
        if (!this.enabled || entry == null || !entry.unlocked()) return false;
        if (entry.potentialActivated()) return false;
        if (entry.level() < this.requiredLevel) return false;
        if (entry.friendship() < this.requiredFriendship) return false;
        return entry.experience() >= this.requiredExperience;
    }

    public boolean hasRequirement() {
        return this.requiredLevel > 1 || this.requiredFriendship > 1 || this.requiredExperience > 0;
    }

}
