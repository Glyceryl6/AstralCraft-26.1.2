package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

/** Compact 2D board status HUD. It intentionally omits the board outline on large maps. */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    private static final int HUD_WIDTH = 232;
    private static final int HEADER_HEIGHT = 23;
    private static final int ROW_HEIGHT = 27;
    private static final int PORTRAIT_SIZE = 21;
    private static final int HUD_MARGIN_X = 8;
    private static final int HUD_MARGIN_Y = 32;
    private static final int CURRENT_COLOR = 0xFF55FF70;
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
        if (minecraft.player == null || minecraft.options.hideGui || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(snapshot -> ClientAnimationClock.elapsedTicks(snapshot.receivedAtTick()) > STALE_AFTER_TICKS);
        Snapshot snapshot = SNAPSHOTS.values().stream().map(TrackedSnapshot::snapshot)
                .min(Comparator.comparingDouble(value -> value.distanceToSqr(
                        minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()))).orElse(null);
        if (snapshot == null || snapshot.pawns().isEmpty()) return;
        int rowCount = Math.min(4, snapshot.pawns().size());
        int width = Math.clamp(graphics.guiWidth() - 6, 156, 180);
        int height = HEADER_HEIGHT + rowCount * ROW_HEIGHT + 4;
        int x = Math.max(3, graphics.guiWidth() - HUD_MARGIN_X - width);
        int y = HUD_MARGIN_Y;
        graphics.fill(x, y, x + width, y + height, 0xB6080812);
        graphics.fill(x, y, x + width, y + 2, 0xA0FFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xA0000000);
        Component round = Component.translatable("hud.astral_craft.board_round", snapshot.round());
        graphics.text(minecraft.font, round, x + 7, y + 7, 0xFFFFFFFF, true);
        int rowY = y + HEADER_HEIGHT;
        for (int index = 0; index < rowCount; index++) {
            thisRenderParticipantRow(graphics, minecraft, snapshot, snapshot.pawns().get(index),
                    x + 4, rowY + index * ROW_HEIGHT, width - 8);
        }
    }

    private static void thisRenderParticipantRow(GuiGraphicsExtractor graphics, Minecraft minecraft, Snapshot snapshot, Pawn pawn, int x, int y, int width) {
        boolean current = pawn.slotId().equals(snapshot.currentSlotId());
        graphics.fill(x, y, x + width, y + ROW_HEIGHT - 2, 0xA811121B);
        if (current) graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 2, CURRENT_COLOR);
        int portraitX = x + 7;
        int portraitY = y + 2;
        graphics.fill(portraitX - 1, portraitY - 1,
                portraitX + PORTRAIT_SIZE + 1, portraitY + PORTRAIT_SIZE + 1, 0xFF696976);
        graphics.fill(portraitX, portraitY,
                portraitX + PORTRAIT_SIZE, portraitY + PORTRAIT_SIZE, 0xFF07070B);
        AstralStatusIconRenderer.renderCharacterSkinHead(graphics, pawn.characterId(), pawn.skinId(),
                portraitX, portraitY, PORTRAIT_SIZE, 255);
        if (pawn.knockedDown()) renderKnockdownMask(graphics, portraitX, portraitY, PORTRAIT_SIZE);
        if (pawn.disconnectedHuman()) renderDisconnectedMark(graphics, portraitX, portraitY, PORTRAIT_SIZE);
        int textX = portraitX + PORTRAIT_SIZE + 7;
        int availableNameWidth = Math.max(20, width - (textX - x) - 7);
        String name = minecraft.font.plainSubstrByWidth(pawn.controllerName(), availableNameWidth);
        graphics.text(minecraft.font, name, textX, y + 3, pawn.knockedDown() ? 0xFF9A9AA2 : 0xFFFFFFFF, true);
        Component values = Component.translatable("hud.astral_craft.board_stats", pawn.starCoins(), pawn.health(), pawn.maximumHealth());
        graphics.text(minecraft.font, values, textX, y + 14, pawn.knockedDown() ? 0xFF888892 : 0xFFE7DFFF, false);
        String stars = "★".repeat(Math.clamp(pawn.stars(), 0, 3)) + "☆".repeat(Math.clamp(3 - pawn.stars(), 0, 3));
        graphics.text(minecraft.font, stars, x + width - minecraft.font.width(stars) - 5, y + 14, 0xFFFFD34E, true);
    }

    private static void renderDisconnectedMark(GuiGraphicsExtractor graphics, int x, int y, int size) {
        int markSize = 8;
        int left = x + size - markSize;
        int top = y;
        graphics.fill(left - 1, top, left + markSize + 1, top + markSize + 1, 0xC0000000);
        for (int offset = 1; offset < markSize; offset++) {
            graphics.fill(left + offset, top + offset, left + offset + 2, top + offset + 2, 0xFFFF3030);
            graphics.fill(left + markSize - offset, top + offset, left + markSize - offset + 2,
                    top + offset + 2, 0xFFFF3030);
        }
    }

    private static void renderKnockdownMask(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xA06A6A6A);
        graphics.fill(x, y, x + size, y + 1, 0xFFD0D0D0);
        graphics.fill(x, y + size - 1, x + size, y + size, 0xFF3A3A3A);
        graphics.fill(x, y, x + 1, y + size, 0xFFD0D0D0);
        graphics.fill(x + size - 1, y, x + size, y + size, 0xFF3A3A3A);
    }

    private record TrackedSnapshot(Snapshot snapshot, double receivedAtTick) {}

    private record Snapshot(String boardId, int centerX, int centerY, int centerZ,
                            List<Pawn> pawns, boolean enabled, int round, UUID currentSlotId) {
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
            List<Pawn> pawns = new ArrayList<>();
            for (String raw : parts[6].split(";")) {
                if (raw.isBlank()) continue;
                String[] fields = raw.split(",", 13);
                if (fields.length < 12) continue;
                try {
                    pawns.add(new Pawn(Identifier.parse(fields[3]), fields[4],
                            UUID.fromString(fields[5]), decodeName(fields[6]),
                            Integer.parseInt(fields[7]), Integer.parseInt(fields[8]),
                            Integer.parseInt(fields[9]), Integer.parseInt(fields[10]),
                            "1".equals(fields[11]), fields.length >= 13 && "1".equals(fields[12])));
                } catch (IllegalArgumentException ignored) {}
            }
            try {
                UUID current = parts[8].isBlank() ? new UUID(0L, 0L) : UUID.fromString(parts[8]);
                return Optional.of(new Snapshot(parts[0], Integer.parseInt(center[0]),
                        Integer.parseInt(center[1]), Integer.parseInt(center[2]), List.copyOf(pawns),
                        Boolean.parseBoolean(parts[4]) && "PLAYING".equals(parts[5]),
                        Integer.parseInt(parts[7]), current));
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

    private record Pawn(Identifier characterId, String skinId, UUID slotId,
                        String controllerName, int starCoins, int health,
                        int maximumHealth, int stars, boolean knockedDown, boolean disconnectedHuman) {}

}
