package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardHudSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD minimap-style projection for scanned board sessions.
 *
 * <p>It is intentionally a compact corner widget rather than a full-screen hologram, so it can
 * coexist with scoreboards and other HUD mods. Tweak anchor/margins below or expose them via a
 * client config later.</p>
 */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    private static final int HUD_SIZE = 118;
    private static final int HUD_MARGIN_X = 8;
    private static final int HUD_MARGIN_Y = 34;
    private static final HudAnchor HUD_ANCHOR = HudAnchor.TOP_RIGHT;
    private static final long STALE_AFTER_MILLIS = 1500L;

    private static Snapshot snapshot = Snapshot.EMPTY;
    private static long lastUpdateMillis;

    public static void acceptSnapshot(BoardHudSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            snapshot = Snapshot.parse(payload.encoded());
            lastUpdateMillis = System.currentTimeMillis();
        });
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || snapshot.nodes().isEmpty()) return;
        if (System.currentTimeMillis() - lastUpdateMillis > STALE_AFTER_MILLIS) return;
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
        graphics.text(mc.font, Component.translatable("hud.astral_craft.board"),
                x + 6, y + 5, 0xFFFFFFFF, true);
        int plotX = x + 8;
        int plotY = y + 20;
        int plotSize = HUD_SIZE - 16;
        Bounds bounds = Bounds.of(snapshot.nodes());
        if (bounds == null) return;
        for (Node node : snapshot.nodes()) {
            int px = plotX + Mth.clamp(Math.round((node.x() - bounds.minX()) / Math.max(1.0F, bounds.width()) * plotSize), 0, plotSize);
            int pz = plotY + Mth.clamp(Math.round((node.z() - bounds.minZ()) / Math.max(1.0F, bounds.depth()) * plotSize), 0, plotSize);
            int color = colorFor(node.panelType());
            graphics.fill(px - 2, pz - 2, px + 3, pz + 3, color);
        }
    }

    private static int colorFor(String type) {
        if (type.contains("start")) return 0xFF72FF72;
        if (type.contains("shop")) return 0xFFFFD15C;
        if (type.contains("teleport")) return 0xFF58C8FF;
        if (type.contains("damage") || type.contains("monster")) return 0xFFFF6464;
        if (type.contains("heal")) return 0xFF80FFA8;
        return 0xFFC6B7FF;
    }

    private enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private record Snapshot(List<Node> nodes) {

        private static final Snapshot EMPTY = new Snapshot(List.of());

        static Snapshot parse(String encoded) {
            if (encoded == null || encoded.isBlank()) return EMPTY;
            String[] parts = encoded.split("\\|", 3);
            if (parts.length < 3) return EMPTY;
            List<Node> nodes = new ArrayList<>();
            for (String raw : parts[2].split(";")) {
                if (raw.isBlank()) continue;
                String[] f = raw.split(",", 5);
                if (f.length < 5) continue;
                try {
                    nodes.add(new Node(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), f[4]));
                } catch (NumberFormatException ignored) {
                }
            }
            return new Snapshot(List.copyOf(nodes));
        }

    }

    private record Node(String id, int x, int y, int z, String panelType) {}

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {

        float width() { return Math.max(1, maxX - minX); }
        float depth() { return Math.max(1, maxZ - minZ); }

        static Bounds of(List<Node> nodes) {
            if (nodes.isEmpty()) return null;
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Node n : nodes) {
                minX = Math.min(minX, n.x());
                maxX = Math.max(maxX, n.x());
                minZ = Math.min(minZ, n.z());
                maxZ = Math.max(maxZ, n.z());
            }

            return new Bounds(minX, maxX, minZ, maxZ);
        }
    }

}