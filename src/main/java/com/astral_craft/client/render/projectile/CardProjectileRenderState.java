package com.astral_craft.client.render.projectile;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class CardProjectileRenderState extends EntityRenderState {

    public boolean hit;
    public float age;
    public float duration;
    public Vec3 tangent = new Vec3(0.0D, 1.0D, 0.0D);

}