package com.astral_craft.client.render.effect;

import com.astral_craft.common.entity.visual.ArcProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ArcProjectileRenderer extends EntityRenderer<ArcProjectileEntity, ArcProjectileRenderState> {

    private static final Identifier FIREWORK_TEXTURE = Identifier.withDefaultNamespace("textures/item/firework_rocket.png");
    private static final Identifier SLINGSHOT_TEXTURE = Identifier.withDefaultNamespace("textures/block/sand.png");

    public ArcProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ArcProjectileRenderState createRenderState() {
        return new ArcProjectileRenderState();
    }

    @Override
    public void extractRenderState(ArcProjectileEntity entity, ArcProjectileRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.mode = entity.mode();
        state.hit = entity.hit();
        state.age = entity.age() + partialTick;
        state.duration = entity.durationTicks();
        Entity target = entity.level().getEntity(entity.targetId());
        if (target instanceof LivingEntity living) {
            state.tangent = entity.tangent((entity.age() + partialTick) / (float) entity.durationTicks(), living);
        } else if (entity.getDeltaMovement().lengthSqr() > 1.0E-7D) {
            state.tangent = entity.getDeltaMovement().normalize();
        }
    }

    @Override
    public void submit(ArcProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        renderTrail(state, poseStack, collector);
        poseStack.pushPose();
        rotateAlongTangent(poseStack, state.tangent);
        if (state.mode == ArcProjectileEntity.MODE_FIRECRACKER) {
            float fade = state.hit ? Mth.clamp((state.age - state.duration) / 10.0F, 0.0F, 1.0F) : 0.0F;
            float scale = 0.42F * (1.0F - fade);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(FIREWORK_TEXTURE),
                    (pose, consumer) -> quadBillboard(pose, consumer, scale, 0xFFFFFFFF));
        } else {
            float fade = state.hit ? Mth.clamp((state.age - state.duration) / 10.0F, 0.0F, 1.0F) : 0.0F;
            float scale = 0.13F * (1.0F - fade);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(SLINGSHOT_TEXTURE),
                    (pose, consumer) -> EffectRenderGeometry.cube(pose, consumer, scale, 0xFFFFFFFF));
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void renderTrail(ArcProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.hit) return;
        Vec3 tail = state.tangent.scale(-0.65D);
        int color = state.mode == ArcProjectileEntity.MODE_FIRECRACKER ? 0xAAFF9B42 : 0x8888CCFF;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(BeaconRenderer.BEAM_LOCATION),
                (pose, consumer) -> EffectRenderGeometry.tube(pose, consumer, tail, Vec3.ZERO, 0.035F, color, 6, -state.age * 0.15F, 2.0F));
    }

    private static void rotateAlongTangent(PoseStack poseStack, Vec3 tangent) {
        Vec3 dir = tangent.lengthSqr() < 1.0E-7D ? new Vec3(0.0D, 1.0D, 0.0D) : tangent.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    private static void quadBillboard(PoseStack.Pose pose, VertexConsumer consumer, float scale, int argb) {
        vertex(consumer, pose, -scale * 0.45F, -scale, 0.0F, 0.0F, 1.0F, argb);
        vertex(consumer, pose, scale * 0.45F, -scale, 0.0F, 1.0F, 1.0F, argb);
        vertex(consumer, pose, scale * 0.45F, scale, 0.0F, 1.0F, 0.0F, argb);
        vertex(consumer, pose, -scale * 0.45F, scale, 0.0F, 0.0F, 0.0F, argb);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

}