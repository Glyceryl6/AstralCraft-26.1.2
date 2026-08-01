package com.astral_craft.common.gameplay.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AstralActiveEventInstance(
        String eventId,
        String nameKey,
        String descriptionKey,
        int ticksLeft,
        int totalTicks,
        int intervalLeft,
        int intervalTicks) {

    public static final Codec<AstralActiveEventInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("event_id").forGetter(AstralActiveEventInstance::eventId),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(AstralActiveEventInstance::nameKey),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(AstralActiveEventInstance::descriptionKey),
            Codec.INT.optionalFieldOf("ticks_left", 0).forGetter(AstralActiveEventInstance::ticksLeft),
            Codec.INT.optionalFieldOf("total_ticks", 0).forGetter(AstralActiveEventInstance::totalTicks),
            Codec.INT.optionalFieldOf("interval_left", 20).forGetter(AstralActiveEventInstance::intervalLeft),
            Codec.INT.optionalFieldOf("interval_ticks", 20).forGetter(AstralActiveEventInstance::intervalTicks)
    ).apply(instance, AstralActiveEventInstance::new));

    public static AstralActiveEventInstance create(AstralEventDefinition definition) {
        return new AstralActiveEventInstance(definition.id().toString(),
                definition.nameKey(),
                definition.descriptionKey(),
                definition.safeDurationTicks(),
                definition.safeDurationTicks(),
                definition.safeIntervalTicks(),
                definition.safeIntervalTicks());
    }

    public AstralActiveEventInstance tick() {
        return new AstralActiveEventInstance(this.eventId, this.nameKey, this.descriptionKey,
                Math.max(0, this.ticksLeft - 1), this.totalTicks, Math.max(0, this.intervalLeft - 1), this.intervalTicks);
    }

    public AstralActiveEventInstance resetInterval() {
        return new AstralActiveEventInstance(this.eventId, this.nameKey, this.descriptionKey,
                this.ticksLeft, this.totalTicks, Math.max(1, this.intervalTicks), this.intervalTicks);
    }

    public int secondsLeft() {
        return Math.max(0, (this.ticksLeft + 19) / 20);
    }

}
