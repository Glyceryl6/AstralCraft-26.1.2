package com.astral_craft.client.render;

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

/**
 * 3D dice renderer that does not depend on baked dice-face textures.
 *
 * <p>The cube body is submitted as custom geometry. Face labels are submitted as text so custom dice ranges can be
 * represented without preparing new textures.</p>
 */
public class AstralDiceRenderer extends EntityRenderer<AstralDiceEntity, AstralDiceRenderState> {

    private static final Identifier DICE_BODY_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final float HALF_SIZE = 0.35F;
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
    }

    @Override
    public void submit(AstralDiceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        applyDiceTransform(state, poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(DICE_BODY_TEXTURE), AstralDiceRenderer::renderCube);
        submitFaceTexts(state, poseStack, collector);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void applyDiceTransform(AstralDiceRenderState state, PoseStack poseStack) {
        poseStack.translate(state.mergeOffsetX, 0.45F, state.mergeOffsetZ);
        poseStack.scale(state.scale, state.scale, state.scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xSpin));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ySpin));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.zSpin));
    }

    private static void renderCube(PoseStack.Pose pose, VertexConsumer consumer) {
        float h = HALF_SIZE;
        quad(consumer, pose, -h, -h, h, h, -h, h, h, h, h, -h, h, h, 0.0F, 0.0F, 1.0F);      // front
        quad(consumer, pose, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0.0F, 0.0F, -1.0F); // back
        quad(consumer, pose, h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1.0F, 0.0F, 0.0F);      // right
        quad(consumer, pose, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1.0F, 0.0F, 0.0F); // left
        quad(consumer, pose, -h, h, h, h, h, h, h, h, -h, -h, h, -h, 0.0F, 1.0F, 0.0F);      // top
        quad(consumer, pose, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0.0F, -1.0F, 0.0F); // bottom
    }

    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float normalX, float normalY, float normalZ) {
        vertex(consumer, pose, x0, y0, z0, 0.0F, 0.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, x1, y1, z1, 1.0F, 0.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, x2, y2, z2, 1.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, x3, y3, z3, 0.0F, 1.0F, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v,
            float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private void submitFaceTexts(AstralDiceRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        submitFaceText(state.text, poseStack, collector, 0.0F, 0.0F, TEXT_OFFSET, 0.0F, 0.0F, 0.0F);
        submitFaceText(state.text, poseStack, collector, 0.0F, 0.0F, -TEXT_OFFSET, 0.0F, 180.0F, 0.0F);
        submitFaceText(state.text, poseStack, collector, TEXT_OFFSET, 0.0F, 0.0F, 0.0F, 90.0F, 0.0F);
        submitFaceText(state.text, poseStack, collector, -TEXT_OFFSET, 0.0F, 0.0F, 0.0F, -90.0F, 0.0F);
        submitFaceText(state.text, poseStack, collector, 0.0F, TEXT_OFFSET, 0.0F, -90.0F, 0.0F, 0.0F);
        submitFaceText(state.text, poseStack, collector, 0.0F, -TEXT_OFFSET, 0.0F, 90.0F, 0.0F, 0.0F);
    }

    private void submitFaceText(
            String text, PoseStack poseStack, SubmitNodeCollector collector,
            float x, float y, float z, float xRot, float yRot, float zRot) {
        FormattedCharSequence sequence = Component.literal(text).getVisualOrderText();
        float width = this.font.width(sequence);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        poseStack.scale(0.09F, -0.09F, 0.09F);
        collector.submitText(poseStack, -width / 2.0F, -4.5F, sequence, false, Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT, 0xFF111111, 0x00000000, 0);
        poseStack.popPose();
    }

}