package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record CharacterPotentialDefinition(
        boolean enabled,
        int requiredLevel,
        int requiredFriendship,
        int requiredExperience,
        List<CharacterPotentialMaterialRequirement> materialRequirements) {

    public static final List<CharacterPotentialMaterialRequirement> DEFAULT_MATERIALS = List.of(
            new CharacterPotentialMaterialRequirement(Identifier.withDefaultNamespace("gold_ingot"), 16),
            new CharacterPotentialMaterialRequirement(Identifier.withDefaultNamespace("emerald"), 4),
            new CharacterPotentialMaterialRequirement(Identifier.withDefaultNamespace("diamond"), 1));
    public static final CharacterPotentialDefinition NONE = new CharacterPotentialDefinition(false, 0, 0, 0, List.of());

    public static final Codec<CharacterPotentialDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CharacterPotentialDefinition::enabled),
            Codec.INT.optionalFieldOf("required_level", 1).forGetter(CharacterPotentialDefinition::requiredLevel),
            Codec.INT.optionalFieldOf("required_friendship", 1).forGetter(CharacterPotentialDefinition::requiredFriendship),
            Codec.INT.optionalFieldOf("required_experience", 0).forGetter(CharacterPotentialDefinition::requiredExperience),
            CharacterPotentialMaterialRequirement.CODEC.listOf().optionalFieldOf("materials", DEFAULT_MATERIALS).forGetter(CharacterPotentialDefinition::materialRequirements)
    ).apply(instance, CharacterPotentialDefinition::new));

    public CharacterPotentialDefinition {
        if (!enabled) {
            requiredLevel = 0;
            requiredFriendship = 0;
            requiredExperience = 0;
            materialRequirements = List.of();
        } else {
            requiredLevel = Math.max(1, requiredLevel);
            requiredFriendship = Math.max(1, requiredFriendship);
            requiredExperience = Math.max(0, requiredExperience);
            materialRequirements = materialRequirements == null || materialRequirements.isEmpty() ? DEFAULT_MATERIALS : List.copyOf(materialRequirements);
        }
    }

    public static CharacterPotentialDefinition create(int requiredFriendship, int requiredExperience, List<CharacterPotentialMaterialRequirement> materialRequirements) {
        return new CharacterPotentialDefinition(true, 1, requiredFriendship, requiredExperience, materialRequirements);
    }

    public static CharacterPotentialDefinition defaultRequirement() {
        return new CharacterPotentialDefinition(true, 1, 1, 0, DEFAULT_MATERIALS);
    }

    public boolean canActivate(CharacterProgressEntry entry) {
        if (!this.enabled || entry == null || !entry.unlocked()) return false;
        if (entry.potentialActivated()) return false;
        if (entry.level() < this.requiredLevel) return false;
        if (entry.friendship() < this.requiredFriendship) return false;
        return entry.experience() >= this.requiredExperience;
    }

    public boolean canActivate(CharacterProgressEntry entry, Player player) {
        if (!this.canActivate(entry)) return false;
        if (entry.level() < CharacterProgressEntry.MAX_PVE_LEVEL) return false;
        return this.hasMaterials(player);
    }

    public boolean hasMaterials(Player player) {
        if (!this.enabled) return false;
        for (CharacterPotentialMaterialRequirement requirement : this.materialRequirements) {
            if (!requirement.satisfied(player)) return false;
        }
        return true;
    }

    public void consumeMaterials(Player player) {
        if (!this.enabled) return;
        for (CharacterPotentialMaterialRequirement requirement : this.materialRequirements) {
            requirement.consume(player);
        }
    }

    public boolean hasRequirement() {
        return this.requiredFriendship > 1 || this.requiredExperience > 0 || !this.materialRequirements.isEmpty();
    }

}
