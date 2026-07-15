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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Nearest-board minimap plus live participant status rows. */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    private static final int HUD_WIDTH = 314;
    private static final int HUD_HEIGHT = 158;
    private static final int HUD_MARGIN_X = 8;
    private static final int HUD_MARGIN_Y = 34;
    private static final int MAP_SIZE = 112;
    private static final int MAP_PORTRAIT_SIZE = 12;
    private static final int ROW_PORTRAIT_SIZE = 24;
    private static final int NORMAL_NODE_COLOR = 0xFFC6B7FF;
    private static final int START_NODE_COLOR = 0xFF72FF72;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final Map<String, TrackedSnapshot> SNAPSHOTS = new LinkedHashMap<>();

    public static void acceptSnapshot(BoardHudSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BoardProtectionWorldRenderer.acceptSnapshot(payload.encoded());
            Snapshot.parse(payload.encoded()).ifPresent(snapshot -> {
                if (snapshot.enabled()) SNAPSHOTS.put(snapshot.boardId(),
                        new TrackedSnapshot(snapshot, ClientAnimationClock.nowTicks()));
                else SNAPSHOTS.remove(snapshot.boardId());
            });
        });
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(snapshot ->
                ClientAnimationClock.elapsedTicks(snapshot.receivedAtTick()) > STALE_AFTER_TICKS);
        Snapshot snapshot = SNAPSHOTS.values().stream().map(TrackedSnapshot::snapshot)
                .filter(value -> !value.nodes().isEmpty())
                .min(Comparator.comparingDouble(value -> value.distanceToSqr(
                        minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())))
                .orElse(null);
        if (snapshot == null) return;

        int x = Math.max(3, graphics.guiWidth() - HUD_MARGIN_X - HUD_WIDTH);
        int y = HUD_MARGIN_Y;
        graphics.fill(x, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0xB2080812);
        graphics.fill(x, y, x + HUD_WIDTH, y + 2, 0xA0FFFFFF);
        graphics.fill(x, y + HUD_HEIGHT - 1, x + HUD_WIDTH, y + HUD_HEIGHT, 0xA0000000);
        graphics.text(minecraft.font, Component.translatable("hud.astral_craft.board_round", snapshot.round()),
                x + 7, y + 6, 0xFFFFFFFF, true);

        int plotX = x + 7;
        int plotY = y + 25;
        Bounds bounds = Bounds.of(snapshot.nodes());
        if (bounds != null) {
            for (Node node : snapshot.nodes()) {
                ScreenPoint point = point(node.x(), node.z(), bounds, plotX, plotY, MAP_SIZE);
                graphics.fill(point.x() - 2, point.y() - 2, point.x() + 3, point.y() + 3,
                        node.start() ? START_NODE_COLOR : NORMAL_NODE_COLOR);
            }
            Map<PanelKey, List<Pawn>> grouped = new LinkedHashMap<>();
            for (Pawn pawn : snapshot.pawns()) {
                grouped.computeIfAbsent(new PanelKey(pawn.x(), pawn.z()), ignored -> new ArrayList<>()).add(pawn);
            }
            for (Map.Entry<PanelKey, List<Pawn>> entry : grouped.entrySet()) {
                ScreenPoint point = point(entry.getKey().x(), entry.getKey().z(), bounds, plotX, plotY, MAP_SIZE);
                renderMapPortraits(graphics, entry.getValue(), point.x(), point.y(), plotX, plotY, MAP_SIZE);
            }
        }

        int rowsX = x + MAP_SIZE + 18;
        int rowY = y + 24;
        for (Pawn pawn : snapshot.pawns().stream().limit(4).toList()) {
            renderParticipantRow(graphics, minecraft, snapshot, pawn, rowsX, rowY, HUD_WIDTH - MAP_SIZE - 26);
            rowY += 31;
        }
    }

    private static void renderParticipantRow(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                             Snapshot snapshot, Pawn pawn, int x, int y, int width) {
        boolean current = pawn.slotId().equals(snapshot.currentSlotId());
        graphics.fill(x, y, x + width, y + 28, current ? 0xCC243426 : 0x9A11121B);
        if (current) graphics.fill(x, y, x + 3, y + 28, 0xFF55FF70);
        int portraitX = x + 5;
        int portraitY = y + 2;
        graphics.fill(portraitX - 1, portraitY - 1,
                portraitX + ROW_PORTRAIT_SIZE + 1, portraitY + ROW_PORTRAIT_SIZE + 1,
                current ? 0xFF78FF8A : 0xFF696976);
        AstralStatusIconRenderer.renderCharacterSkinHead(graphics, pawn.characterId(), pawn.skinId(),
                portraitX, portraitY, ROW_PORTRAIT_SIZE, 255);
        if (pawn.knockedDown()) {
            renderKnockdownMask(graphics, portraitX, portraitY, ROW_PORTRAIT_SIZE);
        }
        String name = minecraft.font.plainSubstrByWidth(pawn.controllerName(), Math.max(20, width - 37));
        graphics.text(minecraft.font, name, x + 34, y + 3, 0xFFFFFFFF, true);
        Component values = Component.translatable("hud.astral_craft.board_stats",
                pawn.starCoins(), pawn.health(), pawn.maximumHealth());
        graphics.text(minecraft.font, values, x + 34, y + 14, 0xFFE7DFFF, false);
        String stars = "★".repeat(Math.clamp(pawn.stars(), 0, 3))
                + "☆".repeat(Math.clamp(3 - pawn.stars(), 0, 3));
        graphics.text(minecraft.font, stars, x + width - minecraft.font.width(stars) - 5,
                y + 14, 0xFFFFD34E, true);
    }

    private static void renderMapPortraits(GuiGraphicsExtractor graphics, List<Pawn> pawns,
                                           int centerX, int centerY, int plotX, int plotY, int plotSize) {
        List<ScreenPoint> offsets = portraitOffsets(pawns.size());
        for (int index = 0; index < pawns.size(); index++) {
            Pawn pawn = pawns.get(index);
            ScreenPoint offset = offsets.get(index);
            int x = Mth.clamp(centerX + offset.x() - MAP_PORTRAIT_SIZE / 2,
                    plotX, plotX + plotSize - MAP_PORTRAIT_SIZE);
            int y = Mth.clamp(centerY + offset.y() - MAP_PORTRAIT_SIZE / 2,
                    plotY, plotY + plotSize - MAP_PORTRAIT_SIZE);
            graphics.fill(x - 1, y - 1, x + MAP_PORTRAIT_SIZE + 1, y + MAP_PORTRAIT_SIZE + 1, 0xE8000000);
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, pawn.characterId(), pawn.skinId(),
                    x, y, MAP_PORTRAIT_SIZE, 255);
            if (pawn.knockedDown()) renderKnockdownMask(graphics, x, y, MAP_PORTRAIT_SIZE);
        }
    }

    private static void renderKnockdownMask(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0x9A777777);
        graphics.fill(x, y, x + size, y + 1, 0xFFE0E0E0);
        graphics.fill(x, y + size - 1, x + size, y + size, 0xFF444444);
        graphics.fill(x, y, x + 1, y + size, 0xFFE0E0E0);
        graphics.fill(x + size - 1, y, x + size, y + size, 0xFF444444);
    }

    private static List<ScreenPoint> portraitOffsets(int count) {
        if (count <= 1) return List.of(new ScreenPoint(0, 0));
        if (count == 2) return List.of(new ScreenPoint(-5, 0), new ScreenPoint(5, 0));
        if (count == 3) return List.of(new ScreenPoint(0, -5), new ScreenPoint(-5, 5), new ScreenPoint(5, 5));
        if (count == 4) return List.of(new ScreenPoint(-5, -5), new ScreenPoint(5, -5),
                new ScreenPoint(-5, 5), new ScreenPoint(5, 5));
        List<ScreenPoint> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int column = index % 3;
            int row = index / 3;
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

    private record TrackedSnapshot(Snapshot snapshot, double receivedAtTick) {}

    private record Snapshot(String boardId, int centerX, int centerY, int centerZ,
                            List<Node> nodes, List<Pawn> pawns, boolean enabled,
                            int round, UUID currentSlotId) {
        private double distanceToSqr(double x, double y, double z) {
            double dx = this.centerX + 0.5D - x;
            double dy = this.centerY + 0.5D - y;
            double dz = this.centerZ + 0.5D - z;
            return dx * dx + dy * dy + dz * dz;
        }

        private static Optional<Snapshot> parse(String encoded) {
            if (encoded == null || encoded.isBlank()) return Optional.empty();
            String[] parts = encoded.split("\\|", -1);
            if (parts.length < 9 || parts[0].isBlank()) return Optional.empty();
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
                return Optional.of(new Snapshot(parts[0], Integer.parseInt(center[0]), Integer.parseInt(center[1]),
                        Integer.parseInt(center[2]), List.copyOf(nodes), List.copyOf(pawns),
                        Boolean.parseBoolean(parts[4]), Integer.parseInt(parts[7]), current));
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
    private record ScreenPoint(int x, int y) {}

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
        private float width() { return Math.max(1, this.maxX - this.minX); }
        private float depth() { return Math.max(1, this.maxZ - this.minZ); }

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