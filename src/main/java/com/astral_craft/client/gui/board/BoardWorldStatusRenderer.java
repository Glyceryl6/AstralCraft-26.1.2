package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.client.util.ClientAnimationClock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** A diegetic minimap and scoreboard mounted vertically along a deterministic board edge. */
public class BoardWorldStatusRenderer {

    private static final Identifier PANEL_TEXTURE = Identifier.withDefaultNamespace("textures/block/black_concrete.png");
    private static final Identifier SOLID_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int PANEL_COLOR = 0xD91A1B24;
    private static final int PANEL_BACK_COLOR = 0xC812131B;
    private static final int CURRENT_COLOR = 0xFF58FF75;
    private static final int NORMAL_NODE_COLOR = 0xFFC6B7FF;
    private static final int START_NODE_COLOR = 0xFF72FF72;
    private static final double MAX_RENDER_DISTANCE_SQR = 512.0D * 512.0D;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final Map<String, Snapshot> SNAPSHOTS = new LinkedHashMap<>();

    public static void acceptSnapshot(String encoded) {
        Snapshot.parse(encoded).ifPresent(snapshot -> {
            if (snapshot.enabled()) SNAPSHOTS.put(snapshot.boardId(), snapshot);
            else SNAPSHOTS.remove(snapshot.boardId());
        });
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(value ->
                ClientAnimationClock.elapsedTicks(value.receivedAtTick()) > STALE_AFTER_TICKS);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (Snapshot snapshot : SNAPSHOTS.values()) {
            if (snapshot.center().distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) continue;
            submitSnapshot(event, snapshot, camera);
        }
    }

    private static void submitSnapshot(SubmitCustomGeometryEvent event, Snapshot snapshot, Vec3 camera) {
        double spanX = snapshot.maxX() - snapshot.minX() + 1.0D;
        double spanZ = snapshot.maxZ() - snapshot.minZ() + 1.0D;
        boolean northEdge = spanX >= spanZ;
        double span = Math.max(8.0D, northEdge ? spanX : spanZ);
        double height = Mth.clamp(span * 0.22D, 4.5D, 12.0D);
        double baseY = snapshot.maxY() + 0.18D;
        Vec3 along = northEdge ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 normal = northEdge ? new Vec3(0.0D, 0.0D, 1.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 center = northEdge
                ? new Vec3((snapshot.minX() + snapshot.maxX() + 1.0D) * 0.5D, baseY,
                snapshot.minZ() - 0.16D)
                : new Vec3(snapshot.minX() - 0.16D, baseY,
                (snapshot.minZ() + snapshot.maxZ() + 1.0D) * 0.5D);
        Vec3 half = along.scale(span * 0.5D);
        Vec3 bottomLeft = center.subtract(half);
        Vec3 bottomRight = center.add(half);
        Vec3 topRight = bottomRight.add(0.0D, height, 0.0D);
        Vec3 topLeft = bottomLeft.add(0.0D, height, 0.0D);
        submitPanel(event, camera, bottomLeft, bottomRight, topRight, topLeft, normal);
        double textScale = Mth.clamp(span / 28.0D, 0.85D, 3.8D);
        Vec3 roundAnchor = center.add(normal.scale(0.07D)).add(0.0D, height * 0.92D, 0.0D);
        submitText(event, camera, roundAnchor, textScale * 1.12D,
                Component.translatable("hud.astral_craft.board_round", snapshot.round())
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
        renderWorldMinimap(event, snapshot, camera, center, along, normal, span, height);
        renderParticipantColumns(event, snapshot, camera, center, along, normal, span, height, textScale);
    }

    private static void renderWorldMinimap(SubmitCustomGeometryEvent event, Snapshot snapshot, Vec3 camera,
                                           Vec3 panelCenter, Vec3 along, Vec3 normal,
                                           double span, double height) {
        if (snapshot.nodes().isEmpty()) return;
        Bounds bounds = Bounds.of(snapshot.nodes());
        if (bounds == null) return;
        double mapWidth = span * 0.84D;
        double mapHeight = height * 0.30D;
        double mapCenterY = height * 0.67D;
        int horizontalRange = Math.max(1, bounds.horizontalRange(snapshot.northEdge()));
        int verticalRange = Math.max(1, bounds.verticalRange(snapshot.northEdge()));
        double nodeSize = Mth.clamp(Math.min(mapWidth / (horizontalRange + 2.0D),
                mapHeight / (verticalRange + 2.0D)) * 0.42D, 0.08D, 0.28D);
        for (Node node : snapshot.nodes()) {
            Vec3 nodeCenter = mapPoint(snapshot, bounds, node.x(), node.z(), panelCenter, along, normal,
                    mapWidth, mapHeight, mapCenterY);
            submitSolidQuad(event, camera, nodeCenter, along, normal, nodeSize,
                    node.start() ? START_NODE_COLOR : NORMAL_NODE_COLOR);
        }

        Map<PanelKey, List<Pawn>> grouped = new LinkedHashMap<>();
        for (Pawn pawn : snapshot.pawns()) {
            grouped.computeIfAbsent(new PanelKey(pawn.x(), pawn.z()), ignored -> new ArrayList<>()).add(pawn);
        }

        double portraitSize = Mth.clamp(nodeSize * 2.30D, 0.24D, 0.62D);
        for (Map.Entry<PanelKey, List<Pawn>> entry : grouped.entrySet()) {
            Vec3 point = mapPoint(snapshot, bounds, entry.getKey().x(), entry.getKey().z(), panelCenter,
                    along, normal, mapWidth, mapHeight, mapCenterY);
            List<Offset> offsets = portraitOffsets(entry.getValue().size(), portraitSize * 0.58D);
            for (int index = 0; index < entry.getValue().size(); index++) {
                Pawn pawn = entry.getValue().get(index);
                Offset offset = offsets.get(index);
                Vec3 portraitCenter = point.add(along.scale(offset.along()))
                        .add(0.0D, offset.vertical(), 0.0D).add(normal.scale(0.018D));
                if (pawn.slotId().equals(snapshot.currentSlotId())) {
                    submitPortraitBorder(event, camera, portraitCenter, along, normal, portraitSize * 1.16D, CURRENT_COLOR);
                }
                submitPortrait(event, camera, portraitCenter, along, normal, portraitSize, pawn, 0xFFFFFFFF);
                if (pawn.knockedDown()) {
                    submitSolidQuad(event, camera, portraitCenter.add(normal.scale(0.006D)), along, normal, portraitSize, 0xA89A9AA0);
                }
            }
        }
    }

    private static void renderParticipantColumns(SubmitCustomGeometryEvent event, Snapshot snapshot, Vec3 camera,
                                                 Vec3 panelCenter, Vec3 along, Vec3 normal,
                                                 double span, double height, double textScale) {
        List<Pawn> pawns = snapshot.pawns();
        if (pawns.isEmpty()) return;
        double columnWidth = span / pawns.size();
        double portraitSize = Mth.clamp(Math.min(columnWidth * 0.24D, height * 0.19D), 0.68D, 1.55D);
        for (int index = 0; index < pawns.size(); index++) {
            Pawn pawn = pawns.get(index);
            double offset = -span * 0.5D + columnWidth * (index + 0.5D);
            Vec3 columnCenter = panelCenter.add(along.scale(offset)).add(normal.scale(0.07D));
            boolean current = pawn.slotId().equals(snapshot.currentSlotId());
            Vec3 portraitCenter = columnCenter.add(0.0D, height * 0.35D, 0.0D);
            if (current) {
                submitPortraitBorder(event, camera, portraitCenter, along, normal, portraitSize * 1.14D, CURRENT_COLOR);
            }

            submitPortrait(event, camera, portraitCenter, along, normal, portraitSize, pawn, 0xFFFFFFFF);
            if (pawn.knockedDown()) {
                submitSolidQuad(event, camera, portraitCenter.add(normal.scale(0.006D)), along, normal, portraitSize, 0xA89A9AA0);
            }

            MutableComponent name = Component.literal((current ? "▶ " : "") + pawn.controllerName())
                    .withStyle(current ? ChatFormatting.GREEN : pawn.knockedDown() ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE);
            submitText(event, camera, columnCenter.add(0.0D, height * 0.20D, 0.0D),
                    textScale, name, true);
            String stars = "★".repeat(Math.clamp(pawn.stars(), 0, 3))
                    + "☆".repeat(Math.clamp(3 - pawn.stars(), 0, 3));
            Component stats = Component.translatable("hud.astral_craft.board_world_stats",
                    pawn.starCoins(), pawn.health(), pawn.maximumHealth(), stars)
                    .withStyle(pawn.knockedDown() ? ChatFormatting.GRAY : ChatFormatting.YELLOW);
            submitText(event, camera, columnCenter.add(0.0D, height * 0.09D, 0.0D),
                    textScale * 0.84D, stats, true);
        }
    }

    private static Vec3 mapPoint(Snapshot snapshot, Bounds bounds, int x, int z,
                                 Vec3 panelCenter, Vec3 along, Vec3 normal,
                                 double mapWidth, double mapHeight, double mapCenterY) {
        double horizontal = snapshot.northEdge()
                ? normalize(x, bounds.minX(), bounds.maxX())
                : normalize(z, bounds.minZ(), bounds.maxZ());
        double vertical = snapshot.northEdge()
                ? normalize(z, bounds.minZ(), bounds.maxZ())
                : normalize(x, bounds.minX(), bounds.maxX());
        return panelCenter.add(along.scale((horizontal - 0.5D) * mapWidth))
                .add(0.0D, mapCenterY + (0.5D - vertical) * mapHeight, 0.0D)
                .add(normal.scale(0.075D));
    }

    private static double normalize(int value, int minimum, int maximum) {
        return (value - minimum) / Math.max(1.0D, maximum - minimum);
    }

    private static List<Offset> portraitOffsets(int count, double distance) {
        if (count <= 1) return List.of(new Offset(0.0D, 0.0D));
        if (count == 2) return List.of(new Offset(-distance, 0.0D), new Offset(distance, 0.0D));
        if (count == 3) return List.of(new Offset(0.0D, distance),
                new Offset(-distance, -distance), new Offset(distance, -distance));
        if (count == 4) return List.of(new Offset(-distance, distance), new Offset(distance, distance),
                new Offset(-distance, -distance), new Offset(distance, -distance));
        List<Offset> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int column = index % 3;
            int row = index / 3;
            result.add(new Offset((column - 1) * distance, (0.5D - row) * distance));
        }
        return List.copyOf(result);
    }

    private static void submitPanel(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal) {
        Vec3 ar = a.subtract(camera);
        Vec3 br = b.subtract(camera);
        Vec3 cr = c.subtract(camera);
        Vec3 dr = d.subtract(camera);
        event.getSubmitNodeCollector().order(1).submitCustomGeometry(event.getPoseStack(),
                RenderTypes.entityTranslucentEmissive(PANEL_TEXTURE), (pose, consumer) -> {
                    EffectRenderGeometry.vertex(consumer, pose, ar, PANEL_COLOR, 0.0F, 1.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, br, PANEL_COLOR, 1.0F, 1.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, cr, PANEL_COLOR, 1.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, dr, PANEL_COLOR, 0.0F, 0.0F, normal);
                    Vec3 reverse = normal.scale(-1.0D);
                    EffectRenderGeometry.vertex(consumer, pose, dr, PANEL_BACK_COLOR, 0.0F, 0.0F, reverse);
                    EffectRenderGeometry.vertex(consumer, pose, cr, PANEL_BACK_COLOR, 1.0F, 0.0F, reverse);
                    EffectRenderGeometry.vertex(consumer, pose, br, PANEL_BACK_COLOR, 1.0F, 1.0F, reverse);
                    EffectRenderGeometry.vertex(consumer, pose, ar, PANEL_BACK_COLOR, 0.0F, 1.0F, reverse);
                });
    }

    private static void submitSolidQuad(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 center,
                                        Vec3 along, Vec3 normal, double size, int color) {
        submitQuad(event, camera, SOLID_TEXTURE, center, along, normal, size, color,
                0.0F, 0.0F, 1.0F, 1.0F);
    }

    private static void submitPortraitBorder(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 center,
                                             Vec3 along, Vec3 normal, double size, int color) {
        submitQuad(event, camera, SOLID_TEXTURE, center, along, normal, size, color,
                0.0F, 0.0F, 1.0F, 1.0F);
    }

    private static void submitPortrait(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 center,
                                       Vec3 along, Vec3 normal, double size, Pawn pawn, int color) {
        Identifier texture = AstralStatusIconRenderer.characterSkinTexture(pawn.characterId(), pawn.skinId());
        submitQuad(event, camera, texture, center, along, normal, size, color,
                8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F);
        submitQuad(event, camera, texture, center.add(normal.scale(0.003D)), along, normal, size, color,
                40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F);
    }

    private static void submitQuad(SubmitCustomGeometryEvent event, Vec3 camera, Identifier texture,
                                   Vec3 center, Vec3 along, Vec3 preferredNormal, double size, int color,
                                   float u0, float v0, float u1, float v1) {
        Vec3 horizontal = along.normalize().scale(size * 0.5D);
        Vec3 vertical = new Vec3(0.0D, size * 0.5D, 0.0D);
        Vec3 a = center.subtract(horizontal).subtract(vertical).subtract(camera);
        Vec3 b = center.add(horizontal).subtract(vertical).subtract(camera);
        Vec3 c = center.add(horizontal).add(vertical).subtract(camera);
        Vec3 d = center.subtract(horizontal).add(vertical).subtract(camera);
        Vec3 normal = preferredNormal.normalize();
        Vec3 reverse = normal.scale(-1.0D);
        event.getSubmitNodeCollector().order(2).submitCustomGeometry(event.getPoseStack(),
                RenderTypes.entityTranslucentEmissive(texture), (pose, consumer) -> {
                    EffectRenderGeometry.vertex(consumer, pose, a, color, u0, v1, normal);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, u1, v1, normal);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, u1, v0, normal);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, u0, v0, normal);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, u0, v0, reverse);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, u1, v0, reverse);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, u1, v1, reverse);
                    EffectRenderGeometry.vertex(consumer, pose, a, color, u0, v1, reverse);
                });
    }

    private static void submitText(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 anchor,
                                   double scale, Component text, boolean background) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
        poseStack.scale((float) scale, (float) scale, (float) scale);
        event.getSubmitNodeCollector().order(3).submitNameTag(poseStack, Vec3.ZERO, 0, text,
                background, LightCoordsUtil.FULL_BRIGHT, anchor.distanceToSqr(camera),
                event.getLevelRenderState().cameraRenderState);
        poseStack.popPose();
    }

    private record Snapshot(String boardId, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, boolean enabled,
                            int round, UUID currentSlotId, List<Node> nodes, List<Pawn> pawns,
                            double receivedAtTick) {
        private Vec3 center() {
            return new Vec3((this.minX + this.maxX + 1.0D) * 0.5D,
                    (this.minY + this.maxY + 1.0D) * 0.5D,
                    (this.minZ + this.maxZ + 1.0D) * 0.5D);
        }

        private boolean northEdge() {
            return this.maxX - this.minX >= this.maxZ - this.minZ;
        }

        private static Optional<Snapshot> parse(String encoded) {
            if (encoded == null || encoded.isBlank()) return Optional.empty();
            String[] parts = encoded.split("\\|", -1);
            if (parts.length < 9 || parts[0].isBlank()) return Optional.empty();
            String[] area = parts[3].split(",", 6);
            if (area.length != 6) return Optional.empty();
            List<Node> nodes = new ArrayList<>();
            for (String raw : parts[2].split(";")) {
                if (raw.isBlank()) continue;
                String[] fields = raw.split(",", 4);
                if (fields.length < 4) continue;
                try {
                    nodes.add(new Node(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
                            Integer.parseInt(fields[2]), "1".equals(fields[3])));
                } catch (NumberFormatException ignored) {}
            }
            List<Pawn> pawns = new ArrayList<>();
            for (String raw : parts[6].split(";")) {
                if (raw.isBlank()) continue;
                String[] fields = raw.split(",", 12);
                if (fields.length < 12) continue;
                try {
                    pawns.add(new Pawn(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
                            Integer.parseInt(fields[2]), Identifier.parse(fields[3]), fields[4],
                            UUID.fromString(fields[5]), decodeName(fields[6]), Integer.parseInt(fields[7]),
                            Integer.parseInt(fields[8]), Integer.parseInt(fields[9]), Integer.parseInt(fields[10]),
                            "1".equals(fields[11])));
                } catch (IllegalArgumentException ignored) {}
            }
            try {
                UUID current = parts[8].isBlank() ? new UUID(0L, 0L) : UUID.fromString(parts[8]);
                return Optional.of(new Snapshot(parts[0], Integer.parseInt(area[0]), Integer.parseInt(area[1]),
                        Integer.parseInt(area[2]), Integer.parseInt(area[3]), Integer.parseInt(area[4]),
                        Integer.parseInt(area[5]), Boolean.parseBoolean(parts[4]) && "PLAYING".equals(parts[5]),
                        Integer.parseInt(parts[7]),
                        current, List.copyOf(nodes), List.copyOf(pawns), ClientAnimationClock.nowTicks()));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        private static String decodeName(String value) {
            try {
                return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                return value;
            }
        }
    }

    private record Node(int x, int y, int z, boolean start) {}
    private record Pawn(int x, int y, int z, Identifier characterId, String skinId,
                        UUID slotId, String controllerName, int starCoins, int health,
                        int maximumHealth, int stars, boolean knockedDown) {}
    private record PanelKey(int x, int z) {}
    private record Offset(double along, double vertical) {}

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {

        private int horizontalRange(boolean northEdge) {
            return northEdge ? this.maxX - this.minX : this.maxZ - this.minZ;
        }

        private int verticalRange(boolean northEdge) {
            return northEdge ? this.maxZ - this.minZ : this.maxX - this.minX;
        }

        private static Bounds of(List<Node> nodes) {
            if (nodes.isEmpty()) return null;
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Node node : nodes) {
                minX = Math.min(minX, node.x());
                maxX = Math.max(maxX, node.x());
                minZ = Math.min(minZ, node.z());
                maxZ = Math.max(maxZ, node.z());
            }
            return new Bounds(minX, maxX, minZ, maxZ);
        }

    }

}