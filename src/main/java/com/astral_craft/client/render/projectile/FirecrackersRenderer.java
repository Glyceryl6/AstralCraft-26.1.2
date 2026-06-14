package com.astral_craft.client.render.projectile;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.model.entity.FirecrackersModel;
import com.astral_craft.common.entity.projectile.FirecrackersProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class FirecrackersRenderer extends EntityRenderer<FirecrackersProjectileEntity, FirecrackersRenderState> {

    private final FirecrackersModel model;

    public FirecrackersRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FirecrackersModel(context.bakeLayer(FirecrackersModel.LAYER));
    }

    public void submit(FirecrackersRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot + 90.0F));
        submitNodeCollector.submitModel(this.model, state, poseStack,
                AstralCraft.prefix("textures/entity/firecrackers.png"),
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public FirecrackersRenderState createRenderState() {
        return new FirecrackersRenderState();
    }

    public void extractRenderState(FirecrackersProjectileEntity entity, FirecrackersRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.yRot = entity.getYRot(partialTicks);
    }

}