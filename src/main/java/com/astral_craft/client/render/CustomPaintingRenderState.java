package com.astral_craft.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public class CustomPaintingRenderState extends EntityRenderState {
    public Identifier texture = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    public Direction facing = Direction.NORTH;
    public int width = 1;
    public int height = 1;
}
