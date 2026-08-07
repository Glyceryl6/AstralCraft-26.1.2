package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.platform.StartPlatform;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.items.cards.HandcardRedirection;
import com.astral_craft.common.network.s2c.BoardRouteStatePayload;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

/** Route selection, graph search and client preview state for board movement. */
public class BoardRouteService {

    public static boolean selectBranch(ServerPlayer player, BlockPos clickedPos) {
        BoardSession session = BoardSessionManager.findByController(player).orElse(null);
        if (session == null) return false;
        BoardSession.MovementState movement = session.movement();
        if (movement == null || movement.branchChoices().isEmpty()) return false;
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return false;
        String nodeId = session.positions().entrySet().stream().filter(entry -> entry.getValue().equals(clickedPos))
                .map(Map.Entry::getKey).findFirst().orElse("");
        if (!movement.branchChoices().contains(nodeId)) return false;
        HandcardRedirection.consumeFreeDirection(player.level(), session, participant.recordManualDecision());
        session.setMovement(movement.beginStep(nodeId, AstralServerTickClock.now(player.level()), BoardSessionManager.MOVEMENT_STEP_TICKS));
        BoardSessionManager.markChanged(player.level());
        preview(player.level(), session);
        return true;
    }

    public static List<String> nextChoices(BoardSession session, BoardParticipant participant) {
        BoardNode node = session.nodes().get(participant.currentNodeKey());
        if (node == null || node.next().isEmpty()) return List.of();
        List<String> choices = new ArrayList<>(node.next());
        if (HandcardRedirection.hasFreeDirection(participant)) return List.copyOf(choices);
        if (!participant.hasPreviousNode() && choices.size() > 1) return List.of(choices.getFirst());
        if (participant.hasPreviousNode() && choices.size() > 1) choices.remove(participant.previousNodeKey());
        return choices.isEmpty() ? node.next() : List.copyOf(choices);
    }

    public static String initialPreviousNode(BoardSession session, String currentNodeId) {
        return preferredNeighbor(session, currentNodeId, session.travelDirection().opposite()).orElse("");
    }

    public static Direction travelDirection(BoardSession session, BoardParticipant participant) {
        BlockPos current = session.positions().get(participant.currentNodeKey());
        if (current == null) return Direction.NORTH;
        if (participant.hasPreviousNode()) {
            BlockPos previous = session.positions().get(participant.previousNodeKey());
            if (previous != null && !previous.equals(current)) {
                return BoardEntityService.directionBetween(previous, current);
            }
        }

        return preferredNeighbor(session, participant.currentNodeKey(), session.travelDirection()).map(session.positions()::get)
                .map(target -> BoardEntityService.directionBetween(current, target)).orElse(Direction.NORTH);
    }

    public static Direction facingDirection(BoardSession session, BoardParticipant participant) {
        BlockPos current = session.positions().get(participant.currentNodeKey());
        if (current == null) return Direction.NORTH;
        List<String> choices = nextChoices(session, participant);
        if (choices.size() == 1) {
            BlockPos target = session.positions().get(choices.getFirst());
            if (target != null) return BoardEntityService.directionBetween(current, target);
        }
        if (choices.size() > 1) return directionTowardBoardCenter(session, current);
        return travelDirection(session, participant);
    }

    private static Direction directionTowardBoardCenter(BoardSession session, BlockPos current) {
        double centerX = session.positions().values().stream().mapToDouble(BlockPos::getX).average().orElse(current.getX());
        double centerZ = session.positions().values().stream().mapToDouble(BlockPos::getZ).average().orElse(current.getZ());
        int dx = Double.compare(centerX, current.getX());
        int dz = Double.compare(centerZ, current.getZ());
        if (Math.abs(centerX - current.getX()) >= Math.abs(centerZ - current.getZ()) && dx != 0) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (dz != 0) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return Direction.NORTH;
    }

    public static String previousNodeForTravelDirection(BoardSession session, String destinationNodeId, Direction travelDirection) {
        BlockPos destination = session.positions().get(destinationNodeId);
        if (destination == null || travelDirection == null) return "";
        Direction backward = travelDirection.getOpposite();
        String best = "";
        int bestDot = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (String neighborId : neighbors(session, destinationNodeId)) {
            BlockPos neighbor = session.positions().get(neighborId);
            if (neighbor == null) continue;
            int dx = neighbor.getX() - destination.getX();
            int dz = neighbor.getZ() - destination.getZ();
            int dot = dx * backward.getStepX() + dz * backward.getStepZ();
            int distance = Math.abs(dx) + Math.abs(dz);
            if (dot > bestDot || dot == bestDot && distance < bestDistance) {
                best = neighborId;
                bestDot = dot;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static Optional<String> preferredNeighbor(BoardSession session, String currentNodeId, BoardTravelDirection travelDirection) {
        BlockPos current = session.positions().get(currentNodeId);
        if (current == null) return Optional.empty();
        double centerX = session.positions().values().stream().mapToDouble(BlockPos::getX).average().orElse(current.getX());
        double centerZ = session.positions().values().stream().mapToDouble(BlockPos::getZ).average().orElse(current.getZ());
        double currentAngle = Math.atan2(current.getZ() - centerZ, current.getX() - centerX);
        String best = null;
        double bestDelta = Double.MAX_VALUE;
        for (String neighborId : neighbors(session, currentNodeId)) {
            BlockPos neighbor = session.positions().get(neighborId);
            if (neighbor == null) continue;
            double neighborAngle = Math.atan2(neighbor.getZ() - centerZ, neighbor.getX() - centerX);
            double delta = normalizeAngle(neighborAngle - currentAngle) * travelDirection.angularSign();
            if (delta <= 1.0E-6D) delta += Math.PI * 2.0D;
            if (delta < bestDelta) {
                best = neighborId;
                bestDelta = delta;
            }
        }

        return Optional.ofNullable(best);
    }

    private static List<String> neighbors(BoardSession session, String nodeId) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        BoardNode node = session.nodes().get(nodeId);
        if (node != null) result.addAll(node.next());
        session.nodes().values().stream().filter(candidate -> candidate.next().contains(nodeId))
                .map(BoardNode::id).forEach(result::add);
        return List.copyOf(result);
    }

    private static double normalizeAngle(double angle) {
        while (angle <= -Math.PI) angle += Math.PI * 2.0D;
        while (angle > Math.PI) angle -= Math.PI * 2.0D;
        return angle;
    }

    public static int graphDistance(BoardSession session, String start, String target, int maximum) {
        if (start.equals(target)) return 0;
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> distances = new HashMap<>();
        queue.add(start);
        distances.put(start, 0);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int distance = distances.get(current);
            if (distance >= maximum) continue;
            BoardNode node = session.nodes().get(current);
            if (node == null) continue;
            for (String next : node.next()) {
                if (distances.containsKey(next)) continue;
                int nextDistance = distance + 1;
                if (next.equals(target)) return nextDistance;
                distances.put(next, nextDistance);
                queue.add(next);
            }
        }

        return -1;
    }

    public static void preview(ServerLevel level, BoardSession session) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null) {
            broadcastState(session, false, List.of(), List.of(), List.of());
            return;
        }

        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null) return;
        List<List<String>> paths = possiblePaths(session, participant.currentNodeKey(), participant.previousNodeKey(),
                movement.remainingSteps(), HandcardRedirection.hasFreeDirection(participant));
        List<List<String>> highlightedPaths = stopOpportunityPaths(session, participant, paths);
        broadcastState(session, true, routePositions(session, paths), routePositions(session, highlightedPaths),
                nodePositions(session, movement.branchChoices()));
    }

    public static void broadcastState(BoardSession session, boolean active, List<List<BlockPos>> routes,
                                      List<List<BlockPos>> highlightedRoutes, List<BlockPos> branches) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(session.dimension());
        if (level == null) return;
        BoardSession.MovementState movement = session.movement();
        BoardParticipant participant = movement == null ? null : session.participant(movement.slotId()).orElse(null);
        int decisionTicks = active && movement != null && !movement.branchChoices().isEmpty()
                ? (int) Math.max(0L, movement.nextStepTick() - AstralServerTickClock.now(level)) : 0;
        int decisionDurationTicks = active && participant != null && !movement.branchChoices().isEmpty()
                ? participant.decisionDurationTicks(BoardSessionManager.BRANCH_TIMEOUT_TICKS) : 1;
        Identifier characterId = participant == null ? AstralCraft.prefix("mimi") : participant.characterId();
        Identifier skinId = participant == null ? Identifier.withDefaultNamespace("default") : participant.skinId();
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) > 160.0D * 160.0D) continue;
            List<List<BlockPos>> personalHighlights = participant != null && participant.controlledBy(player.getUUID()) ? highlightedRoutes : List.of();
            PacketDistributor.sendToPlayer(player, new BoardRouteStatePayload(session.id(), routes,
                    personalHighlights, branches, decisionTicks, decisionDurationTicks, characterId, skinId, active));
        }
    }

    private static List<List<String>> possiblePaths(BoardSession session, String start, String previous, int steps, boolean freeDirection) {
        List<List<String>> result = new ArrayList<>();
        collectPaths(session, start, previous, steps, freeDirection, new ArrayList<>(List.of(start)), result);
        return result;
    }

    private static List<List<String>> stopOpportunityPaths(BoardSession session, BoardParticipant participant,
                                                           List<List<String>> paths) {
        int cost = StartPlatform.nextStarCost(participant.stats().stars());
        if (cost <= 0 || participant.stats().starCoins() < cost) return List.of();
        return paths.stream().filter(path -> hasLevelUpOpportunity(session, participant, path)).toList();
    }

    private static boolean hasLevelUpOpportunity(BoardSession session, BoardParticipant participant, List<String> path) {
        for (int index = 1; index < path.size(); index++) {
            String nodeId = path.get(index);
            BoardNode node = session.nodes().get(nodeId);
            if (node == null || !(BuiltInRegistries.BLOCK.getValue(node.platformId()) instanceof StartPlatform platform)) continue;
            boolean landing = index == path.size() - 1;
            if (landing || !platform.characterStart() || session.canStopAtStart(participant, nodeId)) return true;
        }
        return false;
    }

    private static void collectPaths(BoardSession session, String current, String previous, int remaining,
                                     boolean freeDirection, List<String> path, List<List<String>> result) {
        if (result.size() >= BoardSessionManager.MAX_ROUTE_BRANCHES) return;
        if (remaining <= 0) {
            result.add(List.copyOf(path));
            return;
        }

        BoardNode node = session.nodes().get(current);
        if (node == null || node.next().isEmpty()) {
            result.add(List.copyOf(path));
            return;
        }

        List<String> choices = new ArrayList<>(node.next());
        if (!freeDirection) {
            if ((previous == null || previous.isBlank()) && choices.size() > 1) {
                choices = new ArrayList<>(List.of(choices.getFirst()));
            } else if (previous != null && !previous.isBlank() && choices.size() > 1) {
                choices.remove(previous);
            }
        }

        for (String next : choices) {
            path.add(next);
            collectPaths(session, next, current, remaining - 1, false, path, result);
            path.removeLast();
            if (result.size() >= BoardSessionManager.MAX_ROUTE_BRANCHES) break;
        }
    }

    private static List<List<BlockPos>> routePositions(BoardSession session, List<List<String>> paths) {
        List<List<BlockPos>> result = new ArrayList<>();
        for (List<String> path : paths) {
            List<BlockPos> positions = nodePositions(session, path);
            if (positions.size() >= 2) result.add(positions);
        }

        return List.copyOf(result);
    }

    private static List<BlockPos> nodePositions(BoardSession session, List<String> nodeIds) {
        return nodeIds.stream().map(session.positions()::get).filter(Objects::nonNull).toList();
    }

}