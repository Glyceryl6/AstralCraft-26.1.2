package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.reveal.CardRevealFrame;
import com.astral_craft.client.render.CardRevealEntityOverlay.EntityCardReveal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

public class CardRevealPlayerLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier FRONT_FRAME = AstralCraft.prefix("textures/item/template_handcard_effect.png");
    private static final float HALF_W = 33.0F;
    private static final float HALF_H = 48.0F;
    private static final float WORLD_SCALE = 0.0115F;

    public CardRevealPlayerLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        EntityCardReveal reveal = state.getRenderDataOrDefault(CardRevealEntityOverlay.CARD_REVEAL, null);
        if (reveal == null) {
            return;
        }

        CardRevealFrame frame = CardRevealEntityOverlay.frame(reveal);
        int color = ((((int) (frame.alpha() * 255.0F)) & 0xFF) << 24) | 0xFFFFFF;
        if ((color >>> 24) <= 2) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 2.45D + frame.centerYOffset() * 0.012D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.scale(WORLD_SCALE * frame.cardScale() * frame.widthScale(), WORLD_SCALE * frame.cardScale() * frame.heightScale(), WORLD_SCALE);
        if (frame.front()) {
            submitQuad(collector, poseStack, FRONT_FRAME, color, -HALF_W, HALF_H, HALF_W, -HALF_H, 0.0F, 0.0F, CARD_FRAME_U(), CARD_FRAME_V());
            submitQuad(collector, poseStack, reveal.frontTexture(), color, -24.0F, 28.0F, 24.0F, -20.0F, 0.0F, 0.0F, 48.0F / 256.0F, 48.0F / 360.0F);
        } else {
            submitQuad(collector, poseStack, reveal.backTexture(), color, -HALF_W, HALF_H, HALF_W, -HALF_H, 0.0F, 0.0F, 1.0F, 1.0F);
        }
        poseStack.popPose();
    }

    private static float CARD_FRAME_U() {
        return 66.0F / 256.0F;
    }

    private static float CARD_FRAME_V() {
        return 96.0F / 360.0F;
    }

    private static void submitQuad(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, int argb,
                                   float left, float top, float right, float bottom,
                                   float u0, float v0, float u1, float v1) {
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, consumer) -> {
            vertex(consumer, pose, left, top, 0.0F, argb, u0, v0);
            vertex(consumer, pose, right, top, 0.0F, argb, u1, v0);
            vertex(consumer, pose, right, bottom, 0.0F, argb, u1, v1);
            vertex(consumer, pose, left, bottom, 0.0F, argb, u0, v1);
        });
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int argb, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

}
