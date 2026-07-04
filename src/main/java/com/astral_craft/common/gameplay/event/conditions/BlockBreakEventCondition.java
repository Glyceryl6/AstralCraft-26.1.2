package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralActiveEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record BlockBreakEventCondition(List<Identifier> blocks, List<Identifier> blockTags, boolean inverted) implements AstralActiveEventCondition {

    public static final MapCodec<BlockBreakEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(BlockBreakEventCondition::blocks),
            Identifier.CODEC.listOf().optionalFieldOf("block_tags", List.of()).forGetter(BlockBreakEventCondition::blockTags),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(BlockBreakEventCondition::inverted)
    ).apply(instance, BlockBreakEventCondition::new));

    public BlockBreakEventCondition() {
        this(List.of(), List.of(), false);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("block_break").toString();
    }

    @Override
    public MapCodec<? extends AstralActiveEventCondition> activeCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || !context.hasBlockBreak()) return false;
        boolean matches = ActiveEventConditionFilters.matchesBlock(context.blockState(), this.blocks, this.blockTags);
        return this.inverted != matches;
    }

}