package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.entity.StarCoinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class StarCoinRenderer extends EntityRenderer<StarCoinEntity, StarCoinRenderState> {

    private static final Identifier TEXTURE = AstralCraft.prefix("textures/item/star_coin.png");

    public StarCoinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.12F;
    }

    @Override
    public StarCoinRenderState createRenderState() {
        return new StarCoinRenderState();
    }

    @Override
    public void extractRenderState(StarCoinEntity entity, StarCoinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.kind();
        state.age = entity.visualAge(partialTick);
        state.amount = entity.amount();
    }

    @Override
    public void submit(StarCoinRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        int layers = state.kind == StarCoinEntity.Kind.PILE ? Math.clamp(state.amount, 1, 6) : 1;
        float size = state.kind == StarCoinEntity.Kind.PILE ? Math.min(0.18F, 0.11F + Math.max(0, state.amount - 1) * 0.008F) : 0.12F;
        for (int layer = 0; layer < layers; layer++) {
            poseStack.pushPose();
            double bob = state.kind == StarCoinEntity.Kind.PILE
                    ? Math.sin((state.age + layer * 2.0F) * 0.12F) * 0.006D
                    : Math.sin(state.age * 0.16F) * 0.045D;
            poseStack.translate(0.0D, layer * 0.034D + bob, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * (state.kind == StarCoinEntity.Kind.PILE ? 2.0F : 7.0F) + layer * 23.0F));
            submitCoin(poseStack, collector, size);
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void submitCoin(PoseStack poseStack, SubmitNodeCollector collector, float halfSize) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, consumer) -> {
            EffectRenderGeometry.quad(consumer, pose,
                    -halfSize, -halfSize, 0.008F, halfSize, -halfSize, 0.008F,
                    halfSize, halfSize, 0.008F, -halfSize, halfSize, 0.008F,
                    0xFFFFFFFF, 0.0F, 0.0F, 1.0F);
            EffectRenderGeometry.quad(consumer, pose,
                    halfSize, -halfSize, -0.008F, -halfSize, -halfSize, -0.008F,
                    -halfSize, halfSize, -0.008F, halfSize, halfSize, -0.008F,
                    0xFFFFFFFF, 0.0F, 0.0F, -1.0F);
        });
    }

}