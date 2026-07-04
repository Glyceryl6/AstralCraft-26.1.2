package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record DimensionEventCondition(List<Identifier> dimensions, boolean inverted) implements AstralEventGeneralCondition {

    public static final MapCodec<DimensionEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("dimensions", List.of()).forGetter(DimensionEventCondition::dimensions),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(DimensionEventCondition::inverted)
    ).apply(instance, DimensionEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("dimension").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.level() == null || this.dimensions.isEmpty()) return !this.inverted;
        Identifier current = context.level().dimension().identifier();
        boolean matches = this.dimensions.contains(current);
        return this.inverted != matches;
    }

}