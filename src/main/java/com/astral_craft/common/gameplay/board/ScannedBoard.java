package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public record ScannedBoard(Map<String, BoardNode> nodes, Map<String, BlockPos> positions, BoardArea area,
                           List<String> startNodes, BoardMode mode, List<String> errors) {

    public ScannedBoard withMode(BoardMode mode) {
        return new ScannedBoard(this.nodes, this.positions, this.area, this.startNodes,
                mode == null ? BoardMode.UNDECIDED : mode, this.errors);
    }

    public boolean isValid() {
        return this.errors.isEmpty() && this.startNodes.size() >= 4 && !this.nodes.isEmpty();
    }

}