package com.astral_craft.client.render.projectile;

import com.astral_craft.common.entity.projectile.AbstractCardProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractCardProjectileRenderer<T extends AbstractCardProjectileEntity, S extends CardProjectileRenderState> extends EntityRenderer<T, S> {

    protected AbstractCardProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.hit = entity.hit();
        state.age = entity.age() + partialTick;
        state.duration = entity.durationTicks();
        Vec3 motion = entity.getDeltaMovement();
        state.tangent = motion.lengthSqr() > 1.0E-7D ? motion.normalize() : entity.tangent();
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        rotateAlongTangent(poseStack, state.tangent);
        this.submitProjectile(state, poseStack, collector, cameraState);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    protected abstract void submitProjectile(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState);

    protected static void rotateAlongTangent(PoseStack poseStack, Vec3 tangent) {
        Vec3 dir = tangent.lengthSqr() < 1.0E-7D ? new Vec3(0.0D, 1.0D, 0.0D) : tangent.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
        float horizontal = (float) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float pitch = (float) Math.toDegrees(-Math.atan2(dir.y, horizontal));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

}