package com.astral_craft.common.gameplay.board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record BoardArea(BlockPos min, BlockPos max) {

    public static final Codec<BoardArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(BoardArea::min),
            BlockPos.CODEC.fieldOf("max").forGetter(BoardArea::max)
    ).apply(instance, BoardArea::new));

    public BoardArea {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        min = new BlockPos(minX, minY, minZ);
        max = new BlockPos(maxX, maxY, maxZ);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= this.min.getX() && pos.getX() <= this.max.getX()
                && pos.getY() >= this.min.getY() && pos.getY() <= this.max.getY()
                && pos.getZ() >= this.min.getZ() && pos.getZ() <= this.max.getZ();
    }

    public boolean intersects(BoardArea other) {
        return this.min.getX() <= other.max.getX() && this.max.getX() >= other.min.getX()
                && this.min.getY() <= other.max.getY() && this.max.getY() >= other.min.getY()
                && this.min.getZ() <= other.max.getZ() && this.max.getZ() >= other.min.getZ();
    }

    public BlockPos center() {
        return new BlockPos((this.min.getX() + this.max.getX()) / 2,
                (this.min.getY() + this.max.getY()) / 2,
                (this.min.getZ() + this.max.getZ()) / 2);
    }

    public BoardArea inflate(int xz, int y) {
        return new BoardArea(this.min.offset(-xz, -y, -xz), this.max.offset(xz, y, xz));
    }

    public int width() {
        return this.max.getX() - this.min.getX() + 1;
    }

    public int depth() {
        return this.max.getZ() - this.min.getZ() + 1;
    }

}
