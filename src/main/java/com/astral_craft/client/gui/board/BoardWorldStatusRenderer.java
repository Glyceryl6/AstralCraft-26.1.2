package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.client.util.ClientAnimationClock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** World-space version of the original BoardHudOverlay layout. */
public class BoardWorldStatusRenderer {

    private static final Identifier PANEL_TEXTURE = Identifier.withDefaultNamespace("textures/block/black_concrete.png");
    private static final Identifier SOLID_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int PANEL_COLOR = 0xE312131B;
    private static final int PANEL_INNER_COLOR = 0xD9080812;
    private static final int ROW_COLOR = 0xD811121B;
    private static final int BORDER_COLOR = 0xFF747483;
    private static final int CURRENT_COLOR = 0xFF55FF70;
    private static final int NORMAL_NODE_COLOR = 0xFFC6B7FF;
    private static final int START_NODE_COLOR = 0xFF72FF72;
    private static final double MAX_RENDER_DISTANCE_SQR = 512.0D * 512.0D;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final double HUD_ASPECT = 314.0D / 158.0D;
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
        double boardSpan = Math.max(snapshot.maxX() - snapshot.minX() + 1.0D,
                snapshot.maxZ() - snapshot.minZ() + 1.0D);
        double width = Mth.clamp(boardSpan * 0.88D, 10.0D, 30.0D);
        double height = Mth.clamp(width / HUD_ASPECT, 5.0D, 15.0D);
        double baseY = snapshot.minY() + 0.04D;
        Vec3 anchor = new Vec3((snapshot.minX() + snapshot.maxX() + 1.0D) * 0.5D,
                baseY, snapshot.minZ() - 0.72D);
        Vec3 facing = horizontalDirection(anchor, camera);
        Vec3 right = new Vec3(facing.z, 0.0D, -facing.x).normalize();
        Vec3 center = anchor.add(0.0D, height * 0.5D, 0.0D);
        submitRect(event, camera, PANEL_TEXTURE, center, right, facing, width, height, PANEL_COLOR,
                0.0F, 0.0F, 1.0F, 1.0F, 1);
        submitRect(event, camera, SOLID_TEXTURE,
                local(center, right, facing, 0.0D, height * 0.46D, 0.008D), right, facing,
                width * 0.975D, height * 0.89D, PANEL_INNER_COLOR,
                0.0F, 0.0F, 1.0F, 1.0F, 2);
        double margin = width * 0.025D;
        double headerHeight = height * 0.13D;
        double mapSize = Math.min(height - headerHeight - margin * 2.0D, width * 0.37D);
        double mapLeft = -width * 0.5D + margin;
        double contentTop = height * 0.5D - headerHeight - margin;
        double mapCenterX = mapLeft + mapSize * 0.5D;
        double mapCenterY = contentTop - mapSize * 0.5D;
        double rowsLeft = mapLeft + mapSize + margin * 1.8D;
        double rowsWidth = width * 0.5D - margin - rowsLeft;
        double textScale = Mth.clamp(width / 22.0D, 0.72D, 1.55D);
        Vec3 titleAnchor = local(center, right, facing, 0.0D,
                height * 0.5D - headerHeight * 0.58D, 0.025D);
        submitText(event, camera, titleAnchor, textScale * 1.05D,
                Component.translatable("hud.astral_craft.board_round", snapshot.round())
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
        renderMinimap(event, snapshot, camera, center, right, facing,
                mapCenterX, mapCenterY, mapSize);
        renderParticipantRows(event, snapshot, camera, center, right, facing,
                rowsLeft, contentTop, rowsWidth, mapSize, textScale);
    }

    private static void renderMinimap(SubmitCustomGeometryEvent event, Snapshot snapshot, Vec3 camera,
                                      Vec3 center, Vec3 right, Vec3 facing,
                                      double mapCenterX, double mapCenterY, double mapSize) {
        if (snapshot.nodes().isEmpty()) return;
        Bounds bounds = Bounds.of(snapshot.nodes());
        if (bounds == null) return;
        submitRect(event, camera, SOLID_TEXTURE,
                local(center, right, facing, mapCenterX, mapCenterY, 0.015D), right, facing,
                mapSize, mapSize, 0xB50A0A10, 0.0F, 0.0F, 1.0F, 1.0F, 2);
        double padding = mapSize * 0.08D;
        double usable = mapSize - padding * 2.0D;
        double nodeSize = Mth.clamp(usable / Math.max(12.0D,
                Math.max(bounds.width(), bounds.depth()) + 4.0D) * 0.7D, 0.07D, 0.24D);
        for (Node node : snapshot.nodes()) {
            Vec3 nodeCenter = mapPoint(bounds, node.x(), node.z(), center, right, facing,
                    mapCenterX, mapCenterY, usable);
            submitRect(event, camera, SOLID_TEXTURE, nodeCenter, right, facing,
                    nodeSize, nodeSize, node.start() ? START_NODE_COLOR : NORMAL_NODE_COLOR,
                    0.0F, 0.0F, 1.0F, 1.0F, 3);
        }

        Map<PanelKey, List<Pawn>> grouped = new LinkedHashMap<>();
        for (Pawn pawn : snapshot.pawns()) {
            grouped.computeIfAbsent(new PanelKey(pawn.x(), pawn.z()), ignored -> new ArrayList<>()).add(pawn);
        }

        double portraitSize = Mth.clamp(nodeSize * 2.3D, 0.22D, 0.58D);
        for (Map.Entry<PanelKey, List<Pawn>> entry : grouped.entrySet()) {
            Vec3 point = mapPoint(bounds, entry.getKey().x(), entry.getKey().z(), center, right, facing,
                    mapCenterX, mapCenterY, usable);
            List<Offset> offsets = portraitOffsets(entry.getValue().size(), portraitSize * 0.46D);
            for (int index = 0; index < entry.getValue().size(); index++) {
                Pawn pawn = entry.getValue().get(index);
                Offset offset = offsets.get(index);
                Vec3 portraitCenter = point.add(right.scale(offset.horizontal()))
                        .add(0.0D, offset.vertical(), 0.0D).add(facing.scale(0.02D));
                renderPortraitFrame(event, camera, portraitCenter, right, facing, portraitSize, pawn,
                        pawn.slotId().equals(snapshot.currentSlotId()));
            }
        }
    }

    private static void renderParticipantRows(SubmitCustomGeometryEvent event, Snapshot snapshot, Vec3 camera,
                                              Vec3 center, Vec3 right, Vec3 facing,
                                              double rowsLeft, double contentTop, double rowsWidth,
                                              double contentHeight, double textScale) {
        List<Pawn> pawns = snapshot.pawns();
        if (pawns.isEmpty()) return;
        double rowGap = contentHeight * 0.028D;
        double rowHeight = (contentHeight - rowGap * 3.0D) / 4.0D;
        double portraitSize = rowHeight * 0.72D;
        for (int index = 0; index < Math.min(4, pawns.size()); index++) {
            Pawn pawn = pawns.get(index);
            double rowCenterY = contentTop - rowHeight * 0.5D - index * (rowHeight + rowGap);
            Vec3 rowCenter = local(center, right, facing,
                    rowsLeft + rowsWidth * 0.5D, rowCenterY, 0.016D);
            submitRect(event, camera, SOLID_TEXTURE, rowCenter, right, facing,
                    rowsWidth, rowHeight, ROW_COLOR, 0.0F, 0.0F, 1.0F, 1.0F, 2);

            double portraitX = rowsLeft + portraitSize * 0.62D;
            Vec3 portraitCenter = local(center, right, facing, portraitX, rowCenterY, 0.028D);
            boolean current = pawn.slotId().equals(snapshot.currentSlotId());
            renderPortraitFrame(event, camera, portraitCenter, right, facing, portraitSize, pawn, current);
            double textX = portraitX + portraitSize * 0.72D;
            Component name = Component.literal(pawn.controllerName()).withStyle(
                    pawn.knockedDown() ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE, ChatFormatting.BOLD);
            submitText(event, camera,
                    local(center, right, facing, textX, rowCenterY + rowHeight * 0.19D, 0.04D),
                    textScale * 0.80D, name, false);
            String stars = "★".repeat(Math.clamp(pawn.stars(), 0, 3))
                    + "☆".repeat(Math.clamp(3 - pawn.stars(), 0, 3));
            Component stats = Component.translatable("hud.astral_craft.board_world_stats",
                    pawn.starCoins(), pawn.health(), pawn.maximumHealth(), stars)
                    .withStyle(pawn.knockedDown() ? ChatFormatting.GRAY : ChatFormatting.YELLOW);
            submitText(event, camera,
                    local(center, right, facing, textX, rowCenterY - rowHeight * 0.20D, 0.04D),
                    textScale * 0.67D, stats, false);
        }
    }

    private static void renderPortraitFrame(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 center,
                                            Vec3 right, Vec3 facing, double size, Pawn pawn, boolean current) {
        submitRect(event, camera, SOLID_TEXTURE, center, right, facing,
                size * 1.14D, size * 1.14D, BORDER_COLOR,
                0.0F, 0.0F, 1.0F, 1.0F, 4);
        submitRect(event, camera, SOLID_TEXTURE, center.add(facing.scale(0.003D)), right, facing,
                size * 1.03D, size * 1.03D, 0xFF08080C,
                0.0F, 0.0F, 1.0F, 1.0F, 4);
        Identifier texture = AstralStatusIconRenderer.characterSkinTexture(pawn.characterId(), pawn.skinId());
        int tint = pawn.knockedDown() ? 0xFF777777 : 0xFFFFFFFF;
        submitRect(event, camera, texture, center.add(facing.scale(0.008D)), right, facing,
                size, size, tint, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, 5);
        submitRect(event, camera, texture, center.add(facing.scale(0.011D)), right, facing,
                size, size, tint, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, 5);
        if (current) {
            Vec3 markerCenter = center.subtract(right.scale(size * 0.70D));
            submitRect(event, camera, SOLID_TEXTURE, markerCenter, right, facing,
                    size * 0.12D, size * 0.70D, CURRENT_COLOR,
                    0.0F, 0.0F, 1.0F, 1.0F, 6);
        }
    }

    private static Vec3 mapPoint(Bounds bounds, int x, int z,
                                 Vec3 center, Vec3 right, Vec3 facing,
                                 double mapCenterX, double mapCenterY, double usableSize) {
        double normalizedX = normalize(x, bounds.minX(), bounds.maxX());
        double normalizedZ = normalize(z, bounds.minZ(), bounds.maxZ());
        return local(center, right, facing,
                mapCenterX + (normalizedX - 0.5D) * usableSize,
                mapCenterY + (0.5D - normalizedZ) * usableSize, 0.03D);
    }

    private static Vec3 local(Vec3 center, Vec3 right, Vec3 facing, double horizontal, double vertical, double depth) {
        return center.add(right.scale(horizontal)).add(0.0D, vertical, 0.0D).add(facing.scale(depth));
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        return direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
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

    private static void submitRect(SubmitCustomGeometryEvent event, Vec3 camera, Identifier texture,
                                   Vec3 center, Vec3 right, Vec3 normal,
                                   double width, double height, int color,
                                   float u0, float v0, float u1, float v1, int order) {
        Vec3 horizontal = right.normalize().scale(width * 0.5D);
        Vec3 vertical = new Vec3(0.0D, height * 0.5D, 0.0D);
        Vec3 a = center.subtract(horizontal).subtract(vertical).subtract(camera);
        Vec3 b = center.add(horizontal).subtract(vertical).subtract(camera);
        Vec3 c = center.add(horizontal).add(vertical).subtract(camera);
        Vec3 d = center.subtract(horizontal).add(vertical).subtract(camera);
        Vec3 front = normal.normalize();
        Vec3 back = front.scale(-1.0D);
        event.getSubmitNodeCollector().order(order).submitCustomGeometry(event.getPoseStack(),
                RenderTypes.entityTranslucentEmissive(texture), (pose, consumer) -> {
                    EffectRenderGeometry.vertex(consumer, pose, a, color, u0, v1, front);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, u1, v1, front);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, u1, v0, front);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, u0, v0, front);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, u0, v0, back);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, u1, v0, back);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, u1, v1, back);
                    EffectRenderGeometry.vertex(consumer, pose, a, color, u0, v1, back);
                });
    }

    private static void submitText(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 anchor,
                                   double scale, Component text, boolean background) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
        poseStack.scale((float) scale, (float) scale, (float) scale);
        event.getSubmitNodeCollector().order(7).submitNameTag(poseStack, Vec3.ZERO, 0, text,
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
                        Integer.parseInt(parts[7]), current, List.copyOf(nodes), List.copyOf(pawns),
                        ClientAnimationClock.nowTicks()));
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
    private record Offset(double horizontal, double vertical) {}

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {

        private int width() { return Math.max(1, this.maxX - this.minX); }
        private int depth() { return Math.max(1, this.maxZ - this.minZ); }

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