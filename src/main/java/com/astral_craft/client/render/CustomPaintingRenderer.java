package com.astral_craft.client.render;

import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.io.IOException;

public class CustomPaintingRenderer extends EntityRenderer<CustomPaintingEntity, CustomPaintingRenderState> {

    private static final Identifier BLANK_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final Identifier MISSING_TEXTURE = Identifier.withDefaultNamespace("textures/block/magenta_glazed_terracotta.png");

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
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(state.texture), (pose, consumer) -> {
            EffectRenderGeometry.quad(consumer, pose,
                    -halfWidth, -halfHeight, 0.0F, halfWidth, -halfHeight, 0.0F,
                    halfWidth, halfHeight, 0.0F, -halfWidth, halfHeight, 0.0F,
                    0xFFFFFFFF, 0.0F, 0.0F, 1.0F);
            EffectRenderGeometry.quad(consumer, pose,
                    halfWidth, -halfHeight, -0.002F, -halfWidth, -halfHeight, -0.002F,
                    -halfWidth, halfHeight, -0.002F, halfWidth, halfHeight, -0.002F,
                    0xFFFFFFFF, 0.0F, 0.0F, -1.0F);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
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