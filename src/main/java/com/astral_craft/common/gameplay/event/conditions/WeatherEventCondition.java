package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WeatherEventCondition(boolean raining, boolean thundering, boolean requireExact) implements AstralEventGeneralCondition {

    public static final MapCodec<WeatherEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("raining", false).forGetter(WeatherEventCondition::raining),
            Codec.BOOL.optionalFieldOf("thundering", false).forGetter(WeatherEventCondition::thundering),
            Codec.BOOL.optionalFieldOf("require_exact", false).forGetter(WeatherEventCondition::requireExact)
    ).apply(instance, WeatherEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("weather").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.level() == null) return false;
        if (this.requireExact) {
            return context.level().isRaining() == this.raining && context.level().isThundering() == this.thundering;
        }

        if (this.thundering && !context.level().isThundering()) return false;
        if (this.raining && !context.level().isRaining()) return false;
        if (!this.raining && !this.thundering) return !context.level().isRaining() && !context.level().isThundering();
        return true;
    }

}