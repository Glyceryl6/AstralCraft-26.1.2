package com.astral_craft.client.render;

import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.io.IOException;

public class CustomPaintingRenderer extends EntityRenderer<CustomPaintingEntity, CustomPaintingRenderState> {

    private static final Identifier BLANK_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final Identifier MISSING_TEXTURE = Identifier.withDefaultNamespace("textures/block/magenta_glazed_terracotta.png");
    private static final Identifier PAINTING_BACK_TEXTURE = Identifier.withDefaultNamespace("textures/painting/back.png");
    private static final float HALF_DEPTH = 1.0F / 32.0F;
    private static final float FRAME_WIDTH = 1.0F / 16.0F;

    public CustomPaintingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public CustomPaintingRenderState createRenderState() {
        return new CustomPaintingRenderState();
    }

    @Override
    public void extractRenderState(CustomPaintingEntity entity, CustomPaintingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        CustomPaintingData data = entity.data();
        state.facing = entity.facing();
        state.width = data.width();
        state.height = data.height();
        state.texture = resolveTexture(data);
    }

    @Override
    public void submit(CustomPaintingRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation(state.facing)));
        float halfWidth = state.width * 0.5F;
        float halfHeight = state.height * 0.5F;
        float inset = Math.min(FRAME_WIDTH, Math.min(halfWidth, halfHeight) * 0.25F);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(PAINTING_BACK_TEXTURE),
                (pose, consumer) -> renderVanillaFrame(pose, consumer, state, halfWidth, halfHeight, inset));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(state.texture),
                (pose, consumer) -> quad(consumer, pose,
                        -halfWidth + inset, -halfHeight + inset, HALF_DEPTH + 0.001F,
                        halfWidth - inset, -halfHeight + inset, HALF_DEPTH + 0.001F,
                        halfWidth - inset, halfHeight - inset, HALF_DEPTH + 0.001F,
                        -halfWidth + inset, halfHeight - inset, HALF_DEPTH + 0.001F,
                        0.0F, 0.0F, 1.0F, state.lightCoords));
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    private static void renderVanillaFrame(PoseStack.Pose pose, VertexConsumer consumer, CustomPaintingRenderState state,
                                           float halfWidth, float halfHeight, float inset) {
        float front = HALF_DEPTH;
        float back = -HALF_DEPTH;
        for (int x = 0; x < state.width; x++) {
            float left = -halfWidth + x;
            float right = left + 1.0F;
            for (int y = 0; y < state.height; y++) {
                float bottom = -halfHeight + y;
                float top = bottom + 1.0F;
                quad(consumer, pose, left, bottom, back, left, top, back, right, top, back, right, bottom, back,
                        0.0F, 0.0F, -1.0F, state.lightCoords);
            }
            quad(consumer, pose, left, -halfHeight, back, right, -halfHeight, back,
                    right, -halfHeight, front, left, -halfHeight, front, 0.0F, -1.0F, 0.0F, state.lightCoords);
            quad(consumer, pose, left, halfHeight, front, right, halfHeight, front,
                    right, halfHeight, back, left, halfHeight, back, 0.0F, 1.0F, 0.0F, state.lightCoords);
            quad(consumer, pose, left, -halfHeight, front, right, -halfHeight, front,
                    right, -halfHeight + inset, front, left, -halfHeight + inset, front,
                    0.0F, 0.0F, 1.0F, state.lightCoords);
            quad(consumer, pose, left, halfHeight - inset, front, right, halfHeight - inset, front,
                    right, halfHeight, front, left, halfHeight, front, 0.0F, 0.0F, 1.0F, state.lightCoords);
        }
        for (int y = 0; y < state.height; y++) {
            float bottom = -halfHeight + y;
            float top = bottom + 1.0F;
            quad(consumer, pose, -halfWidth, bottom, back, -halfWidth, bottom, front,
                    -halfWidth, top, front, -halfWidth, top, back, -1.0F, 0.0F, 0.0F, state.lightCoords);
            quad(consumer, pose, halfWidth, bottom, front, halfWidth, bottom, back,
                    halfWidth, top, back, halfWidth, top, front, 1.0F, 0.0F, 0.0F, state.lightCoords);
            quad(consumer, pose, -halfWidth, bottom, front, -halfWidth + inset, bottom, front,
                    -halfWidth + inset, top, front, -halfWidth, top, front, 0.0F, 0.0F, 1.0F, state.lightCoords);
            quad(consumer, pose, halfWidth - inset, bottom, front, halfWidth, bottom, front,
                    halfWidth, top, front, halfWidth - inset, top, front, 0.0F, 0.0F, 1.0F, state.lightCoords);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz, int light) {
        vertex(consumer, pose, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz, light);
        vertex(consumer, pose, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz, light);
        vertex(consumer, pose, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz, light);
        vertex(consumer, pose, x3, y3, z3, 0.0F, 1.0F, nx, ny, nz, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int light) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static Identifier resolveTexture(CustomPaintingData data) {
        if (!data.configured()) return BLANK_TEXTURE;
        try {
            return data.jpg() ? ScopedJpgTextureCache.getOrLoad(data.resourceId()).textureId() : data.resourceId();
        } catch (IOException | RuntimeException ignored) {
            return MISSING_TEXTURE;
        }
    }

    private static float rotation(Direction direction) {
        return switch (direction) {
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

}