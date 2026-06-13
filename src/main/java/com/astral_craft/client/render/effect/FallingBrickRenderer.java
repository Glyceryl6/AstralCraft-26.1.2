package com.astral_craft.client.render.effect;

import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class FallingBrickRenderer extends EntityRenderer<FallingBrickEntity, FallingBrickRenderState> {

    private static final Identifier BRICK_TEXTURE = Identifier.withDefaultNamespace("textures/block/bricks.png");

    public FallingBrickRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public FallingBrickRenderState createRenderState() {
        return new FallingBrickRenderState();
    }

    @Override
    public void extractRenderState(FallingBrickEntity entity, FallingBrickRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.age() + partialTick;
        state.fallTicks = entity.fallTicks();
        state.hit = entity.hit();
        state.progress = entity.progress(partialTick);
        state.breakProgress = entity.breakProgress(partialTick);
    }

    @Override
    public void submit(FallingBrickRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.hit) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 12.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.age * 7.0F));
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(BRICK_TEXTURE), (pose, consumer) -> EffectRenderGeometry.cube(pose, consumer, 0.28F, 0xFFFFFFFF));
            poseStack.popPose();
        } else {
            float fade = 1.0F - state.breakProgress;
            int color = ((int) (fade * 255.0F) << 24) | 0xFFFFFF;
            for (int i = 0; i < 6; i++) {
                poseStack.pushPose();
                float angle = (float) (Math.PI * 2.0D * i / 6.0D);
                float dist = state.breakProgress * 0.58F;
                poseStack.translate(Math.cos(angle) * dist, state.breakProgress * 0.18F, Math.sin(angle) * dist);
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 61.0F + state.age * 10.0F));
                collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(BRICK_TEXTURE), (pose, consumer) -> EffectRenderGeometry.cube(pose, consumer, 0.10F * fade, color));
                poseStack.popPose();
            }
        }

        super.submit(state, poseStack, collector, cameraState);
    }

}