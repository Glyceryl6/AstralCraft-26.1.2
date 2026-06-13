package com.astral_craft.common.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TimedStatModifier(String stat, int amount, int turns) {

    public static final Codec<TimedStatModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("stat").forGetter(TimedStatModifier::stat),
            Codec.INT.fieldOf("amount").forGetter(TimedStatModifier::amount),
            Codec.INT.fieldOf("turns").forGetter(TimedStatModifier::turns)
    ).apply(instance, TimedStatModifier::new));

    public TimedStatModifier tickDown() {
        return new TimedStatModifier(this.stat, this.amount, this.turns - 1);
    }

    public boolean active() {
        return this.turns != 0;
    }

}