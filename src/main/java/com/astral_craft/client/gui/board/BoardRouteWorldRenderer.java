package com.astral_craft.client.gui.board;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.BoardRouteStatePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client-only route preview for the currently moving board pawn. */
public class BoardRouteWorldRenderer {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int ROUTE_COLOR = 0xF08BE8FF;
    private static final int STAR_ROUTE_COLOR = 0xFFFFE45F;
    private static final int BRANCH_COLOR = 0xFFFFD875;
    private static final int STAR_BRANCH_COLOR = 0xFFFFFF8A;
    private static final float ROUTE_Y_OFFSET = 0.555F;
    private static final float ROUTE_HALF_WIDTH = 0.075F;
    private static final float STAR_ROUTE_HALF_WIDTH = 0.105F;
    private static final float DASH_LENGTH = 0.24F;
    private static final float DASH_GAP = 0.10F;
    private static final double STALE_AFTER_TICKS = 20.0D * 30.0D;

    private static RouteState state = RouteState.EMPTY;

    public static void accept(BoardRouteStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            state = payload.active()
                    ? RouteState.parse(payload.boardId(), payload.encodedRoute(),
                    payload.encodedHighlightedRoute(), payload.encodedBranches())
                    : RouteState.EMPTY;
            BoardRouteDecisionOverlay.accept(payload);
        });
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        RouteState current = state;
        if (minecraft.level == null || !current.active()
                || ClientAnimationClock.elapsedTicks(current.receivedAtTick()) > STALE_AFTER_TICKS) {
            return;
        }

        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        float cycle = ClientAnimationClock.phaseTicks(20) / 20.0F;
        Set<Edge> highlightedEdges = edges(current.highlightedPaths());
        Set<Edge> normalEdges = edges(current.paths());
        normalEdges.removeAll(highlightedEdges);

        for (Edge edge : normalEdges) {
            submitDashedEdge(event, poseStack, cameraPos, edge, cycle,
                    ROUTE_COLOR, ROUTE_HALF_WIDTH);
        }
        for (Edge edge : highlightedEdges) {
            submitDashedEdge(event, poseStack, cameraPos, edge, cycle,
                    STAR_ROUTE_COLOR, STAR_ROUTE_HALF_WIDTH);
        }

        Vec3 branchOrigin = current.paths().isEmpty() || current.paths().getFirst().isEmpty()
                ? null : current.paths().getFirst().getFirst();
        Set<Vec3> highlightedBranches = new LinkedHashSet<>();
        for (List<Vec3> path : current.highlightedPaths()) {
            if (path.size() >= 2) highlightedBranches.add(path.get(1));
        }
        for (Vec3 branch : current.branches()) {
            boolean highlighted = highlightedBranches.contains(branch);
            submitBranchMarker(event, poseStack, cameraPos, branchOrigin, branch, cycle,
                    highlighted ? STAR_BRANCH_COLOR : BRANCH_COLOR, highlighted);
        }
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

    private static void submitDashedEdge(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                         Vec3 cameraPos, Edge edge, float cycle,
                                         int color, float halfWidth) {
        Vec3 start = edge.start().add(0.5D, ROUTE_Y_OFFSET, 0.5D);
        Vec3 end = edge.end().add(0.5D, ROUTE_Y_OFFSET, 0.5D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) return;
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).scale(halfWidth);
        double period = DASH_LENGTH + DASH_GAP;
        double offset = cycle * period;
        for (double cursor = -offset; cursor < length; cursor += period) {
            double from = Math.max(0.0D, cursor);
            double to = Math.min(length, cursor + DASH_LENGTH);
            if (to <= from) continue;
            Vec3 a = start.add(direction.scale(from)).subtract(side).subtract(cameraPos);
            Vec3 b = start.add(direction.scale(to)).subtract(side).subtract(cameraPos);
            Vec3 c = start.add(direction.scale(to)).add(side).subtract(cameraPos);
            Vec3 d = start.add(direction.scale(from)).add(side).subtract(cameraPos);
            submitQuad(event, poseStack, a, b, c, d, color);
        }
    }

    private static void submitBranchMarker(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                           Vec3 cameraPos, Vec3 origin, Vec3 blockPos, float cycle,
                                           int color, boolean highlighted) {
        double basePulse = highlighted ? 0.37D : 0.31D;
        double pulse = basePulse + Math.sin(cycle * Math.PI * 2.0D) * (highlighted ? 0.065D : 0.045D);
        double y = blockPos.y + ROUTE_Y_OFFSET + 0.055D
                + Math.sin(cycle * Math.PI * 2.0D) * 0.035D;
        Vec3 direction = origin == null ? new Vec3(0.0D, 0.0D, -1.0D)
                : new Vec3(blockPos.x - origin.x, 0.0D, blockPos.z - origin.z);
        if (direction.lengthSqr() < 1.0E-6D) direction = new Vec3(0.0D, 0.0D, -1.0D);
        direction = direction.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 center = new Vec3(blockPos.x + 0.5D, y, blockPos.z + 0.5D);
        Vec3 tip = center.add(direction.scale(pulse)).subtract(cameraPos);
        Vec3 right = center.add(direction.scale(-0.08D)).add(side.scale(0.18D)).subtract(cameraPos);
        Vec3 tail = center.add(direction.scale(-0.28D)).subtract(cameraPos);
        Vec3 left = center.add(direction.scale(-0.08D)).add(side.scale(-0.18D)).subtract(cameraPos);
        submitQuad(event, poseStack, tip, right, tail, left, color);
    }

    private static void submitQuad(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                   Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        event.getSubmitNodeCollector().order(1).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(TEXTURE),
                (pose, consumer) -> {
                    EffectRenderGeometry.vertex(consumer, pose, a, color, 0.0F, 0.0F,
                            new Vec3(0.0D, 1.0D, 0.0D));
                    EffectRenderGeometry.vertex(consumer, pose, b, color, 1.0F, 0.0F,
                            new Vec3(0.0D, 1.0D, 0.0D));
                    EffectRenderGeometry.vertex(consumer, pose, c, color, 1.0F, 1.0F,
                            new Vec3(0.0D, 1.0D, 0.0D));
                    EffectRenderGeometry.vertex(consumer, pose, d, color, 0.0F, 1.0F,
                            new Vec3(0.0D, 1.0D, 0.0D));
                });
    }

    private record RouteState(String boardId, List<List<Vec3>> paths,
                              List<List<Vec3>> highlightedPaths, List<Vec3> branches,
                              double receivedAtTick, boolean active) {

        private static final RouteState EMPTY = new RouteState("", List.of(), List.of(),
                List.of(), 0.0D, false);

        private static RouteState parse(String boardId, String encodedRoute,
                                        String encodedHighlightedRoute, String encodedBranches) {
            return new RouteState(boardId, parsePaths(encodedRoute),
                    parsePaths(encodedHighlightedRoute), parsePoints(encodedBranches),
                    ClientAnimationClock.nowTicks(), true);
        }

        private static List<List<Vec3>> parsePaths(String encoded) {
            if (encoded == null || encoded.isBlank()) return List.of();
            List<List<Vec3>> paths = new ArrayList<>();
            for (String route : encoded.split("\\|", -1)) {
                List<Vec3> points = parsePoints(route);
                if (points.size() >= 2) paths.add(points);
            }
            return List.copyOf(paths);
        }

        private static List<Vec3> parsePoints(String encoded) {
            if (encoded == null || encoded.isBlank()) return List.of();
            List<Vec3> result = new ArrayList<>();
            for (String point : encoded.split(";")) {
                String[] fields = point.split(",", 3);
                if (fields.length != 3) continue;
                try {
                    result.add(new Vec3(Integer.parseInt(fields[0]),
                            Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
                } catch (NumberFormatException ignored) {}
            }
            return List.copyOf(result);
        }
    }

    private record Edge(Vec3 start, Vec3 end) {

        private static Edge normalized(Vec3 first, Vec3 second) {
            if (first.equals(second)) return null;
            if (compare(first, second) <= 0) return new Edge(first, second);
            return new Edge(second, first);
        }

        private static int compare(Vec3 first, Vec3 second) {
            int x = Double.compare(first.x, second.x);
            if (x != 0) return x;
            int y = Double.compare(first.y, second.y);
            return y != 0 ? y : Double.compare(first.z, second.z);
        }
    }
}