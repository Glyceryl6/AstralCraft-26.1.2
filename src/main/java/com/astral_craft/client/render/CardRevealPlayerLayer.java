package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.reveal.CardRevealFrame;
import com.astral_craft.client.render.CardRevealEntityOverlay.EntityCardReveal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import java.util.Locale;

public class CardRevealPlayerLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final float HALF_W = 33.0F;
    private static final float HALF_H = 48.0F;
    private static final float WORLD_SCALE = 0.0115F;

    public CardRevealPlayerLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        EntityCardReveal reveal = state.getRenderData(CardRevealEntityOverlay.CARD_REVEAL);
        if (reveal == null) reveal = CardRevealEntityOverlay.activeFor(state.id);
        if (reveal == null) return;
        CardRevealFrame frame = CardRevealEntityOverlay.frame(reveal);
        int color = ((((int) (frame.alpha() * 255.0F)) & 0xFF) << 24) | 0xFFFFFF;
        if ((color >>> 24) <= 2) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, 2.65D + frame.centerYOffset() * 0.012D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(WORLD_SCALE * frame.cardScale() * frame.widthScale(), WORLD_SCALE * frame.cardScale() * frame.heightScale(), WORLD_SCALE);
        if (frame.front()) {
            submitDoubleSidedQuad(collector, poseStack, this.frameTextureFor(reveal.cardType()), color, -HALF_W, HALF_H, HALF_W, -HALF_H, 0.0F, 0.0F, 1.0F, 1.0F);
            submitDoubleSidedQuad(collector, poseStack, reveal.frontTexture(), color, -24.0F, 28.0F, 24.0F, -20.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        } else {
            submitDoubleSidedQuad(collector, poseStack, reveal.backTexture(), color, -HALF_W, HALF_H, HALF_W, -HALF_H, 0.0F, 0.0F, 1.0F, 1.0F);
        }

        poseStack.popPose();
    }

    private Identifier frameTextureFor(String cardType) {
        String type = cardType.isBlank() ? "effect" : cardType.toLowerCase(Locale.ROOT);
        return AstralCraft.prefix("textures/item/template_handcard_" + type + ".png");
    }

    private static void submitDoubleSidedQuad(
            SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, int argb,
            float left, float top, float right, float bottom, float u0, float v0, float u1, float v1) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, consumer) -> {
            vertex(consumer, pose, left, top, 0.0F, argb, u0, v0, 0.0F, 0.0F, 1.0F);
            vertex(consumer, pose, right, top, 0.0F, argb, u1, v0, 0.0F, 0.0F, 1.0F);
            vertex(consumer, pose, right, bottom, 0.0F, argb, u1, v1, 0.0F, 0.0F, 1.0F);
            vertex(consumer, pose, left, bottom, 0.0F, argb, u0, v1, 0.0F, 0.0F, 1.0F);
            vertex(consumer, pose, left, bottom, 0.0F, argb, u0, v1, 0.0F, 0.0F, -1.0F);
            vertex(consumer, pose, right, bottom, 0.0F, argb, u1, v1, 0.0F, 0.0F, -1.0F);
            vertex(consumer, pose, right, top, 0.0F, argb, u1, v0, 0.0F, 0.0F, -1.0F);
            vertex(consumer, pose, left, top, 0.0F, argb, u0, v0, 0.0F, 0.0F, -1.0F);
        });
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int argb, float u, float v, float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }

}