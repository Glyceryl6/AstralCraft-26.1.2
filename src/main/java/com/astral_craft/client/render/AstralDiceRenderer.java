package com.astral_craft.client.render;

import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;

import java.util.Map;
import java.util.WeakHashMap;

/** 3D dice renderer using a 4x3 appearance texture with the current 1-10 result drawn separately. */
public class AstralDiceRenderer extends EntityRenderer<AstralDiceEntity, AstralDiceRenderState> {

    public static final float HALF_SIZE = 0.35F;
    public static final String ITEM_FACE_TEXT = "10";
    private static final float TEXT_OFFSET = HALF_SIZE + 0.011F;
    private final Font font;
    private final Map<AstralDiceEntity, Double> animationStartTicks = new WeakHashMap<>();

    public AstralDiceRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
        this.shadowRadius = 0.35F;
    }

    @Override
    public AstralDiceRenderState createRenderState() {
        return new AstralDiceRenderState();
    }

    @Override
    public void extractRenderState(AstralDiceEntity entity, AstralDiceRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        double startedAtTick = this.animationStartTicks.computeIfAbsent(entity, ignored ->
                ClientAnimationClock.nowTicks() - Math.max(0, entity.tickCount));
        float ageTicks = ClientAnimationClock.elapsedTicks(startedAtTick);
        state.text = entity.faceText(ageTicks);
        state.xSpin = entity.xSpin(ageTicks);
        state.ySpin = entity.ySpin(ageTicks);
        state.zSpin = entity.zSpin(ageTicks);
        float mergeProgress = entity.mergeProgress(ageTicks);
        state.mergeOffsetX = entity.mergeOffsetX() * mergeProgress;
        state.mergeOffsetZ = entity.mergeOffsetZ() * mergeProgress;
        state.scale = entity.renderScale(ageTicks);
        state.texture = entity.texture();
    }

    @Override
    public void submit(AstralDiceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        applyDiceTransform(state, poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(ScopedJpgTextureCache.resolve(state.texture)),
                (pose, consumer) -> renderCube(pose, consumer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
        submitFaceTexts(this.font, state.text, poseStack, collector);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static void renderItem(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, String text,
                                  Font font, int lightCoords, int overlayCoords) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(ScopedJpgTextureCache.resolve(texture)),
                (pose, consumer) -> renderCube(pose, consumer, lightCoords, overlayCoords));
        submitFaceTexts(font, text, poseStack, collector);
    }

    private static void applyDiceTransform(AstralDiceRenderState state, PoseStack poseStack) {
        poseStack.translate(state.mergeOffsetX, 0.45F, state.mergeOffsetZ);
        poseStack.scale(state.scale, state.scale, state.scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xSpin));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ySpin));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.zSpin));
    }

    public static void renderCube(PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, int overlayCoords) {
        float h = HALF_SIZE;
        quad(consumer, pose, -h, -h, h, h, -h, h, h, h, h, -h, h, h, 0.0F, 0.0F, 1.0F, 1, 1, lightCoords, overlayCoords);      // front
        quad(consumer, pose, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0.0F, 0.0F, -1.0F, 3, 1, lightCoords, overlayCoords); // back
        quad(consumer, pose, h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1.0F, 0.0F, 0.0F, 2, 1, lightCoords, overlayCoords);      // right
        quad(consumer, pose, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1.0F, 0.0F, 0.0F, 0, 1, lightCoords, overlayCoords); // left
        quad(consumer, pose, -h, h, h, h, h, h, h, h, -h, -h, h, -h, 0.0F, 1.0F, 0.0F, 1, 0, lightCoords, overlayCoords);      // top
        quad(consumer, pose, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0.0F, -1.0F, 0.0F, 1, 2, lightCoords, overlayCoords); // bottom
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float normalX, float normalY, float normalZ, int cellX, int cellY,
                             int lightCoords, int overlayCoords) {
        float u0 = cellX / 4.0F;
        float u1 = (cellX + 1) / 4.0F;
        float v0 = cellY / 3.0F;
        float v1 = (cellY + 1) / 3.0F;
        vertex(consumer, pose, x0, y0, z0, u0, v0, normalX, normalY, normalZ, lightCoords, overlayCoords);
        vertex(consumer, pose, x1, y1, z1, u1, v0, normalX, normalY, normalZ, lightCoords, overlayCoords);
        vertex(consumer, pose, x2, y2, z2, u1, v1, normalX, normalY, normalZ, lightCoords, overlayCoords);
        vertex(consumer, pose, x3, y3, z3, u0, v1, normalX, normalY, normalZ, lightCoords, overlayCoords);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ, int lightCoords, int overlayCoords) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(overlayCoords).setLight(lightCoords).setNormal(pose, normalX, normalY, normalZ);
    }

    private static void submitFaceTexts(Font font, String text, PoseStack poseStack, SubmitNodeCollector collector) {
        submitFaceText(font, text, poseStack, collector, 0.0F, 0.0F, TEXT_OFFSET, 0.0F, 0.0F, 0.0F);
        submitFaceText(font, text, poseStack, collector, 0.0F, 0.0F, -TEXT_OFFSET, 0.0F, 180.0F, 0.0F);
        submitFaceText(font, text, poseStack, collector, TEXT_OFFSET, 0.0F, 0.0F, 0.0F, 90.0F, 0.0F);
        submitFaceText(font, text, poseStack, collector, -TEXT_OFFSET, 0.0F, 0.0F, 0.0F, -90.0F, 0.0F);
        submitFaceText(font, text, poseStack, collector, 0.0F, TEXT_OFFSET, 0.0F, -90.0F, 0.0F, 0.0F);
        submitFaceText(font, text, poseStack, collector, 0.0F, -TEXT_OFFSET, 0.0F, 90.0F, 0.0F, 0.0F);
    }

    private static void submitFaceText(Font font, String text, PoseStack poseStack, SubmitNodeCollector collector,
                                       float x, float y, float z, float xRot, float yRot, float zRot) {
        FormattedCharSequence sequence = Component.literal(text).getVisualOrderText();
        float width = font.width(sequence);
        float scale = text.length() > 1 ? 0.072F : 0.09F;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        poseStack.scale(scale, -scale, scale);
        collector.submitText(poseStack, -width / 2.0F, -4.5F, sequence, false, Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT, 0xFF171221, 0x00000000, 0);
        poseStack.popPose();
    }

}