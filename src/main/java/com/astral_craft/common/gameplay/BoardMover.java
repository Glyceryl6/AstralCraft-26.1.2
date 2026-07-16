package com.astral_craft.common.gameplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardMover {

    public static MovementResult walk(Map<String, BoardNode> nodes, String startNodeId, int steps) {
        return walk(nodes, startNodeId, steps, null);
    }

    public static MovementResult walk(Map<String, BoardNode> nodes, String startNodeId, int steps, String preferredNextNodeId) {
        if (steps < 0) {
            throw new IllegalArgumentException("steps must be >= 0");
        }
        BoardNode current = requireNode(nodes, startNodeId);
        List<MoveStep> route = new ArrayList<>();
        String preferred = preferredNextNodeId;

        for (int remaining = steps; remaining > 0; remaining--) {
            String nextId = chooseNext(current, preferred);
            current = requireNode(nodes, nextId);
            route.add(new MoveStep(current.id(), current.platformId(), remaining - 1, remaining == 1));
            preferred = null;
        }

        return new MovementResult(startNodeId, current.id(), steps, route);
    }

    private static BoardNode requireNode(Map<String, BoardNode> nodes, String id) {
        BoardNode node = nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("Unknown board node: " + id);
        }
        return node;
    }

    private static String chooseNext(BoardNode node, String preferredNextNodeId) {
        if (node.next().isEmpty()) {
            return node.id();
        }
        if (preferredNextNodeId != null && node.next().contains(preferredNextNodeId)) {
            return preferredNextNodeId;
        }
        return node.next().getFirst();
    }

}