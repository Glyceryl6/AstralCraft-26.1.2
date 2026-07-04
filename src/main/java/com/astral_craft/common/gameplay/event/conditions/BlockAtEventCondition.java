package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

public record BlockAtEventCondition(List<Identifier> blocks, int offsetX, int offsetY, int offsetZ, boolean inverted) implements AstralEventGeneralCondition {

    public static final MapCodec<BlockAtEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(BlockAtEventCondition::blocks),
            Codec.INT.optionalFieldOf("offset_x", 0).forGetter(BlockAtEventCondition::offsetX),
            Codec.INT.optionalFieldOf("offset_y", 0).forGetter(BlockAtEventCondition::offsetY),
            Codec.INT.optionalFieldOf("offset_z", 0).forGetter(BlockAtEventCondition::offsetZ),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(BlockAtEventCondition::inverted)
    ).apply(instance, BlockAtEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("block_at").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.level() == null || context.origin() == null) return this.inverted;
        BlockPos pos = context.origin().offset(this.offsetX, this.offsetY, this.offsetZ);
        Identifier current = BuiltInRegistries.BLOCK.getKey(context.level().getBlockState(pos).getBlock());
        boolean matches = this.blocks.isEmpty() || this.blocks.contains(current);
        return this.inverted != matches;
    }

}