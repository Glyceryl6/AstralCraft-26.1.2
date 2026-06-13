package com.astral_craft.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

class EffectRenderGeometry {

    static void tube(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, float radius, int argb, int sides, float vScroll, float vScale) {
        Vec3 tangent = end.subtract(start);
        if (tangent.lengthSqr() < 1.0E-7D) {
            tangent = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            tangent = tangent.normalize();
        }

        Vec3 reference = Math.abs(tangent.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 normal = tangent.cross(reference);
        if (normal.lengthSqr() < 1.0E-7D) normal = new Vec3(1.0D, 0.0D, 0.0D);
        normal = normal.normalize();
        Vec3 binormal = tangent.cross(normal).normalize();
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / sides);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / sides);
            Vec3 o0 = normal.scale(Math.cos(a0) * radius).add(binormal.scale(Math.sin(a0) * radius));
            Vec3 o1 = normal.scale(Math.cos(a1) * radius).add(binormal.scale(Math.sin(a1) * radius));
            float u0 = i / (float) sides;
            float u1 = (i + 1) / (float) sides;
            vertex(consumer, pose, start.add(o0), argb, u0, vScroll, o0);
            vertex(consumer, pose, start.add(o1), argb, u1, vScroll, o1);
            vertex(consumer, pose, end.add(o1), argb, u1, vScale + vScroll, o1);
            vertex(consumer, pose, end.add(o0), argb, u0, vScale + vScroll, o0);
        }
    }

    static void cube(PoseStack.Pose pose, VertexConsumer consumer, float h, int argb) {
        quad(consumer, pose, -h, -h, h, h, -h, h, h, h, h, -h, h, h, argb, 0, 0, 1);
        quad(consumer, pose, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, argb, 0, 0, -1);
        quad(consumer, pose, h, -h, h, h, -h, -h, h, h, -h, h, h, h, argb, 1, 0, 0);
        quad(consumer, pose, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, argb, -1, 0, 0);
        quad(consumer, pose, -h, h, h, h, h, h, h, h, -h, -h, h, -h, argb, 0, 1, 0);
        quad(consumer, pose, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, argb, 0, -1, 0);
    }

    static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                     float x0, float y0, float z0, float x1, float y1, float z1,
                     float x2, float y2, float z2, float x3, float y3, float z3,
                     int argb, float nx, float ny, float nz) {
        vertex(consumer, pose, new Vec3(x0, y0, z0), argb, 0.0F, 0.0F, new Vec3(nx, ny, nz));
        vertex(consumer, pose, new Vec3(x1, y1, z1), argb, 1.0F, 0.0F, new Vec3(nx, ny, nz));
        vertex(consumer, pose, new Vec3(x2, y2, z2), argb, 1.0F, 1.0F, new Vec3(nx, ny, nz));
        vertex(consumer, pose, new Vec3(x3, y3, z3), argb, 0.0F, 1.0F, new Vec3(nx, ny, nz));
    }

    static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int argb, float u, float v, Vec3 normal) {
        Vec3 n = normal.lengthSqr() < 1.0E-7D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(argb).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

}