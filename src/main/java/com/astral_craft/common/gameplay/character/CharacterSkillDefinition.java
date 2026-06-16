package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CharacterSkillDefinition(String id, String nameKey, String descriptionKey, int cooldown) {

    public static final Codec<CharacterSkillDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkillDefinition::id),
            Codec.STRING.fieldOf("name_key").forGetter(CharacterSkillDefinition::nameKey),
            Codec.STRING.fieldOf("description_key").forGetter(CharacterSkillDefinition::descriptionKey),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CharacterSkillDefinition::cooldown)
    ).apply(instance, CharacterSkillDefinition::new));

}