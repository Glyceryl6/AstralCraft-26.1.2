package com.astral_craft.client.render.effect;

import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LaserStrikeRenderer extends EntityRenderer<LaserStrikeEntity, LaserStrikeRenderState> {

    private static final int SIDES = 10;

    public LaserStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public LaserStrikeRenderState createRenderState() {
        return new LaserStrikeRenderState();
    }

    @Override
    public void extractRenderState(LaserStrikeEntity entity, LaserStrikeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.age() + partialTick;
        state.growTicks = entity.growTicks();
        state.holdTicks = entity.holdTicks();
        state.fadeTicks = entity.fadeTicks();
        state.height = entity.beamHeight();
        state.radius = entity.radius();
        state.color = entity.color();
        state.targetOffset = new Vec3(0.0D, 0.0D, 0.0D);
    }

    @Override
    public void submit(LaserStrikeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float grow = Mth.clamp(state.age / Math.max(1.0F, state.growTicks), 0.0F, 1.0F);
        float fade = Mth.clamp((state.age - state.growTicks - state.holdTicks) / Math.max(1.0F, state.fadeTicks), 0.0F, 1.0F);
        float radius = state.radius * (1.0F - fade);
        if (radius > 0.003F) {
            Vec3 top = new Vec3(0.0D, state.height, 0.0D);
            Vec3 bottom = new Vec3(0.0D, state.height * (1.0F - grow), 0.0D);
            int core = withAlpha(state.color, 255.0F * (1.0F - fade));
            int glow = withAlpha(state.color, 90.0F * (1.0F - fade));
            float scroll = -state.age * 0.12F;
            RenderType type = RenderTypes.entityTranslucentEmissive(BeaconRenderer.BEAM_LOCATION);
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> EffectRenderGeometry.tube(pose, consumer, bottom, top, radius * 3.5F, glow, SIDES, scroll, 6.0F));
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> EffectRenderGeometry.tube(pose, consumer, bottom, top, radius, core, SIDES, scroll * 1.8F, 12.0F));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Mth.clamp(Math.round(alpha), 0, 255);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

}