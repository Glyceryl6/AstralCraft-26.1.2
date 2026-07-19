package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Capability for cards that place a board object on a logical panel. */
public interface BoardPanelPlacementCard {

    BoardTrapType boardTrapType();

    int boardPlacementRange();

    default boolean revealWhenPlaced() {
        return true;
    }

    default Optional<String> randomValidNode(BoardSession session, BoardParticipant source, RandomSource random) {
        List<String> valid = new ArrayList<>();
        for (String nodeId : session.nodes().keySet()) {
            int distance = BoardRouteService.graphDistance(session, source.currentNodeKey(), nodeId, this.boardPlacementRange());
            if (distance >= 0 && distance <= this.boardPlacementRange()) valid.add(nodeId);
        }

        return valid.isEmpty() ? Optional.empty() : Optional.of(valid.get(random.nextInt(valid.size())));
    }

}