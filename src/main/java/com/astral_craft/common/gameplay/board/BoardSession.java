package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;

public final class BoardSession {

    private final UUID id;
    private final ResourceKey<Level> dimension;
    private final Map<String, BoardNode> nodes;
    private final Map<String, BlockPos> positions;
    private final BoardArea protectedArea;
    private final BlockPos hologramCenter;
    private int ticksAlive;

    public BoardSession(UUID id, ResourceKey<Level> dimension, ScannedBoard board) {
        this.id = id;
        this.dimension = dimension;
        this.nodes = Map.copyOf(board.nodes());
        this.positions = Map.copyOf(board.positions());
        this.protectedArea = board.area();
        this.hologramCenter = board.area().center();
    }

    public UUID id() { return id; }
    public ResourceKey<Level> dimension() { return dimension; }
    public Map<String, BoardNode> nodes() { return nodes; }
    public Map<String, BlockPos> positions() { return positions; }
    public BoardArea protectedArea() { return protectedArea; }
    public BlockPos hologramCenter() { return hologramCenter; }
    public int ticksAlive() { return ticksAlive; }
    public void tick() { ticksAlive++; }

    public boolean protects(ResourceKey<Level> dimension, BlockPos pos) {
        return this.dimension.equals(dimension) && this.protectedArea.contains(pos);
    }

}