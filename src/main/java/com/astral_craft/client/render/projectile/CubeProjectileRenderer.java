package com.astral_craft.client.render.projectile;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.entity.projectile.AbstractCardProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CubeProjectileRenderer<T extends AbstractCardProjectileEntity> extends AbstractCardProjectileRenderer<T, CardProjectileRenderState> {

    private final Identifier texture;
    private final float baseScale;

    public CubeProjectileRenderer(EntityRendererProvider.Context context, Identifier texture, float baseScale) {
        super(context);
        this.texture = texture;
        this.baseScale = baseScale;
    }

    @Override
    public CardProjectileRenderState createRenderState() {
        return new CardProjectileRenderState();
    }

    @Override
    protected void submitProjectile(CardProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float fade = state.hit ? Mth.clamp((state.age - state.duration) / 10.0F, 0.0F, 1.0F) : 0.0F;
        float scale = this.baseScale * (1.0F - fade);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(this.texture), (pose, consumer) -> EffectRenderGeometry.cube(pose, consumer, scale, 0xFFFFFFFF));
    }

}