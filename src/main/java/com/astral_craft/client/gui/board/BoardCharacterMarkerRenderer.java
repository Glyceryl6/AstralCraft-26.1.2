package com.astral_craft.client.gui.board;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Renders the directional shadow/arrow beneath board-controlled character entities. */
public class BoardCharacterMarkerRenderer {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int SHADOW_COLOR = 0x6A131326;
    private static final int ARROW_COLOR = 0xDD74E9FF;

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        AABB range = new AABB(cameraPos, cameraPos).inflate(128.0D);
        float pulse = 0.92F + (float) Math.sin(ClientAnimationClock.phaseTicks(30) / 30.0F * Math.PI * 2.0D) * 0.08F;
        for (AstralCharacterEntity entity : minecraft.level.getEntitiesOfClass(AstralCharacterEntity.class, range,
                candidate -> candidate.boardSessionUuid().isPresent())) {
            submitMarker(event, event.getPoseStack(), cameraPos, entity, pulse);
        }
    }

    private static void submitMarker(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 cameraPos,
                                     AstralCharacterEntity entity, float pulse) {
        Vec3 center = new Vec3(entity.getX(), entity.getY() + 0.025D, entity.getZ()).subtract(cameraPos);
        double shadow = 0.47D;
        submitQuad(event, poseStack,
                center.add(0.0D, 0.0D, -shadow), center.add(shadow, 0.0D, 0.0D),
                center.add(0.0D, 0.0D, shadow), center.add(-shadow, 0.0D, 0.0D), SHADOW_COLOR);

        Direction direction = entity.boardDirection();
        Vec3 forward = new Vec3(direction.getStepX(), 0.0D, direction.getStepZ());
        if (forward.lengthSqr() < 0.5D) forward = new Vec3(0.0D, 0.0D, -1.0D);
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double length = 0.44D * pulse;
        double halfWidth = 0.14D * pulse;
        Vec3 base = center.subtract(forward.scale(0.14D));
        Vec3 tip = base.add(forward.scale(length));
        Vec3 left = base.subtract(side.scale(halfWidth));
        Vec3 right = base.add(side.scale(halfWidth));
        Vec3 notch = base.add(forward.scale(length * 0.36D));
        submitQuad(event, poseStack, left, tip, right, notch, ARROW_COLOR);
    }

    private static void submitQuad(SubmitCustomGeometryEvent event, PoseStack poseStack,
                                   Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        event.getSubmitNodeCollector().order(1).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(TEXTURE),
                (pose, consumer) -> {
                    Vec3 normal = new Vec3(0.0D, 1.0D, 0.0D);
                    EffectRenderGeometry.vertex(consumer, pose, a, color, 0.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, 1.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, 1.0F, 1.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, 0.0F, 1.0F, normal);
                });
    }
}
