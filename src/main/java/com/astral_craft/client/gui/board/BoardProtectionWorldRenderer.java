package com.astral_craft.client.gui.board;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.client.util.ClientAnimationClock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Solid violet ground outline used to identify protected board areas without resembling movement routes. */
public class BoardProtectionWorldRenderer {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int OUTLINE_COLOR = 0x8CCE75FF;
    private static final int CORNER_COLOR = 0xD8F1C1FF;
    private static final double MAX_RENDER_DISTANCE_SQR = 160.0D * 160.0D;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final double HALF_WIDTH = 0.035D;
    private static final double CORNER_LENGTH = 1.35D;
    private static final Map<String, ProtectionSnapshot> SNAPSHOTS = new LinkedHashMap<>();

    public static void acceptSnapshot(String encoded) {
        ProtectionSnapshot.parse(encoded).ifPresent(snapshot -> {
            if (snapshot.enabled()) SNAPSHOTS.put(snapshot.boardId(), snapshot);
            else SNAPSHOTS.remove(snapshot.boardId());
        });
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(snapshot ->
                ClientAnimationClock.elapsedTicks(snapshot.receivedAtTick()) > STALE_AFTER_TICKS);
        if (SNAPSHOTS.isEmpty()) return;

        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        for (ProtectionSnapshot current : SNAPSHOTS.values()) {
            double y = current.minY() + 3.035D;
            double minX = current.minX() - 0.18D;
            double maxX = current.maxX() + 1.18D;
            double minZ = current.minZ() - 0.18D;
            double maxZ = current.maxZ() + 1.18D;
            Vec3 center = new Vec3((minX + maxX) * 0.5D, y, (minZ + maxZ) * 0.5D);
            if (center.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) continue;

            submitSolidLine(event, poseStack, cameraPos, new Vec3(minX, y, minZ),
                    new Vec3(maxX, y, minZ), OUTLINE_COLOR, HALF_WIDTH);
            submitSolidLine(event, poseStack, cameraPos, new Vec3(maxX, y, minZ),
                    new Vec3(maxX, y, maxZ), OUTLINE_COLOR, HALF_WIDTH);
            submitSolidLine(event, poseStack, cameraPos, new Vec3(maxX, y, maxZ),
                    new Vec3(minX, y, maxZ), OUTLINE_COLOR, HALF_WIDTH);
            submitSolidLine(event, poseStack, cameraPos, new Vec3(minX, y, maxZ),
                    new Vec3(minX, y, minZ), OUTLINE_COLOR, HALF_WIDTH);

            submitCorner(event, poseStack, cameraPos, minX, y, minZ, 1.0D, 1.0D);
            submitCorner(event, poseStack, cameraPos, maxX, y, minZ, -1.0D, 1.0D);
            submitCorner(event, poseStack, cameraPos, maxX, y, maxZ, -1.0D, -1.0D);
            submitCorner(event, poseStack, cameraPos, minX, y, maxZ, 1.0D, -1.0D);
        }
    }

    private static void submitCorner(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 cameraPos,
                                     double x, double y, double z, double xDirection, double zDirection) {
        submitSolidLine(event, poseStack, cameraPos, new Vec3(x, y + 0.006D, z),
                new Vec3(x + xDirection * CORNER_LENGTH, y + 0.006D, z), CORNER_COLOR, HALF_WIDTH * 1.8D);
        submitSolidLine(event, poseStack, cameraPos, new Vec3(x, y + 0.006D, z),
                new Vec3(x, y + 0.006D, z + zDirection * CORNER_LENGTH), CORNER_COLOR, HALF_WIDTH * 1.8D);
    }

    private static void submitSolidLine(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 cameraPos,
                                        Vec3 start, Vec3 end, int color, double halfWidth) {
        submitSegment(event, poseStack, cameraPos, start, end, color, halfWidth);
    }

    private static void submitSegment(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 cameraPos,
                                      Vec3 start, Vec3 end, int color, double halfWidth) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) return;
        direction = direction.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).scale(halfWidth);
        Vec3 a = start.subtract(side).subtract(cameraPos);
        Vec3 b = end.subtract(side).subtract(cameraPos);
        Vec3 c = end.add(side).subtract(cameraPos);
        Vec3 d = start.add(side).subtract(cameraPos);
        event.getSubmitNodeCollector().order(2).submitCustomGeometry(
                poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, consumer) -> {
                    Vec3 normal = new Vec3(0.0D, 1.0D, 0.0D);
                    EffectRenderGeometry.vertex(consumer, pose, a, color, 0.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, 1.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, 1.0F, 1.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, 0.0F, 1.0F, normal);
                });
    }

    private record ProtectionSnapshot(String boardId, int minX, int minY, int minZ,
                                      int maxX, int maxY, int maxZ, boolean enabled,
                                      double receivedAtTick) {

        private static Optional<ProtectionSnapshot> parse(String encoded) {
            if (encoded == null || encoded.isBlank()) return Optional.empty();
            String[] parts = encoded.split("\\|", -1);
            if (parts.length < 6 || parts[0].isBlank()) return Optional.empty();
            String[] area = parts[3].split(",", 6);
            if (area.length != 6) return Optional.empty();
            try {
                return Optional.of(new ProtectionSnapshot(parts[0],
                        Integer.parseInt(area[0]), Integer.parseInt(area[1]), Integer.parseInt(area[2]),
                        Integer.parseInt(area[3]), Integer.parseInt(area[4]), Integer.parseInt(area[5]),
                        Boolean.parseBoolean(parts[4]), ClientAnimationClock.nowTicks()));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
    }
}
