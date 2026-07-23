package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.blocks.BasePlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Shared client/server placement transform and terrain validation for saved board templates. */
public class BoardTemplatePlacement {

    public static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.SOUTH;
    }

    public static Direction xDirection(Direction facing) {
        return switch (horizontal(facing)) {
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    public static Rotation rotation(Direction facing) {
        return switch (horizontal(facing)) {
            case SOUTH -> Rotation.NONE;
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos origin(BlockPos clickedGround, Direction facing, BoardTemplateData template) {
        Direction xDirection = xDirection(facing);
        return clickedGround.above().relative(xDirection.getOpposite(), (template.width() - 1) / 2);
    }

    public static BlockPos transform(BlockPos origin, Direction facing, BlockPos offset) {
        Direction xDirection = xDirection(facing);
        Direction zDirection = horizontal(facing);
        return origin.relative(xDirection, offset.getX()).relative(zDirection, offset.getZ()).above(offset.getY());
    }

    public static BoardArea boardArea(BlockPos clickedGround, Direction facing, BoardTemplateData template) {
        BlockPos origin = origin(clickedGround, facing, template);
        BlockPos first = transform(origin, facing, BlockPos.ZERO);
        BlockPos last = transform(origin, facing, new BlockPos(template.width() - 1, 0, template.depth() - 1));
        int minX = Math.min(first.getX(), last.getX());
        int maxX = Math.max(first.getX(), last.getX());
        int minZ = Math.min(first.getZ(), last.getZ());
        int maxZ = Math.max(first.getZ(), last.getZ());
        int panelY = first.getY();
        return new BoardArea(new BlockPos(minX, panelY - 3, minZ), new BlockPos(maxX, panelY + 8, maxZ));
    }

    public static boolean canPlace(Level level, BlockPos clickedGround, Direction facing, BoardTemplateData template) {
        if (level == null || template == null || !template.valid()) return false;
        BlockPos origin = origin(clickedGround, facing, template);
        for (int z = 0; z < template.depth(); z++) {
            for (int x = 0; x < template.width(); x++) {
                BlockPos target = transform(origin, facing, new BlockPos(x, 0, z));
                BlockPos support = target.below();
                if (!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)) return false;
                if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return false;
                if (!level.getBlockState(target).canBeReplaced()) return false;
            }
        }
        return true;
    }

    private static Direction rotatedDirection(Direction direction, Direction facing) {
        if (!direction.getAxis().isHorizontal()) return direction;
        return switch (horizontal(facing)) {
            case SOUTH -> direction;
            case WEST -> switch (direction) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> direction;
            };
            case NORTH -> direction.getOpposite();
            case EAST -> switch (direction) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                default -> direction;
            };
            default -> direction;
        };
    }

    public static List<PlacedBlock> transformedBlocks(BlockPos clickedGround, Direction facing, BoardTemplateData template) {
        BlockPos origin = origin(clickedGround, facing, template);
        Rotation rotation = rotation(facing);
        List<PlacedBlock> result = new ArrayList<>(template.blocks().size());
        for (BoardTemplateData.TemplateBlock block : template.blocks()) {
            BlockState state = block.state();
            if (state.hasProperty(BasePlatform.FACING)) {
                state = state.setValue(BasePlatform.FACING, rotatedDirection(state.getValue(BasePlatform.FACING), facing));
            } else {
                state = state.rotate(rotation);
            }
            result.add(new PlacedBlock(transform(origin, facing, block.offset()), state));
        }
        return List.copyOf(result);
    }

    public record PlacedBlock(BlockPos pos, BlockState state) {}
}
