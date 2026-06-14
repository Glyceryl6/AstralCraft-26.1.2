package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.PanelTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Scans a connected physical platform layout and turns it into logical board nodes. */
public class BoardScanner {

    public static final int MAX_SCAN_NODES = 384;

    public static ScannedBoard scan(ServerLevel level, BlockPos origin) {
        List<String> errors = new ArrayList<>();
        BlockState originState = level.getBlockState(origin);
        if (!PlatformPanelMapper.isPlatform(originState.getBlock())) {
            errors.add("origin_not_panel");
            return new ScannedBoard(Map.of(), Map.of(), new BoardArea(origin, origin), List.of(), errors);
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        visited.add(origin.immutable());
        while (!queue.isEmpty() && visited.size() <= MAX_SCAN_NODES) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && PlatformPanelMapper.isPlatform(level.getBlockState(next).getBlock())) {
                    visited.add(next.immutable());
                    queue.add(next.immutable());
                }
            }
        }

        if (visited.size() > MAX_SCAN_NODES) {
            errors.add("too_many_panels");
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : visited) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }

        Map<String, BoardNode> nodes = new LinkedHashMap<>();
        Map<String, BlockPos> positions = new LinkedHashMap<>();
        List<String> starts = new ArrayList<>();
        for (BlockPos pos : visited) {
            BlockState state = level.getBlockState(pos);
            Identifier panelId = PlatformPanelMapper.panelId(state.getBlock())
                    .orElse(AstralCraft.prefix("recover"));
            String id = id(pos);
            List<String> next = nextIds(level, visited, pos, state);
            nodes.put(id, new BoardNode(id, panelId, next));
            positions.put(id, pos.immutable());
            if (panelId.equals(PanelTypes.START.getId())) {
                starts.add(id);
            }
        }

        if (starts.size() < 4) {
            errors.add("need_4_start_panels");
        }

        if (nodes.size() < 8) {
            errors.add("too_few_panels");
        }

        return new ScannedBoard(nodes, positions, new BoardArea(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)).inflate(4, 8), starts, errors);
    }

    private static List<String> nextIds(ServerLevel level, Set<BlockPos> all, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();
        if (state.hasProperty(BasePlatform.FACING)) {
            directions.add(state.getValue(BasePlatform.FACING));
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!directions.contains(direction)) directions.add(direction);
        }

        List<String> ids = new ArrayList<>();
        for (Direction direction : directions) {
            BlockPos next = pos.relative(direction);
            if (all.contains(next)) {
                ids.add(id(next));
            }
        }

        return ids;
    }

    public static String id(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

}