package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.blocks.BasePlatform;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compact board platform layout stored directly on a board projector ItemStack. */
public record BoardTemplateData(int width, int depth, int panelCount, List<TemplateBlock> blocks) {

    public static final int MAX_FOOTPRINT_CELLS = 32768;

    public static final Codec<BoardTemplateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, BoardScanner.MAX_SCAN_NODES).fieldOf("width").forGetter(BoardTemplateData::width),
            Codec.intRange(1, BoardScanner.MAX_SCAN_NODES).fieldOf("depth").forGetter(BoardTemplateData::depth),
            Codec.intRange(1, BoardScanner.MAX_SCAN_NODES).fieldOf("panel_count").forGetter(BoardTemplateData::panelCount),
            TemplateBlock.CODEC.listOf().fieldOf("blocks").forGetter(BoardTemplateData::blocks)
    ).apply(instance, BoardTemplateData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardTemplateData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public BoardTemplateData {
        width = Math.clamp(width, 1, BoardScanner.MAX_SCAN_NODES);
        depth = Math.clamp(depth, 1, BoardScanner.MAX_SCAN_NODES);
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
        panelCount = Math.clamp(panelCount, 1, Math.max(1, blocks.size()));
    }

    public boolean valid() {
        if (this.blocks.isEmpty() || this.blocks.size() > BoardScanner.MAX_SCAN_NODES
                || this.panelCount != this.blocks.size()
                || (long) this.width * this.depth > MAX_FOOTPRINT_CELLS) return false;
        Set<BlockPos> offsets = new HashSet<>();
        for (TemplateBlock block : this.blocks) {
            BlockPos offset = block.offset();
            if (offset.getY() != 0 || offset.getX() < 0 || offset.getX() >= this.width
                    || offset.getZ() < 0 || offset.getZ() >= this.depth
                    || !(block.state().getBlock() instanceof BasePlatform) || !offsets.add(offset)) return false;
        }
        return true;
    }

    public record TemplateBlock(BlockPos offset, BlockState state) {

        public static final Codec<TemplateBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(TemplateBlock::offset),
                BlockState.CODEC.fieldOf("state").forGetter(TemplateBlock::state)
        ).apply(instance, TemplateBlock::new));

        public TemplateBlock {
            offset = offset.immutable();
        }
    }

}