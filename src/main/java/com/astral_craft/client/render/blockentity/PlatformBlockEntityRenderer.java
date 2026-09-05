package com.astral_craft.client.render.blockentity;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.common.blockentity.PlatformBlockEntity;
import com.astral_craft.common.blocks.platform.StartPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlatformBlockEntityRenderer implements BlockEntityRenderer<PlatformBlockEntity, PlatformBlockEntityRenderer.RenderState> {

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(PlatformBlockEntity blockEntity, RenderState state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.texture = blockEntity.getBlockState().getBlock() instanceof StartPlatform && blockEntity.characterId() != null
                ? AstralStatusIconRenderer.characterSkinTexture(blockEntity.characterId(), blockEntity.skinId().getPath()) : null;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.texture == null) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(state.texture),
                (pose, consumer) -> {
                    renderPortraitQuad(pose, consumer, state.lightCoords, 8.0F, 8.0F, 16.0F, 16.0F, 0.068F);
                    renderPortraitQuad(pose, consumer, state.lightCoords, 40.0F, 8.0F, 48.0F, 16.0F, 0.069F);
                });
    }

    private static void renderPortraitQuad(PoseStack.Pose pose, VertexConsumer consumer, int light,
                                           float u0Pixels, float v0Pixels, float u1Pixels, float v1Pixels, float y) {
        float min = 0.22F;
        float max = 0.78F;
        float u0 = u0Pixels / 64.0F;
        float v0 = v0Pixels / 64.0F;
        float u1 = u1Pixels / 64.0F;
        float v1 = v1Pixels / 64.0F;
        vertex(consumer, pose, min, y, max, u0, v1, light);
        vertex(consumer, pose, max, y, max, u1, v1, light);
        vertex(consumer, pose, max, y, min, u1, v0, light);
        vertex(consumer, pose, min, y, min, u0, v0, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static class RenderState extends BlockEntityRenderState {
        private @Nullable Identifier texture;
    }
}
