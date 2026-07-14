package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
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

/** Compact nearest-board HUD that coexists with scoreboards and other overlays. */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    private static final int HUD_SIZE = 118;
    private static final int HUD_MARGIN_X = 8;
    private static final int HUD_MARGIN_Y = 34;
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
            int px = plotX + Mth.clamp(Math.round((node.x() - bounds.minX())
                    / Math.max(1.0F, bounds.width()) * plotSize), 0, plotSize);
            int pz = plotY + Mth.clamp(Math.round((node.z() - bounds.minZ())
                    / Math.max(1.0F, bounds.depth()) * plotSize), 0, plotSize);
            graphics.fill(px - 2, pz - 2, px + 3, pz + 3, colorFor(node.panelType()));
        }
    }

    private static int colorFor(String type) {
        if (type.contains("start")) return 0xFF72FF72;
        if (type.contains("shop")) return 0xFFFFD15C;
        if (type.contains("teleport")) return 0xFF58C8FF;
        if (type.contains("damage") || type.contains("monster")) return 0xFFFF6464;
        if (type.contains("heal") || type.contains("recover")) return 0xFF80FFA8;
        return 0xFFC6B7FF;
    }

    private enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private record TrackedSnapshot(Snapshot snapshot, double receivedAtTick) {}

    private record Snapshot(String boardId, int centerX, int centerY, int centerZ,
                            List<Node> nodes, boolean enabled) {

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
                            Integer.parseInt(fields[2]), fields[3]));
                } catch (NumberFormatException ignored) {}
            }

            try {
                return Optional.of(new Snapshot(parts[0], Integer.parseInt(center[0]), Integer.parseInt(center[1]),
                        Integer.parseInt(center[2]), List.copyOf(nodes), Boolean.parseBoolean(parts[4])));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

    }

    private record Node(int x, int y, int z, String panelType) {}

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