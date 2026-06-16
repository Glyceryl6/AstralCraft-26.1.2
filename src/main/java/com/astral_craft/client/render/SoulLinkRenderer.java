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
    private static final int SEGMENTS = 40;
    private static final int TUBE_SIDES = 12;
    private static final float GLOW_RADIUS_MULTIPLIER = 4.25F;
    private static final float CORE_ALPHA = 1.0F;
    private static final float GLOW_ALPHA = 0.42F;

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
        state.start = this.attachmentPoint(first).subtract(origin);
        state.end = this.attachmentPoint(second).subtract(origin);
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
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> this.renderTube(state, pose, consumer, state.thickness * GLOW_RADIUS_MULTIPLIER, GLOW_ALPHA, 2.0F));
            collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> this.renderTube(state, pose, consumer, state.thickness, CORE_ALPHA, 6.0F));
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    public Vec3 attachmentPoint(Entity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.65D, 0.0D);
    }

    /**
     * Draws a stable 3D tube around the parabolic curve. The basis is computed once from the whole
     * link direction, instead of per segment, which avoids sudden normal flips and most of the
     * shimmer that looked like distortion while players moved around the link.
     */
    public void renderTube(SoulLinkRenderState state, PoseStack.Pose pose, VertexConsumer consumer, float radius, float alphaScale, float textureRepeats) {
        Vec3 direction = state.end.subtract(state.start);
        if (direction.lengthSqr() < 1.0E-7D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            direction = direction.normalize();
        }

        Vec3 up = Math.abs(direction.y) > 0.94D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 normal = direction.cross(up);
        if (normal.lengthSqr() < 1.0E-7D) {
            normal = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }

        Vec3 binormal = direction.cross(normal).normalize();
        float scroll = state.age * 0.055F;
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            Vec3 p0 = this.point(state, t0);
            Vec3 p1 = this.point(state, t1);
            int c0 = this.withAlpha(this.colorAt(state, t0), alphaScale);
            int c1 = this.withAlpha(this.colorAt(state, t1), alphaScale);
            float v0 = t0 * textureRepeats - scroll;
            float v1 = t1 * textureRepeats - scroll;
            for (int side = 0; side < TUBE_SIDES; side++) {
                float a0 = (float) (side * Math.PI * 2.0D / TUBE_SIDES);
                float a1 = (float) ((side + 1) * Math.PI * 2.0D / TUBE_SIDES);
                Vec3 o00 = this.offset(normal, binormal, a0, radius);
                Vec3 o01 = this.offset(normal, binormal, a1, radius);
                Vec3 i00 = this.offset(normal, binormal, a0, radius * 0.78F);
                Vec3 i01 = this.offset(normal, binormal, a1, radius * 0.78F);
                float u0 = side / (float) TUBE_SIDES;
                float u1 = (side + 1) / (float) TUBE_SIDES;
                this.quad(consumer, pose, p0.add(o00), p0.add(o01), p1.add(o01), p1.add(o00), c0, c1, u0, u1, v0, v1, o00, o01);
                this.quad(consumer, pose, p1.add(i00), p1.add(i01), p0.add(i01), p0.add(i00), c1, c0, u0, u1, v1, v0, i00.scale(-1.0D), i01.scale(-1.0D));
            }
        }
    }

    public void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int c0, int c1, float u0, float u1, float v0, float v1, Vec3 n0, Vec3 n1) {
        this.vertex(consumer, pose, a, c0, u0, v0, n0);
        this.vertex(consumer, pose, b, c0, u1, v0, n1);
        this.vertex(consumer, pose, c, c1, u1, v1, n1);
        this.vertex(consumer, pose, d, c1, u0, v1, n0);
    }

    public Vec3 offset(Vec3 normal, Vec3 binormal, float angle, float radius) {
        return normal.scale(Math.cos(angle) * radius).add(binormal.scale(Math.sin(angle) * radius));
    }

    public Vec3 point(SoulLinkRenderState state, float t) {
        Vec3 base = state.start.lerp(state.end, t);
        double arc = Math.sin(t * Math.PI) * state.arcHeight;
        double wave = Math.sin(t * Math.PI * 4.0D + state.age * 0.12D) * state.thickness * 0.20D;
        return base.add(0.0D, arc + wave, 0.0D);
    }

    public int colorAt(SoulLinkRenderState state, float t) {
        if (!state.rainbow) return state.color;
        float hue = (state.age * 0.006F + t) % 1.0F;
        int rgb = Mth.hsvToRgb(hue, 0.95F, 1.0F);
        return 0xFF000000 | rgb;
    }

    public int withAlpha(int argb, float alphaScale) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Mth.clamp(Math.round(a * alphaScale), 0, 255);
        return (argb & 0x00FFFFFF) | (scaled << 24);
    }

    public void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int argb, float u, float v, Vec3 normal) {
        Vec3 n = normal.lengthSqr() < 1.0E-7D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(argb).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

}