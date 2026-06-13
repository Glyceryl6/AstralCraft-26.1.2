package com.astral_craft.common.gameplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StatBundle(
        int attack,
        int defense,
        int speed,
        int maxHealth,
        int health,
        int starCoins,
        int cardPlays,
        int skillCooldownReduction,
        int healStacks,
        int starlightStacks,
        int markStacks
) {
    public static final StatBundle EMPTY = new StatBundle(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public static final Codec<StatBundle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("attack", 0).forGetter(StatBundle::attack),
            Codec.INT.optionalFieldOf("defense", 0).forGetter(StatBundle::defense),
            Codec.INT.optionalFieldOf("speed", 0).forGetter(StatBundle::speed),
            Codec.INT.optionalFieldOf("max_health", 0).forGetter(StatBundle::maxHealth),
            Codec.INT.optionalFieldOf("health", 0).forGetter(StatBundle::health),
            Codec.INT.optionalFieldOf("star_coins", 0).forGetter(StatBundle::starCoins),
            Codec.INT.optionalFieldOf("card_plays", 0).forGetter(StatBundle::cardPlays),
            Codec.INT.optionalFieldOf("skill_cooldown_reduction", 0).forGetter(StatBundle::skillCooldownReduction),
            Codec.INT.optionalFieldOf("heal_stacks", 0).forGetter(StatBundle::healStacks),
            Codec.INT.optionalFieldOf("starlight_stacks", 0).forGetter(StatBundle::starlightStacks),
            Codec.INT.optionalFieldOf("mark_stacks", 0).forGetter(StatBundle::markStacks)
    ).apply(instance, StatBundle::new));

    public StatBundle add(StatBundle other) {
        return new StatBundle(
                this.attack + other.attack,
                this.defense + other.defense,
                this.speed + other.speed,
                this.maxHealth + other.maxHealth,
                this.health + other.health,
                this.starCoins + other.starCoins,
                this.cardPlays + other.cardPlays,
                this.skillCooldownReduction + other.skillCooldownReduction,
                this.healStacks + other.healStacks,
                this.starlightStacks + other.starlightStacks,
                this.markStacks + other.markStacks
        );
    }
}
