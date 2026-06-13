package com.astral_craft.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class SoulLinkRenderState extends EntityRenderState {
    public boolean visible;
    public Vec3 start = Vec3.ZERO;
    public Vec3 end = Vec3.ZERO;
    public float arcHeight;
    public float thickness;
    public int color;
    public boolean rainbow;
    public float age;
}