package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record PositionEventCondition(int minY, int maxY, int centerX, int centerZ, int radius) implements AstralEventGeneralCondition {

    public static final MapCodec<PositionEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("min_y", Integer.MIN_VALUE).forGetter(PositionEventCondition::minY),
            Codec.INT.optionalFieldOf("max_y", Integer.MAX_VALUE).forGetter(PositionEventCondition::maxY),
            Codec.INT.optionalFieldOf("center_x", 0).forGetter(PositionEventCondition::centerX),
            Codec.INT.optionalFieldOf("center_z", 0).forGetter(PositionEventCondition::centerZ),
            Codec.INT.optionalFieldOf("radius", -1).forGetter(PositionEventCondition::radius)
    ).apply(instance, PositionEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("position").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.origin() == null) return false;
        BlockPos pos = context.origin();
        if (pos.getY() < this.minY || pos.getY() > this.maxY) return false;
        if (this.radius < 0) return true;
        long dx = pos.getX() - this.centerX;
        long dz = pos.getZ() - this.centerZ;
        long radiusSquared = (long) this.radius * (long) this.radius;
        return dx * dx + dz * dz <= radiusSquared;
    }

}