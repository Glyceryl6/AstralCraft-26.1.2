package com.astral_craft.client.render.effect;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class FallingBrickRenderState extends EntityRenderState {
    public float age;
    public float fallTicks;
    public boolean hit;
    public float progress;
    public float breakProgress;
}
