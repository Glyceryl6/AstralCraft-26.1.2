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

/** Lightweight world-border-like shells around all recently synchronized protected boards. */
public class BoardProtectionWorldRenderer {

    private static final Identifier FORCEFIELD = Identifier.withDefaultNamespace("textures/misc/forcefield.png");
    private static final int COLOR = 0x5A63DFFF;
    private static final double MAX_RENDER_DISTANCE_SQR = 160.0D * 160.0D;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final Map<String, ProtectionSnapshot> SNAPSHOTS = new LinkedHashMap<>();

    public static void acceptSnapshot(String encoded) {
        ProtectionSnapshot.parse(encoded).ifPresent(snapshot -> {
            if (snapshot.enabled()) {
                SNAPSHOTS.put(snapshot.boardId(), snapshot);
            } else {
                SNAPSHOTS.remove(snapshot.boardId());
            }
        });
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(snapshot ->
                ClientAnimationClock.elapsedTicks(snapshot.receivedAtTick()) > STALE_AFTER_TICKS);
        if (SNAPSHOTS.isEmpty()) return;

        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        float scroll = ClientAnimationClock.phaseTicks(80) / 80.0F;
        for (ProtectionSnapshot current : SNAPSHOTS.values()) {
            Vec3 center = new Vec3((current.minX() + current.maxX() + 1.0D) * 0.5D,
                    (current.minY() + current.maxY() + 1.0D) * 0.5D,
                    (current.minZ() + current.maxZ() + 1.0D) * 0.5D);
            if (center.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) continue;

            double minX = current.minX() - 0.02D;
            double maxX = current.maxX() + 1.02D;
            double minZ = current.minZ() - 0.02D;
            double maxZ = current.maxZ() + 1.02D;
            double minY = current.minY();
            double maxY = current.maxY() + 1.0D;
            submitWall(event, event.getPoseStack(), cameraPos,
                    new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ),
                    new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ), scroll);
            submitWall(event, event.getPoseStack(), cameraPos,
                    new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ),
                    new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), scroll);
            submitWall(event, event.getPoseStack(), cameraPos,
                    new Vec3(minX, minY, maxZ), new Vec3(minX, minY, minZ),
                    new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), scroll);
            submitWall(event, event.getPoseStack(), cameraPos,
                    new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ),
                    new Vec3(maxX, maxY, maxZ), new Vec3(maxX, maxY, minZ), scroll);
        }
    }

    private static void submitWall(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 cameraPos,
                                   Vec3 a, Vec3 b, Vec3 c, Vec3 d, float scroll) {
        Vec3 ar = a.subtract(cameraPos);
        Vec3 br = b.subtract(cameraPos);
        Vec3 cr = c.subtract(cameraPos);
        Vec3 dr = d.subtract(cameraPos);
        Vec3 normal = b.subtract(a).cross(d.subtract(a));
        event.getSubmitNodeCollector().order(2).submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(FORCEFIELD),
                (pose, consumer) -> {
                    EffectRenderGeometry.vertex(consumer, pose, ar, COLOR, 0.0F, scroll, normal);
                    EffectRenderGeometry.vertex(consumer, pose, br, COLOR, 4.0F, scroll, normal);
                    EffectRenderGeometry.vertex(consumer, pose, cr, COLOR, 4.0F, 4.0F + scroll, normal);
                    EffectRenderGeometry.vertex(consumer, pose, dr, COLOR, 0.0F, 4.0F + scroll, normal);
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
