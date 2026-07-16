package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import net.minecraft.server.level.ServerLevel;

/** Immutable inputs supplied to a platform when a board pawn reaches it by normal movement. */
public record BoardPanelContext(
        ServerLevel level,
        BoardSession session,
        BoardParticipant participant,
        BoardNode node,
        boolean landing) {}
