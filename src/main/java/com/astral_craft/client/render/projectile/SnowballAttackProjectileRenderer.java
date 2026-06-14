package com.astral_craft.client.render.projectile;

import com.astral_craft.common.entity.projectile.SnowballAttackProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class SnowballAttackProjectileRenderer extends CubeProjectileRenderer<SnowballAttackProjectileEntity> {

    public SnowballAttackProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, Identifier.withDefaultNamespace("textures/block/snow.png"), 0.16F);
    }

}