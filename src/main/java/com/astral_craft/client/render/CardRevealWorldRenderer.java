package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.reveal.CardRevealFrame;
import com.astral_craft.client.jpgloader.LoadedJpgTexture;
import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.client.render.CardRevealEntityOverlay.EntityCardReveal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("SameParameterValue")
public class CardRevealWorldRenderer {

    private static final float HALF_W = 33.0F;
    private static final float HALF_H = 48.0F;
    private static final float WORLD_SCALE = 0.0115F;
    private static final float HALF_THICKNESS = 1.65F;
    private static final float FRONT_ART_Z_OFFSET = 0.08F;
    private static final int EDGE_RGB = 0x40345F;

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || CardRevealEntityOverlay.activeReveals().isEmpty()) return;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (EntityCardReveal reveal : CardRevealEntityOverlay.activeReveals()) {
            Entity entity = level.getEntity(reveal.entityId());
            if (entity == null || !entity.isAlive()) continue;
            if (entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) continue;
            submitReveal(collector, poseStack, camera, cameraPos, entity, reveal);
        }
    }

    private static void submitReveal(SubmitNodeCollector collector, PoseStack poseStack, Camera camera, Vec3 cameraPos, Entity entity, EntityCardReveal reveal) {
        CardRevealFrame frame = CardRevealEntityOverlay.frame(reveal);
        int color = ((((int) (frame.alpha() * 255.0F)) & 0xFF) << 24) | 0xFFFFFF;
        if ((color >>> 24) <= 2) return;
        poseStack.pushPose();
        poseStack.translate(
                entity.getX() - cameraPos.x,
                entity.getY() - cameraPos.y + entity.getBbHeight() + 0.85D + frame.centerYOffset() * 0.012D,
                entity.getZ() - cameraPos.z);
        faceCameraHorizontally(poseStack, camera, entity);
        float xScale = WORLD_SCALE * frame.cardScale() * frame.widthScale();
        float yScale = WORLD_SCALE * frame.cardScale() * frame.heightScale();
        poseStack.scale(xScale, yScale, WORLD_SCALE);
        if (frame.front()) {
            try {
                Identifier frameTexture = frameTextureFor(reveal.cardType());
                LoadedJpgTexture loaded = ScopedJpgTextureCache.getOrLoad(reveal.frontTexture());
                submitCardBody(collector, poseStack, frameTexture, frameTexture, color, -HALF_W, HALF_H, HALF_W, -HALF_H, HALF_THICKNESS);
                submitTexturedQuad(collector, poseStack, loaded.textureId(), color, -24.0F, 38.0F, 24.0F, -10.0F,
                        HALF_THICKNESS + FRONT_ART_Z_OFFSET, 0.0F, 0.0F, 1.0F, 1.0F);
            } catch (IOException _) {}
        } else {
            submitCardBody(collector, poseStack, reveal.backTexture(), reveal.backTexture(), color, -HALF_W, HALF_H, HALF_W, -HALF_H, HALF_THICKNESS);
        }

        poseStack.popPose();
    }

    private static Identifier frameTextureFor(String cardType) {
        String type = cardType.isBlank() ? "effect" : cardType.toLowerCase(Locale.ROOT);
        return AstralCraft.prefix("textures/item/template_handcard_" + type + ".png");
    }

    private static void faceCameraHorizontally(PoseStack poseStack, Camera camera, Entity entity) {
        Vec3 cameraPos = camera.position();
        double dx = cameraPos.x - entity.getX();
        double dz = cameraPos.z - entity.getZ();
        float targetYaw = camera.yRot() + 180.0F;
        if (dx * dx + dz * dz > 1.0E-4D) {
            targetYaw = (float) (Math.atan2(dx, dz) * 180.0D / Math.PI);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(targetYaw));
    }

    private static void submitCardBody(
            SubmitNodeCollector collector, PoseStack poseStack, Identifier frontTexture, Identifier backTexture,
            int argb, float left, float top, float right, float bottom, float halfThickness) {
        int edgeColor = (argb & 0xFF000000) | EDGE_RGB;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(frontTexture), (pose, consumer) -> {
            frontFace(consumer, pose, left, top, right, bottom, halfThickness, argb, 0.0F, 0.0F, 1.0F, 1.0F);
            edgeFaces(consumer, pose, left, top, right, bottom, -halfThickness, halfThickness, edgeColor);
        });
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(backTexture), (pose, consumer) -> backFace(consumer, pose, left, top, right, bottom, -halfThickness, argb, 0.0F, 0.0F, 1.0F, 1.0F));
    }

    private static void submitTexturedQuad(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, int argb, float left, float top, float right, float bottom, float z, float u0, float v0, float u1, float v1) {
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, consumer) -> frontFace(consumer, pose, left, top, right, bottom, z, argb, u0, v0, u1, v1));
    }

    private static void frontFace(VertexConsumer consumer, PoseStack.Pose pose, float left, float top, float right, float bottom, float z, int argb, float u0, float v0, float u1, float v1) {
        vertex(consumer, pose, left, bottom, z, argb, u0, v1, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, right, bottom, z, argb, u1, v1, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, right, top, z, argb, u1, v0, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, left, top, z, argb, u0, v0, 0.0F, 0.0F, 1.0F);
    }

    private static void backFace(VertexConsumer consumer, PoseStack.Pose pose, float left, float top, float right, float bottom, float z, int argb, float u0, float v0, float u1, float v1) {
        vertex(consumer, pose, left, top, z, argb, u1, v0, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, right, top, z, argb, u0, v0, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, right, bottom, z, argb, u0, v1, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, left, bottom, z, argb, u1, v1, 0.0F, 0.0F, -1.0F);
    }

    private static void edgeFaces(VertexConsumer consumer, PoseStack.Pose pose, float left, float top, float right, float bottom, float backZ, float frontZ, int argb) {
        solidFace(consumer, pose, left, bottom, backZ, left, bottom, frontZ, left, top, frontZ, left, top, backZ, argb, -1.0F, 0.0F, 0.0F);
        solidFace(consumer, pose, right, bottom, frontZ, right, bottom, backZ, right, top, backZ, right, top, frontZ, argb, 1.0F, 0.0F, 0.0F);
        solidFace(consumer, pose, left, top, frontZ, right, top, frontZ, right, top, backZ, left, top, backZ, argb, 0.0F, 1.0F, 0.0F);
        solidFace(consumer, pose, left, bottom, backZ, right, bottom, backZ, right, bottom, frontZ, left, bottom, frontZ, argb, 0.0F, -1.0F, 0.0F);
    }

    private static void solidFace(VertexConsumer consumer, PoseStack.Pose pose,
                                  float x0, float y0, float z0, float x1, float y1, float z1,
                                  float x2, float y2, float z2, float x3, float y3, float z3,
                                  int argb, float normalX, float normalY, float normalZ) {
        vertex(consumer, pose, x0, y0, z0, argb, 0.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, x1, y1, z1, argb, 1.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, x2, y2, z2, argb, 1.0F, 0.0F, normalX, normalY, normalZ);
        vertex(consumer, pose, x3, y3, z3, argb, 0.0F, 0.0F, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int argb, float u, float v, float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, normalX, normalY, normalZ);
    }

}