package com.astral_craft.client.render.effect;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class LaserStrikeRenderState extends EntityRenderState {
    public float age;
    public int growTicks;
    public int holdTicks;
    public int fadeTicks;
    public float height;
    public float radius;
    public int color;
    public Vec3 targetOffset = Vec3.ZERO;
}
