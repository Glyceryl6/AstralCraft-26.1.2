package com.astral_craft.client.gui.board;

import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.network.s2c.BoardRouteStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Client-only route and movement-direction preview for board pawns. */
public class BoardRouteWorldRenderer {

    private static final int ROUTE_GLOW_COLOR = 0xB030BFFF;
    private static final int ROUTE_COLOR = 0xFFBDEFFF;
    private static final int STOP_ROUTE_GLOW_COLOR = 0xB0FFBE16;
    private static final int STOP_ROUTE_COLOR = 0xFFFFE991;
    private static final int IDLE_ARROW_GLOW_COLOR = 0xC035E875;
    private static final int IDLE_ARROW_COLOR = 0xFFB8FFD0;
    private static final int BRANCH_GLOW_COLOR = 0xC02F79FF;
    private static final int BRANCH_COLOR = 0xFFB5D4FF;
    private static final int STOP_BRANCH_GLOW_COLOR = 0xC0FFB000;
    private static final int STOP_BRANCH_COLOR = 0xFFFFE07A;
    private static final float ROUTE_GLOW_WIDTH = 8.0F;
    private static final float ROUTE_WIDTH = 4.0F;
    private static final float STOP_ROUTE_GLOW_WIDTH = 9.0F;
    private static final float STOP_ROUTE_WIDTH = 4.8F;
    private static final float ARROW_GLOW_WIDTH = 8.0F;
    private static final float ARROW_WIDTH = 4.5F;
    private static final float DASH_LENGTH = 0.27F;
    private static final float DASH_GAP = 0.09F;
    private static final double ROUTE_Y_OFFSET = 0.3D;
    private static final double STALE_AFTER_TICKS = 20.0D * 30.0D;

    private static RouteState state = RouteState.EMPTY;

    public static void accept(BoardRouteStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            state = payload.active() ? RouteState.from(payload) : RouteState.EMPTY;
            BoardRouteDecisionOverlay.accept(payload);
        });
    }

    public static void clear(UUID boardId) {
        if (boardId == null || !state.boardId().equals(boardId)) return;
        state = RouteState.EMPTY;
        BoardRouteDecisionOverlay.clear();
    }

    public static void submit() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        RouteState current = state;
        boolean branchDecision = !current.branches().isEmpty();
        if (!current.active() || !branchDecision
                && ClientAnimationClock.elapsedTicks(current.receivedAtTick()) > STALE_AFTER_TICKS) {
            submitIdleDirectionArrows(minecraft);
            return;
        }

        float cycle = ClientAnimationClock.phaseTicks(20) / 20.0F;
        Set<Edge> highlightedEdges = edges(current.highlightedPaths());
        Set<Edge> normalEdges = edges(current.paths());
        normalEdges.removeAll(highlightedEdges);

        for (Edge edge : normalEdges) {
            submitDashedEdge(edge, cycle, ROUTE_GLOW_COLOR, ROUTE_GLOW_WIDTH);
            submitDashedEdge(edge, cycle, ROUTE_COLOR, ROUTE_WIDTH);
        }
        for (Edge edge : highlightedEdges) {
            submitSolidEdge(edge, STOP_ROUTE_GLOW_COLOR, STOP_ROUTE_GLOW_WIDTH);
            submitSolidEdge(edge, STOP_ROUTE_COLOR, STOP_ROUTE_WIDTH);
        }

        Vec3 branchOrigin = current.paths().isEmpty() || current.paths().getFirst().isEmpty()
                ? null : current.paths().getFirst().getFirst();
        Set<Vec3> highlightedBranches = new LinkedHashSet<>();
        for (List<Vec3> path : current.highlightedPaths()) {
            if (path.size() >= 2) highlightedBranches.add(path.get(1));
        }
        for (Vec3 branch : current.branches()) {
            submitBranchMarker(branchOrigin, branch, cycle, highlightedBranches.contains(branch));
            submitBranchOutline(branch);
        }
    }

    public static List<BlockPos> tutorialBranchPositions() {
        RouteState current = state;
        if (!current.active() || !BoardTutorialGuide.visible(current.boardId(), BoardTutorialGuide.Hint.BRANCH)) return List.of();
        return current.branches().stream().map(BlockPos::containing).toList();
    }

    private static void submitBranchOutline(Vec3 position) {
        float cycle = ClientAnimationClock.phaseTicks(24) / 24.0F;
        double expand = 0.035D + Math.sin(cycle * Math.PI * 2.0D) * 0.012D;
        double minX = position.x - expand;
        double minY = position.y - 0.025D;
        double minZ = position.z - expand;
        double maxX = position.x + 1.0D + expand;
        double maxY = position.y + 1.025D;
        double maxZ = position.z + 1.0D + expand;
        Vec3[] points = {
                new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ),
                new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ),
                new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ),
                new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] edge : edges) {
            submitLine(points[edge[0]], points[edge[1]], STOP_BRANCH_GLOW_COLOR, 7.0F);
            submitLine(points[edge[0]], points[edge[1]], STOP_BRANCH_COLOR, 3.2F);
        }
    }

    private static void submitIdleDirectionArrows(Minecraft minecraft) {
        Vec3 playerPos = minecraft.player.position();
        AABB range = new AABB(playerPos, playerPos).inflate(128.0D);
        float cycle = ClientAnimationClock.phaseTicks(30) / 30.0F;
        for (AstralCharacterEntity entity : minecraft.level.getEntitiesOfClass(AstralCharacterEntity.class, range,
                candidate -> candidate.isBoardPawn()
                        && candidate.boardSessionUuid().map(BoardHudOverlay::isTracking).orElse(false)
                        && !"walk".equals(candidate.animationAction())
                        && !"knockdown".equals(candidate.animationAction()))) {
            int mask = entity.boardDirectionMask();
            if (mask == 0) {
                submitIdleArrow(entity.position(), entity.boardDirection(), cycle);
                continue;
            }
            for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                if ((mask & 1 << direction.get2DDataValue()) != 0) submitIdleArrow(entity.position(), direction, cycle);
            }
        }
    }

    private static void submitIdleArrow(Vec3 entityPos, Direction direction, float cycle) {
        Vec3 forward = new Vec3(direction.getStepX(), 0.0D, direction.getStepZ());
        if (forward.lengthSqr() < 0.5D) return;
        double pulse = 0.52D + Math.sin(cycle * Math.PI * 2.0D) * 0.045D;
        Vec3 center = new Vec3(entityPos.x, entityPos.y + 0.3D, entityPos.z);
        Vec3 start = center.add(forward.scale(0.15D));
        Vec3 end = center.add(forward.scale(pulse));
        Gizmos.arrow(start, end, IDLE_ARROW_GLOW_COLOR, ARROW_GLOW_WIDTH).setAlwaysOnTop();
        Gizmos.arrow(start, end, IDLE_ARROW_COLOR, ARROW_WIDTH).setAlwaysOnTop();
    }

    private static Set<Edge> edges(List<List<Vec3>> paths) {
        Set<Edge> result = new LinkedHashSet<>();
        for (List<Vec3> path : paths) {
            for (int index = 1; index < path.size(); index++) {
                Edge edge = Edge.normalized(path.get(index - 1), path.get(index));
                if (edge != null) result.add(edge);
            }
        }

        return result;
    }

    private static void submitDashedEdge(Edge edge, float cycle, int color, float width) {
        Vec3 start = edge.start().add(0.5D, ROUTE_Y_OFFSET, 0.5D);
        Vec3 end = edge.end().add(0.5D, ROUTE_Y_OFFSET, 0.5D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) return;
        Vec3 direction = delta.scale(1.0D / length);
        double period = DASH_LENGTH + DASH_GAP;
        double offset = cycle * period;
        for (double cursor = -offset; cursor < length; cursor += period) {
            double from = Math.max(0.0D, cursor);
            double to = Math.min(length, cursor + DASH_LENGTH);
            if (to > from) submitLine(start.add(direction.scale(from)), start.add(direction.scale(to)), color, width);
        }
    }

    private static void submitSolidEdge(Edge edge, int color, float width) {
        submitLine(edge.start().add(0.5D, ROUTE_Y_OFFSET, 0.5D),
                edge.end().add(0.5D, ROUTE_Y_OFFSET, 0.5D), color, width);
    }

    private static void submitBranchMarker(Vec3 origin, Vec3 blockPos, float cycle, boolean highlighted) {
        double pulse = (highlighted ? 0.47D : 0.41D)
                + Math.sin(cycle * Math.PI * 2.0D) * (highlighted ? 0.07D : 0.055D);
        double y = blockPos.y + ROUTE_Y_OFFSET + 0.08D + Math.sin(cycle * Math.PI * 2.0D) * 0.035D;
        Vec3 direction = origin == null ? new Vec3(0.0D, 0.0D, -1.0D)
                : new Vec3(blockPos.x - origin.x, 0.0D, blockPos.z - origin.z);
        if (direction.lengthSqr() < 1.0E-6D) direction = new Vec3(0.0D, 0.0D, -1.0D);
        direction = direction.normalize();
        Vec3 center = new Vec3(blockPos.x + 0.5D, y, blockPos.z + 0.5D);
        int glow = highlighted ? STOP_BRANCH_GLOW_COLOR : BRANCH_GLOW_COLOR;
        int core = highlighted ? STOP_BRANCH_COLOR : BRANCH_COLOR;
        Vec3 start = center.add(direction.scale(-0.30D));
        Vec3 end = center.add(direction.scale(pulse));
        Gizmos.arrow(start, end, glow, ARROW_GLOW_WIDTH).setAlwaysOnTop();
        Gizmos.arrow(start, end, core, ARROW_WIDTH).setAlwaysOnTop();
    }

    private static void submitLine(Vec3 start, Vec3 end, int color, float width) {
        Gizmos.line(start, end, color, width).setAlwaysOnTop();
    }

    private record RouteState(UUID boardId, List<List<Vec3>> paths,
                              List<List<Vec3>> highlightedPaths, List<Vec3> branches,
                              double receivedAtTick, boolean active) {

        private static final UUID EMPTY_BOARD_ID = new UUID(0L, 0L);
        private static final RouteState EMPTY = new RouteState(EMPTY_BOARD_ID,
                List.of(), List.of(), List.of(), 0.0D, false);

        private static RouteState from(BoardRouteStatePayload payload) {
            return new RouteState(payload.boardId(), toPaths(payload.routes()),
                    toPaths(payload.highlightedRoutes()), toPoints(payload.branches()),
                    ClientAnimationClock.nowTicks(), true);
        }

        private static List<List<Vec3>> toPaths(List<List<BlockPos>> routes) {
            return routes.stream().map(RouteState::toPoints).filter(path -> path.size() >= 2).toList();
        }

        private static List<Vec3> toPoints(List<BlockPos> positions) {
            return positions.stream().map(position -> new Vec3(position.getX(), position.getY(), position.getZ())).toList();
        }

    }

    private record Edge(Vec3 start, Vec3 end) {

        private static Edge normalized(Vec3 first, Vec3 second) {
            if (first.equals(second)) return null;
            return compare(first, second) <= 0 ? new Edge(first, second) : new Edge(second, first);
        }

        private static int compare(Vec3 first, Vec3 second) {
            int x = Double.compare(first.x, second.x);
            if (x != 0) return x;
            int y = Double.compare(first.y, second.y);
            return y != 0 ? y : Double.compare(first.z, second.z);
        }

    }

}