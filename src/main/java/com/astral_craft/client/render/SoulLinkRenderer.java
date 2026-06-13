package com.astral_craft.client.render;

import com.astral_craft.common.entity.SoulLinkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SoulLinkRenderer extends EntityRenderer<SoulLinkEntity, SoulLinkRenderState> {

    private static final Identifier CHAIN_TEXTURE = Identifier.withDefaultNamespace("textures/block/cobweb.png");
    private static final int SEGMENTS = 32;
    private static final int TUBE_SIDES = 8;
    private static final float GLOW_RADIUS_MULTIPLIER = 2.85F;
    private static final float CORE_ALPHA = 1.0F;
    private static final float GLOW_ALPHA = 0.32F;

    public SoulLinkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public SoulLinkRenderState createRenderState() {
        return new SoulLinkRenderState();
    }

    @Override
    public void extractRenderState(SoulLinkEntity entity, SoulLinkRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Entity first = entity.level().getEntity(entity.firstId());
        Entity second = entity.level().getEntity(entity.secondId());
        state.visible = first instanceof LivingEntity && second instanceof LivingEntity;
        if (!state.visible) return;
        Vec3 origin = entity.position();
        state.start = attachmentPoint(first).subtract(origin);
        state.end = attachmentPoint(second).subtract(origin);
        state.arcHeight = entity.arcHeight();
        state.thickness = Math.max(0.03F, entity.thickness());
        state.color = entity.color();
        state.rainbow = entity.rainbow();
        state.age = entity.linkAge() + partialTick;
    }

    @Override
    public void submit(SoulLinkRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.visible) {
            RenderType type = RenderTypes.entityTranslucentEmissive(CHAIN_TEXTURE);
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) ->
                    renderTube(state, pose, consumer, state.thickness * GLOW_RADIUS_MULTIPLIER, GLOW_ALPHA, 3.0F));
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) ->
                    renderTube(state, pose, consumer, state.thickness, CORE_ALPHA, 8.0F));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private static Vec3 attachmentPoint(Entity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.65D, 0.0D);
    }

    /**
     * Draws a real 3D tube around the parabolic curve. The old implementation was a single flat
     * ribbon, so it disappeared at grazing viewing angles. A tube is visible from every side.
     */
    private static void renderTube(SoulLinkRenderState state, PoseStack.Pose pose, VertexConsumer consumer, float radius, float alphaScale, float textureRepeats) {
        float scroll = state.age * 0.085F;
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            Vec3 p0 = point(state, t0);
            Vec3 p1 = point(state, t1);
            Vec3 tangent = p1.subtract(p0);
            if (tangent.lengthSqr() < 1.0E-7D) {
                tangent = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                tangent = tangent.normalize();
            }

            Vec3 reference = Math.abs(tangent.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 normal = tangent.cross(reference);
            if (normal.lengthSqr() < 1.0E-7D) {
                normal = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                normal = normal.normalize();
            }

            Vec3 binormal = tangent.cross(normal).normalize();
            int c0 = withAlpha(colorAt(state, t0), alphaScale);
            int c1 = withAlpha(colorAt(state, t1), alphaScale);
            float v0 = t0 * textureRepeats - scroll;
            float v1 = t1 * textureRepeats - scroll;
            for (int side = 0; side < TUBE_SIDES; side++) {
                float a0 = (float) (side * Math.PI * 2.0D / TUBE_SIDES + state.age * 0.045D);
                float a1 = (float) ((side + 1) * Math.PI * 2.0D / TUBE_SIDES + state.age * 0.045D);
                Vec3 o00 = offset(normal, binormal, a0, radius);
                Vec3 o01 = offset(normal, binormal, a1, radius);
                float u0 = side / (float) TUBE_SIDES;
                float u1 = (side + 1) / (float) TUBE_SIDES;
                vertex(consumer, pose, p0.add(o00), c0, u0, v0, o00);
                vertex(consumer, pose, p0.add(o01), c0, u1, v0, o01);
                vertex(consumer, pose, p1.add(o01), c1, u1, v1, o01);
                vertex(consumer, pose, p1.add(o00), c1, u0, v1, o00);
            }
        }
    }

    private static Vec3 offset(Vec3 normal, Vec3 binormal, float angle, float radius) {
        return normal.scale(Math.cos(angle) * radius).add(binormal.scale(Math.sin(angle) * radius));
    }

    private static Vec3 point(SoulLinkRenderState state, float t) {
        Vec3 base = state.start.lerp(state.end, t);
        double arc = Math.sin(t * Math.PI) * state.arcHeight;
        double wave = Math.sin(t * Math.PI * 8.0D + state.age * 0.25D) * state.thickness * 0.75D;
        return base.add(0.0D, arc + wave, 0.0D);
    }

    private static int colorAt(SoulLinkRenderState state, float t) {
        if (!state.rainbow) return state.color;
        float hue = (state.age * 0.012F + t) % 1.0F;
        int rgb = Mth.hsvToRgb(hue, 0.95F, 1.0F);
        return 0xFF000000 | rgb;
    }

    private static int withAlpha(int argb, float alphaScale) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Mth.clamp(Math.round(a * alphaScale), 0, 255);
        return (argb & 0x00FFFFFF) | (scaled << 24);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int argb, float u, float v, Vec3 normal) {
        Vec3 n = normal.lengthSqr() < 1.0E-7D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(argb).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

}