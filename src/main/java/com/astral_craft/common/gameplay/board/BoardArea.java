package com.astral_craft.common.gameplay.board;

import net.minecraft.core.BlockPos;

public record BoardArea(BlockPos min, BlockPos max) {

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public BlockPos center() {
        return new BlockPos((min.getX() + max.getX()) / 2, max.getY() + 3, (min.getZ() + max.getZ()) / 2);
    }

    public BoardArea inflate(int xz, int y) {
        return new BoardArea(min.offset(-xz, -y, -xz), max.offset(xz, y, xz));
    }

}