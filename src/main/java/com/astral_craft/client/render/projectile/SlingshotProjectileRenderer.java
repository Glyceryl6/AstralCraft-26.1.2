package com.astral_craft.client.render.projectile;

import com.astral_craft.common.entity.projectile.SlingshotProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class SlingshotProjectileRenderer extends CubeProjectileRenderer<SlingshotProjectileEntity> {

    public SlingshotProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, Identifier.withDefaultNamespace("textures/block/stone.png"), 0.13F);
    }

}