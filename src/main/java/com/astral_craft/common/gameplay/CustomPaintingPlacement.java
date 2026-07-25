package com.astral_craft.common.gameplay;

import com.astral_craft.common.components.CustomPaintingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CustomPaintingPlacement {

    public static Vec3 center(BlockPos supportPos, Direction facing, CustomPaintingData data) {
        Direction side = facing.getCounterClockWise();
        int horizontalStart = -data.width() / 2;
        double sideOffset = horizontalStart + (data.width() - 1) * 0.5D;
        return Vec3.atCenterOf(supportPos)
                .add(side.getStepX() * sideOffset, (data.height() - 1) * 0.5D, side.getStepZ() * sideOffset)
                .add(facing.getStepX() * 0.53125D, 0.0D, facing.getStepZ() * 0.53125D);
    }

    public static AABB boundingBox(BlockPos supportPos, Direction facing, CustomPaintingData data) {
        Vec3 center = center(supportPos, facing, data);
        double halfWidth = data.width() * 0.5D;
        double halfHeight = data.height() * 0.5D;
        double thickness = 0.03125D;
        if (facing.getAxis() == Direction.Axis.Z) {
            return new AABB(center.x - halfWidth, center.y - halfHeight, center.z - thickness,
                    center.x + halfWidth, center.y + halfHeight, center.z + thickness);
        }
        return new AABB(center.x - thickness, center.y - halfHeight, center.z - halfWidth,
                center.x + thickness, center.y + halfHeight, center.z + halfWidth);
    }

    public static boolean canPlace(Level level, BlockPos supportPos, Direction facing, CustomPaintingData data, Entity ignored) {
        if (!facing.getAxis().isHorizontal()) return false;
        Direction side = facing.getCounterClockWise();
        int horizontalStart = -data.width() / 2;
        for (int x = 0; x < data.width(); x++) {
            for (int y = 0; y < data.height(); y++) {
                BlockPos support = supportPos.relative(side, horizontalStart + x).above(y);
                BlockState state = level.getBlockState(support);
                if (!state.isFaceSturdy(level, support, facing)) return false;
                BlockPos front = support.relative(facing);
                if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) return false;
            }
        }
        AABB box = boundingBox(supportPos, facing, data).deflate(0.01D);
        return level.noBlockCollision(ignored, box) && level.noEntityCollision(ignored, box);
    }
}
