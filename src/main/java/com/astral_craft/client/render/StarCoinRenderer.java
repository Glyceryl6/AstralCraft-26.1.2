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
        this.shadowRadius = 0.22F;
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
        state.progress = entity.visualProgress(partialTick);
    }

    @Override
    public void submit(StarCoinRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        int layers = state.kind == StarCoinEntity.Kind.PILE ? Math.clamp(state.amount, 1, 5) : 1;
        float size = state.kind == StarCoinEntity.Kind.PILE
                ? Math.min(0.24F, 0.20F + Math.max(0, state.amount - 1) * 0.004F) : 0.15F;
        int alpha = state.kind == StarCoinEntity.Kind.LOSS
                ? Math.clamp(Math.round((1.0F - state.progress) * 255.0F), 0, 255) : 255;
        if (state.kind == StarCoinEntity.Kind.LOSS) size *= 1.0F - state.progress * 0.4F;
        for (int layer = 0; layer < layers; layer++) {
            poseStack.pushPose();
            double bob = state.kind == StarCoinEntity.Kind.PILE
                    ? Math.sin((state.age + layer * 2.0F) * 0.12F) * 0.008D
                    : Math.sin(state.age * 0.16F) * 0.045D;
            if (state.kind == StarCoinEntity.Kind.PILE && layers > 1) {
                double angle = Math.PI * 2.0D * layer / layers + state.age * 0.008D;
                double radius = layers == 2 ? 0.16D : 0.21D;
                poseStack.translate(Math.cos(angle) * radius, bob + layer % 2 * 0.035D, Math.sin(angle) * radius);
            } else {
                poseStack.translate(0.0D, bob, 0.0D);
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * (state.kind == StarCoinEntity.Kind.PILE ? 2.0F : 7.0F) + layer * 29.0F));
            submitCoin(poseStack, collector, size, alpha);
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static void submitCoin(PoseStack poseStack, SubmitNodeCollector collector, float halfSize, int alpha) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, consumer) -> {
            EffectRenderGeometry.quad(consumer, pose,
                    -halfSize, -halfSize, 0.008F, halfSize, -halfSize, 0.008F,
                    halfSize, halfSize, 0.008F, -halfSize, halfSize, 0.008F,
                    alpha << 24 | 0xFFFFFF, 0.0F, 0.0F, 1.0F);
            EffectRenderGeometry.quad(consumer, pose,
                    halfSize, -halfSize, -0.008F, -halfSize, -halfSize, -0.008F,
                    -halfSize, halfSize, -0.008F, halfSize, halfSize, -0.008F,
                    alpha << 24 | 0xFFFFFF, 0.0F, 0.0F, -1.0F);
        });
    }

}