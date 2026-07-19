package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public record ScannedBoard(Map<String, BoardNode> nodes, Map<String, BlockPos> positions, BoardArea area, List<String> startNodes, List<String> errors) {

    public boolean isValid() {
        return this.errors.isEmpty() && this.startNodes.size() >= 4 && !this.nodes.isEmpty();
    }

}