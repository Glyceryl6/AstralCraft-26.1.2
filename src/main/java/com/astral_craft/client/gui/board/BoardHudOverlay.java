package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.BoardHudSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Compact nearest-board HUD with live character portraits grouped by logical panel. */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    private static final int HUD_SIZE = 118;
    private static final int HUD_MARGIN_X = 8;
    private static final int HUD_MARGIN_Y = 34;
    private static final int PORTRAIT_SIZE = 12;
    private static final int NORMAL_NODE_COLOR = 0xFFC6B7FF;
    private static final int START_NODE_COLOR = 0xFF72FF72;
    private static final HudAnchor HUD_ANCHOR = HudAnchor.TOP_RIGHT;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final Map<String, TrackedSnapshot> SNAPSHOTS = new LinkedHashMap<>();

    public static void acceptSnapshot(BoardHudSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BoardProtectionWorldRenderer.acceptSnapshot(payload.encoded());
            Snapshot.parse(payload.encoded()).ifPresent(snapshot -> {
                if (snapshot.enabled()) {
                    SNAPSHOTS.put(snapshot.boardId(), new TrackedSnapshot(snapshot, ClientAnimationClock.nowTicks()));
                } else {
                    SNAPSHOTS.remove(snapshot.boardId());
                }
            });
        });
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(snapshot ->
                ClientAnimationClock.elapsedTicks(snapshot.receivedAtTick()) > STALE_AFTER_TICKS);
        Snapshot snapshot = SNAPSHOTS.values().stream()
                .map(TrackedSnapshot::snapshot)
                .filter(value -> !value.nodes().isEmpty())
                .min(Comparator.comparingDouble(value -> value.distanceToSqr(
                        minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())))
                .orElse(null);
        if (snapshot == null) return;

        int x = switch (HUD_ANCHOR) {
            case TOP_LEFT, BOTTOM_LEFT -> HUD_MARGIN_X;
            case TOP_RIGHT, BOTTOM_RIGHT -> graphics.guiWidth() - HUD_MARGIN_X - HUD_SIZE;
        };
        int y = switch (HUD_ANCHOR) {
            case TOP_LEFT, TOP_RIGHT -> HUD_MARGIN_Y;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - HUD_MARGIN_Y - HUD_SIZE;
        };

        int right = x + HUD_SIZE;
        int bottom = y + HUD_SIZE;
        graphics.fill(x, y, right, bottom, 0x9A080812);
        graphics.fill(x, y, right, y + 1, 0x80FFFFFF);
        graphics.fill(x, bottom - 1, right, bottom, 0x90000000);
        graphics.fill(x, y, x + 1, bottom, 0x60FFFFFF);
        graphics.fill(right - 1, y, right, bottom, 0x70000000);
        graphics.text(minecraft.font, Component.translatable("hud.astral_craft.board"),
                x + 6, y + 5, 0xFFFFFFFF, true);
        int plotX = x + 8;
        int plotY = y + 20;
        int plotSize = HUD_SIZE - 16;
        Bounds bounds = Bounds.of(snapshot.nodes());
        if (bounds == null) return;
        for (Node node : snapshot.nodes()) {
            ScreenPoint point = point(node.x(), node.z(), bounds, plotX, plotY, plotSize);
            int color = node.start() ? START_NODE_COLOR : NORMAL_NODE_COLOR;
            graphics.fill(point.x() - 2, point.y() - 2, point.x() + 3, point.y() + 3, color);
        }

        Map<PanelKey, List<Pawn>> grouped = new LinkedHashMap<>();
        for (Pawn pawn : snapshot.pawns()) {
            grouped.computeIfAbsent(new PanelKey(pawn.x(), pawn.z()), ignored -> new ArrayList<>()).add(pawn);
        }
        for (Map.Entry<PanelKey, List<Pawn>> entry : grouped.entrySet()) {
            ScreenPoint point = point(entry.getKey().x(), entry.getKey().z(), bounds, plotX, plotY, plotSize);
            thisRenderPortraits(graphics, entry.getValue(), point.x(), point.y(), plotX, plotY, plotSize);
        }
    }

    private static void thisRenderPortraits(GuiGraphicsExtractor graphics, List<Pawn> pawns,
                                            int centerX, int centerY, int plotX, int plotY, int plotSize) {
        List<ScreenPoint> offsets = portraitOffsets(pawns.size());
        for (int index = 0; index < pawns.size(); index++) {
            Pawn pawn = pawns.get(index);
            ScreenPoint offset = offsets.get(index);
            int x = Mth.clamp(centerX + offset.x() - PORTRAIT_SIZE / 2,
                    plotX, plotX + plotSize - PORTRAIT_SIZE);
            int y = Mth.clamp(centerY + offset.y() - PORTRAIT_SIZE / 2,
                    plotY, plotY + plotSize - PORTRAIT_SIZE);
            graphics.fill(x - 1, y - 1, x + PORTRAIT_SIZE + 1, y + PORTRAIT_SIZE + 1, 0xE8000000);
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, pawn.characterId(), pawn.skinId(),
                    x, y, PORTRAIT_SIZE, 255);
        }
    }

    private static List<ScreenPoint> portraitOffsets(int count) {
        if (count <= 1) return List.of(new ScreenPoint(0, 0));
        if (count == 2) return List.of(new ScreenPoint(-5, 0), new ScreenPoint(5, 0));
        if (count == 3) return List.of(new ScreenPoint(0, -5), new ScreenPoint(-5, 5), new ScreenPoint(5, 5));
        if (count == 4) return List.of(new ScreenPoint(-5, -5), new ScreenPoint(5, -5),
                new ScreenPoint(-5, 5), new ScreenPoint(5, 5));
        List<ScreenPoint> result = new ArrayList<>();
        int columns = 3;
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            result.add(new ScreenPoint((column - 1) * 8, row * 8 - 4));
        }
        return result;
    }

    private static ScreenPoint point(int nodeX, int nodeZ, Bounds bounds, int plotX, int plotY, int plotSize) {
        int px = plotX + Mth.clamp(Math.round((nodeX - bounds.minX())
                / Math.max(1.0F, bounds.width()) * plotSize), 0, plotSize);
        int pz = plotY + Mth.clamp(Math.round((nodeZ - bounds.minZ())
                / Math.max(1.0F, bounds.depth()) * plotSize), 0, plotSize);
        return new ScreenPoint(px, pz);
    }

    private enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private record TrackedSnapshot(Snapshot snapshot, double receivedAtTick) {}

    private record Snapshot(String boardId, int centerX, int centerY, int centerZ,
                            List<Node> nodes, List<Pawn> pawns, boolean enabled) {

        private double distanceToSqr(double x, double y, double z) {
            double dx = this.centerX + 0.5D - x;
            double dy = this.centerY + 0.5D - y;
            double dz = this.centerZ + 0.5D - z;
            return dx * dx + dy * dy + dz * dz;
        }

        private static Optional<Snapshot> parse(String encoded) {
            if (encoded == null || encoded.isBlank()) return Optional.empty();
            String[] parts = encoded.split("\\|", -1);
            if (parts.length < 6 || parts[0].isBlank()) return Optional.empty();
            String[] center = parts[1].split(",", 3);
            if (center.length != 3) return Optional.empty();
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
            if (parts.length > 6) {
                for (String raw : parts[6].split(";")) {
                    if (raw.isBlank()) continue;
                    String[] fields = raw.split(",", 5);
                    if (fields.length < 5) continue;
                    try {
                        pawns.add(new Pawn(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
                                Integer.parseInt(fields[2]), Identifier.parse(fields[3]), fields[4]));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            try {
                return Optional.of(new Snapshot(parts[0], Integer.parseInt(center[0]), Integer.parseInt(center[1]),
                        Integer.parseInt(center[2]), List.copyOf(nodes), List.copyOf(pawns),
                        Boolean.parseBoolean(parts[4])));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

    }

    private record Node(int x, int y, int z, boolean start) {}

    private record Pawn(int x, int y, int z, Identifier characterId, String skinId) {}

    private record PanelKey(int x, int z) {}

    private record ScreenPoint(int x, int y) {}

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {

        private float width() {
            return Math.max(1, this.maxX - this.minX);
        }

        private float depth() {
            return Math.max(1, this.maxZ - this.minZ);
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