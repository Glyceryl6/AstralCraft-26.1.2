package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TimeOfDayEventCondition(long min, long max) implements AstralEventGeneralCondition {

    public static final MapCodec<TimeOfDayEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.optionalFieldOf("min", 0L).forGetter(TimeOfDayEventCondition::min),
            Codec.LONG.optionalFieldOf("max", 23999L).forGetter(TimeOfDayEventCondition::max)
    ).apply(instance, TimeOfDayEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("time_of_day").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.level() == null) return false;
        long time = context.level().getDefaultClockTime() % 24000L;
        long safeMin = Math.max(0L, this.min % 24000L);
        long safeMax = Math.max(0L, this.max % 24000L);
        if (safeMin <= safeMax) {
            return time >= safeMin && time <= safeMax;
        }
        return time >= safeMin || time <= safeMax;
    }

}