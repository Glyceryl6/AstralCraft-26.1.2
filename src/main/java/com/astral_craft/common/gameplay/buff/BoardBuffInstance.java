package com.astral_craft.common.gameplay.buff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardBuffInstance(int duration, int amplifier) {

    public static final int PERMANENT = -1;
    public static final Codec<BoardBuffInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("duration", PERMANENT).forGetter(BoardBuffInstance::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(BoardBuffInstance::amplifier)
    ).apply(instance, BoardBuffInstance::new));

    public BoardBuffInstance {
        duration = duration == PERMANENT ? PERMANENT : Math.max(1, duration);
        amplifier = Math.max(0, amplifier);
    }

    public int level() {
        return this.amplifier + 1;
    }

    public boolean permanent() {
        return this.duration == PERMANENT;
    }

    public BoardBuffInstance tickDown() {
        return this.permanent() ? this : new BoardBuffInstance(Math.max(1, this.duration - 1), this.amplifier);
    }

}