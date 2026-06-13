package com.astral_craft.common.gameplay;

import java.util.List;

public record MovementResult(String startNodeId, String endNodeId, int requestedSteps, List<MoveStep> route) {

    public MovementResult {
        route = List.copyOf(route);
    }
}
