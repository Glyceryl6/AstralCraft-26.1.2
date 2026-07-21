package com.astral_craft.client.render;

import com.astral_craft.common.entity.StarCoinEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class StarCoinRenderState extends EntityRenderState {

    public StarCoinEntity.Kind kind = StarCoinEntity.Kind.PILE;
    public float age;
    public int amount = 1;
    public float progress;
}
