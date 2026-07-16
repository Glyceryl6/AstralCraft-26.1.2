package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CharacterStatsDefinition(int attack, int defense, int health, int initialStarCoins) {

    public static final Codec<CharacterStatsDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("attack", 1).forGetter(CharacterStatsDefinition::attack),
            Codec.INT.optionalFieldOf("defense", 1).forGetter(CharacterStatsDefinition::defense),
            Codec.INT.optionalFieldOf("health", 10).forGetter(CharacterStatsDefinition::health),
            Codec.INT.optionalFieldOf("initial_star_coins", 6).forGetter(CharacterStatsDefinition::initialStarCoins)
    ).apply(instance, CharacterStatsDefinition::new));

    public static CharacterStatsDefinition defaultStats() {
        return new CharacterStatsDefinition(1, 1, 10, 6);
    }

}