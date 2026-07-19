package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;

/** Marker/capability for cards that place a board object on a logical panel. */
public interface BoardPanelPlacementCard {

    BoardTrapType boardTrapType();

    int boardPlacementRange();

    default boolean revealWhenPlaced() {
        return true;
    }

}