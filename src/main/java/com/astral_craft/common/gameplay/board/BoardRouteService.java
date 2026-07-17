package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.platform.StartPlatform;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.network.s2c.BoardRouteStatePayload;
import net.minecraft.core.BlockPos;
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
        BoardSessionManager.updateParticipant(player.level(), session, participant.recordManualDecision());
        session.setMovement(movement.beginStep(nodeId, player.level().getGameTime(), BoardSessionManager.MOVEMENT_STEP_TICKS));
        BoardSessionManager.markChanged(player.level());
        preview(player.level(), session);
        return true;
    }

    public static List<String> nextChoices(BoardSession session, BoardParticipant participant) {
        BoardNode node = session.nodes().get(participant.currentNodeKey());
        if (node == null || node.next().isEmpty()) return List.of();
        List<String> choices = new ArrayList<>(node.next());
        if (!participant.hasPreviousNode() && choices.size() > 1) return List.of(choices.getFirst());
        if (participant.hasPreviousNode() && choices.size() > 1) choices.remove(participant.previousNodeKey());
        return choices.isEmpty() ? node.next() : List.copyOf(choices);
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
            broadcastState(session, false, "", "", "");
            return;
        }
        BoardParticipant participant = session.participant(movement.slotId()).orElse(null);
        if (participant == null) return;
        List<List<String>> paths = possiblePaths(session, participant.currentNodeKey(), participant.previousNodeKey(),
                movement.remainingSteps());
        List<List<String>> highlightedPaths = startOpportunityPaths(session, participant, paths);
        broadcastState(session, true, encodePaths(session, paths), encodePaths(session, highlightedPaths),
                encodeNodePositions(session, movement.branchChoices()));
    }

    public static void broadcastState(BoardSession session, boolean active, String route, String highlightedRoute, String branches) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(session.dimension());
        if (level == null) return;
        BoardSession.MovementState movement = session.movement();
        BoardParticipant participant = movement == null ? null : session.participant(movement.slotId()).orElse(null);
        int decisionTicks = active && movement != null && !movement.branchChoices().isEmpty()
                ? (int) Math.max(0L, movement.nextStepTick() - level.getGameTime()) : 0;
        int decisionDurationTicks = active && participant != null && !movement.branchChoices().isEmpty()
                ? participant.decisionDurationTicks(BoardSessionManager.BRANCH_TIMEOUT_TICKS) : 1;
        Identifier characterId = participant == null ? AstralCraft.prefix("mimi") : participant.characterId();
        Identifier skinId = participant == null ? Identifier.withDefaultNamespace("default") : participant.skinId();
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D)
                    > 160.0D * 160.0D) continue;
            String personalHighlight = participant != null && participant.controlledBy(player.getUUID())
                    ? highlightedRoute : "";
            PacketDistributor.sendToPlayer(player, new BoardRouteStatePayload(session.id().toString(), route,
                    personalHighlight, branches, decisionTicks, decisionDurationTicks, characterId, skinId, active));
        }
    }

    private static List<List<String>> possiblePaths(BoardSession session, String start, String previous, int steps) {
        List<List<String>> result = new ArrayList<>();
        collectPaths(session, start, previous, steps, new ArrayList<>(List.of(start)), result);
        return result;
    }

    private static List<List<String>> startOpportunityPaths(BoardSession session, BoardParticipant participant,
                                                             List<List<String>> paths) {
        int cost = StartPlatform.nextStarCost(participant.stats().stars());
        if (participant.stats().stars() >= 3 || cost <= 0 || participant.stats().starCoins() < cost) return List.of();
        Set<String> startNodes = Set.copyOf(session.startNodes());
        return paths.stream().filter(path -> {
            if (path.size() < 2) return false;
            if (startNodes.contains(path.getLast())) return true;
            return path.subList(1, path.size()).stream().anyMatch(nodeId -> session.canStopAtStart(participant, nodeId));
        }).toList();
    }

    private static void collectPaths(BoardSession session, String current, String previous, int remaining,
                                     List<String> path, List<List<String>> result) {
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
        if ((previous == null || previous.isBlank()) && choices.size() > 1) {
            choices = new ArrayList<>(List.of(choices.getFirst()));
        } else if (previous != null && !previous.isBlank() && choices.size() > 1) {
            choices.remove(previous);
        }
        for (String next : choices) {
            path.add(next);
            collectPaths(session, next, current, remaining - 1, path, result);
            path.removeLast();
            if (result.size() >= BoardSessionManager.MAX_ROUTE_BRANCHES) break;
        }
    }

    private static String encodePaths(BoardSession session, List<List<String>> paths) {
        StringJoiner routes = new StringJoiner("|");
        for (List<String> path : paths) routes.add(encodeNodePositions(session, path));
        return routes.toString();
    }

    private static String encodeNodePositions(BoardSession session, List<String> nodeIds) {
        StringJoiner joiner = new StringJoiner(";");
        for (String nodeId : nodeIds) {
            BlockPos pos = session.positions().get(nodeId);
            if (pos != null) joiner.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
        return joiner.toString();
    }

}