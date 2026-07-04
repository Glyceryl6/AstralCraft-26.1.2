package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;

public record HealthEventCondition(float min, float max, float maxPercent) implements AstralEventGeneralCondition {

    public static final MapCodec<HealthEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("min", 0.0F).forGetter(HealthEventCondition::min),
            Codec.FLOAT.optionalFieldOf("max", Float.MAX_VALUE).forGetter(HealthEventCondition::max),
            Codec.FLOAT.optionalFieldOf("max_percent", 1.0F).forGetter(HealthEventCondition::maxPercent)
    ).apply(instance, HealthEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("health").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        LivingEntity target = context.targetLiving();
        if (target == null) return false;
        float health = target.getHealth();
        if (health < this.min || health > this.max) return false;
        if (this.maxPercent < 1.0F && target.getMaxHealth() > 0.0F) {
            return health / target.getMaxHealth() <= Math.max(0.0F, this.maxPercent);
        }
        return true;
    }
}
