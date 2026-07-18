package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.blocks.platform.StartPlatform;
import com.astral_craft.common.gameplay.BoardNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Scans a connected physical platform layout and turns it into a protected logical board. */
public class BoardScanner {

    public static final int MAX_SCAN_NODES = 512;
    public static final int MIN_SCAN_NODES = 8;
    private static final int REQUIRED_START_NODES = 4;
    private static final int MAX_START_SELECTION_CANDIDATES = 24;
    private static final double MIN_START_ANGLE_GAP = Math.toRadians(38.0D);
    private static final double MAX_START_ANGLE_GAP = Math.toRadians(142.0D);

    public static ScannedBoard scan(ServerLevel level, BlockPos origin) {
        List<String> errors = new ArrayList<>();
        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof BasePlatform)) {
            errors.add("origin_not_panel");
            return empty(origin, errors);
        }

        Set<BlockPos> reservedPanels = existingBoardPanels(level);
        if (reservedPanels.contains(origin)) {
            errors.add("origin_in_existing_board");
            return empty(origin, errors);
        }

        int boardY = origin.getY();
        Set<BlockPos> visited = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        visited.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (next.getY() != boardY || visited.contains(next) || reservedPanels.contains(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof BasePlatform) {
                    visited.add(next.immutable());
                    if (visited.size() > MAX_SCAN_NODES) {
                        errors.add("too_many_panels");
                        return empty(origin, errors);
                    }
                    queue.add(next.immutable());
                }
            }
        }

        if (visited.size() < MIN_SCAN_NODES) errors.add("too_few_panels");

        Map<BlockPos, List<BlockPos>> adjacency = adjacency(visited);
        if (hasSolidTwoByTwo(visited)) errors.add("ambiguous_dense_panels");
        if (adjacency.values().stream().anyMatch(neighbors -> neighbors.size() < 2)) {
            errors.add("dead_end_or_gap");
        }
        if (hasBridge(adjacency)) errors.add("not_closed");

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : visited) {
            minX = Math.min(minX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        List<BlockPos> allStartPositions = visited.stream()
                .filter(pos -> level.getBlockState(pos).getBlock() instanceof StartPlatform).toList();
        List<BlockPos> selectedStartPositions = selectStartPositions(allStartPositions, visited);
        if (allStartPositions.size() < REQUIRED_START_NODES) {
            errors.add("need_4_start_panels");
        } else if (selectedStartPositions.size() != REQUIRED_START_NODES || !balancedStarts(selectedStartPositions, visited)) {
            errors.add("unbalanced_start_panels");
        }

        Set<BlockPos> selectedStarts = new HashSet<>(selectedStartPositions);
        Map<String, BoardNode> nodes = new LinkedHashMap<>();
        Map<String, BlockPos> positions = new LinkedHashMap<>();
        List<String> starts = new ArrayList<>();
        Comparator<BlockPos> comparingInt = Comparator.comparingInt(Vec3i::getX);
        List<BlockPos> orderedPositions = visited.stream().sorted(comparingInt.thenComparingInt(Vec3i::getZ)).toList();
        for (BlockPos pos : orderedPositions) {
            BlockState state = level.getBlockState(pos);
            Identifier platformId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String id = id(pos);
            List<String> next = nextIds(adjacency.getOrDefault(pos, List.of()), pos, state);
            nodes.put(id, new BoardNode(id, platformId, next));
            positions.put(id, pos.immutable());
            if (selectedStarts.contains(pos)) starts.add(id);
        }
        starts.sort(Comparator.comparingDouble(startId -> clockwiseAngle(positions.get(startId), boardCenter(visited))));

        BoardArea area = visited.isEmpty()
                ? new BoardArea(origin, origin)
                : new BoardArea(new BlockPos(minX, boardY - 3, minZ), new BlockPos(maxX, boardY + 8, maxZ));
        return new ScannedBoard(nodes, positions, area, starts, List.copyOf(errors));
    }

    private static Set<BlockPos> existingBoardPanels(ServerLevel level) {
        Set<BlockPos> result = new HashSet<>();
        for (BoardSession session : BoardSavedData.get(level).sessions()) {
            for (BlockPos pos : session.positions().values()) result.add(pos.immutable());
        }
        return result;
    }

    private static List<BlockPos> selectStartPositions(List<BlockPos> candidates, Set<BlockPos> boardPositions) {
        if (candidates.size() < REQUIRED_START_NODES) return List.copyOf(candidates);
        BoardCenter center = boardCenter(boardPositions);
        List<BlockPos> considered = candidates.stream()
                .sorted(Comparator.comparingDouble((BlockPos pos) -> -distanceSquared(pos, center)))
                .limit(MAX_START_SELECTION_CANDIDATES).toList();
        StartSelection best = null;
        for (int first = 0; first < considered.size() - 3; first++) {
            for (int second = first + 1; second < considered.size() - 2; second++) {
                for (int third = second + 1; third < considered.size() - 1; third++) {
                    for (int fourth = third + 1; fourth < considered.size(); fourth++) {
                        List<BlockPos> selected = List.of(considered.get(first), considered.get(second),
                                considered.get(third), considered.get(fourth));
                        double score = startSelectionScore(selected, center);
                        if (best == null || score < best.score()) best = new StartSelection(selected, score);
                    }
                }
            }
        }

        if (best == null) return List.of();
        return best.positions().stream().sorted(Comparator.comparingDouble(pos -> clockwiseAngle(pos, center))).toList();
    }

    private static double startSelectionScore(List<BlockPos> selected, BoardCenter center) {
        List<Double> angles = selected.stream().map(pos -> clockwiseAngle(pos, center)).sorted().toList();
        double angularPenalty = 0.0D;
        for (int index = 0; index < angles.size(); index++) {
            double current = angles.get(index);
            double next = index + 1 < angles.size() ? angles.get(index + 1) : angles.getFirst() + Math.PI * 2.0D;
            angularPenalty += Math.abs(next - current - Math.PI / 2.0D);
        }

        double centroidX = selected.stream().mapToDouble(BlockPos::getX).average().orElse(center.x());
        double centroidZ = selected.stream().mapToDouble(BlockPos::getZ).average().orElse(center.z());
        double centerPenalty = square(centroidX - center.x()) + square(centroidZ - center.z());
        double averageRadius = selected.stream().mapToDouble(pos -> Math.sqrt(distanceSquared(pos, center))).average().orElse(0.0D);
        double radialVariance = selected.stream().mapToDouble(pos -> square(Math.sqrt(distanceSquared(pos, center)) - averageRadius)).sum();
        double spread = 0.0D;
        for (int first = 0; first < selected.size(); first++) {
            for (int second = first + 1; second < selected.size(); second++) {
                BlockPos firstPos = selected.get(first);
                BlockPos secondPos = selected.get(second);
                spread += square(firstPos.getX() - secondPos.getX())
                        + square(firstPos.getY() - secondPos.getY())
                        + square(firstPos.getZ() - secondPos.getZ());
            }
        }

        return angularPenalty * 1000.0D + centerPenalty * 12.0D + radialVariance * 4.0D - spread * 0.04D;
    }

    private static boolean balancedStarts(List<BlockPos> starts, Set<BlockPos> boardPositions) {
        if (starts.size() != REQUIRED_START_NODES) return false;
        BoardCenter center = boardCenter(boardPositions);
        List<Double> angles = starts.stream().map(pos -> clockwiseAngle(pos, center)).sorted().toList();
        for (int index = 0; index < angles.size(); index++) {
            double current = angles.get(index);
            double next = index + 1 < angles.size() ? angles.get(index + 1) : angles.getFirst() + Math.PI * 2.0D;
            double gap = next - current;
            if (gap < MIN_START_ANGLE_GAP || gap > MAX_START_ANGLE_GAP) return false;
        }
        return true;
    }

    private static BoardCenter boardCenter(Set<BlockPos> positions) {
        return new BoardCenter(positions.stream().mapToDouble(BlockPos::getX).average().orElse(0.0D),
                positions.stream().mapToDouble(BlockPos::getZ).average().orElse(0.0D));
    }

    private static double clockwiseAngle(BlockPos pos, BoardCenter center) {
        double angle = Math.atan2(pos.getX() - center.x(), center.z() - pos.getZ());
        return angle < 0.0D ? angle + Math.PI * 2.0D : angle;
    }

    private static double distanceSquared(BlockPos pos, BoardCenter center) {
        return square(pos.getX() - center.x()) + square(pos.getZ() - center.z());
    }

    private static double square(double value) {
        return value * value;
    }

    private static ScannedBoard empty(BlockPos origin, List<String> errors) {
        return new ScannedBoard(Map.of(), Map.of(), new BoardArea(origin, origin), List.of(), List.copyOf(errors));
    }

    private static boolean hasSolidTwoByTwo(Set<BlockPos> panels) {
        for (BlockPos pos : panels) {
            if (panels.contains(pos.east()) && panels.contains(pos.south())
                    && panels.contains(pos.east().south())) return true;
        }
        return false;
    }

    private static Map<BlockPos, List<BlockPos>> adjacency(Set<BlockPos> all) {
        Map<BlockPos, List<BlockPos>> result = new LinkedHashMap<>();
        for (BlockPos pos : all) {
            List<BlockPos> neighbors = new ArrayList<>();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (all.contains(next)) neighbors.add(next.immutable());
            }

            result.put(pos, List.copyOf(neighbors));
        }

        return result;
    }

    private static List<String> nextIds(List<BlockPos> neighbors, BlockPos pos, BlockState state) {
        List<Direction> directions = new ArrayList<>();
        if (state.hasProperty(BasePlatform.FACING)) directions.add(state.getValue(BasePlatform.FACING));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!directions.contains(direction)) directions.add(direction);
        }

        List<String> ids = new ArrayList<>();
        for (Direction direction : directions) {
            BlockPos next = pos.relative(direction);
            if (neighbors.contains(next)) ids.add(id(next));
        }

        return List.copyOf(ids);
    }

    /** A bridge is a physical gap/dead connector: removing it splits the board into separate parts. */
    private static boolean hasBridge(Map<BlockPos, List<BlockPos>> graph) {
        if (graph.isEmpty()) return true;
        Map<BlockPos, Integer> discovery = new HashMap<>();
        Map<BlockPos, Integer> low = new HashMap<>();
        int[] time = {0};
        BlockPos start = graph.keySet().iterator().next();
        boolean bridge = dfsBridge(start, null, graph, discovery, low, time);
        return bridge || discovery.size() != graph.size();
    }

    private static boolean dfsBridge(BlockPos node, BlockPos parent,
                                     Map<BlockPos, List<BlockPos>> graph,
                                     Map<BlockPos, Integer> discovery,
                                     Map<BlockPos, Integer> low, int[] time) {
        int currentTime = ++time[0];
        discovery.put(node, currentTime);
        low.put(node, currentTime);
        for (BlockPos next : graph.getOrDefault(node, List.of())) {
            if (next.equals(parent)) continue;
            Integer nextDiscovery = discovery.get(next);
            if (nextDiscovery == null) {
                if (dfsBridge(next, node, graph, discovery, low, time)) return true;
                low.put(node, Math.min(low.get(node), low.get(next)));
                if (low.get(next) > discovery.get(node)) return true;
            } else {
                low.put(node, Math.min(low.get(node), nextDiscovery));
            }
        }

        return false;
    }

    public static String id(BlockPos pos) {
        return AstralCraft.prefix("board_node/x" + coordinate(pos.getX()) + "_y"
                + coordinate(pos.getY()) + "_z" + coordinate(pos.getZ())).toString();
    }

    private static String coordinate(int value) {
        long magnitude = Math.abs((long) value);
        return (value < 0 ? "n" : "p") + Long.toString(magnitude, 36);
    }

    private record BoardCenter(double x, double z) {}
    private record StartSelection(List<BlockPos> positions, double score) {}

}