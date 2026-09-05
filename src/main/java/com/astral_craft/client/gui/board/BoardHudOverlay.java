package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload.PawnView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Compact 2D board status HUD. It intentionally omits the board outline on large maps. */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    private static final int HEADER_HEIGHT = 23;
    private static final int ROW_HEIGHT = 27;
    private static final int PORTRAIT_SIZE = 21;
    private static final int HUD_MARGIN_X = 8;
    private static final int HUD_MARGIN_Y = 32;
    private static final int CURRENT_COLOR = 0xFF55FF70;
    private static final double STALE_AFTER_TICKS = 40.0D;
    private static final Map<UUID, TrackedSnapshot> SNAPSHOTS = new LinkedHashMap<>();

    public static void acceptSnapshot(BoardHudSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BoardProtectionWorldRenderer.acceptSnapshot(payload);
            if (payload.playing()) {
                SNAPSHOTS.put(payload.boardId(), new TrackedSnapshot(payload, ClientAnimationClock.nowTicks()));
            } else {
                SNAPSHOTS.remove(payload.boardId());
            }
        });
    }

    public static void clear(UUID boardId) {
        if (boardId == null) return;
        SNAPSHOTS.remove(boardId);
        BoardProtectionWorldRenderer.clear(boardId);
    }

    public static boolean isTracking(UUID boardId) {
        return boardId != null && SNAPSHOTS.containsKey(boardId);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || SNAPSHOTS.isEmpty()) return;
        SNAPSHOTS.values().removeIf(snapshot -> ClientAnimationClock.elapsedTicks(snapshot.receivedAtTick()) > STALE_AFTER_TICKS);
        BoardHudSnapshotPayload snapshot = SNAPSHOTS.values().stream().map(TrackedSnapshot::snapshot)
                .min(Comparator.comparingDouble(value -> distanceToSqr(value.center(),
                        minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()))).orElse(null);
        if (snapshot == null || snapshot.pawns().isEmpty()) return;
        int rowCount = Math.min(4, snapshot.pawns().size());
        int width = Math.clamp(graphics.guiWidth() - 6, 156, 180);
        List<FormattedCharSequence> objectiveLines = tutorialObjectiveLines(minecraft, snapshot, width - 14);
        int objectiveHeight = objectiveLines.isEmpty() ? 0 : 8 + objectiveLines.size() * 10 + 5;
        int height = HEADER_HEIGHT + rowCount * ROW_HEIGHT + 4 + objectiveHeight;
        int x = Math.max(3, graphics.guiWidth() - HUD_MARGIN_X - width);
        int y = HUD_MARGIN_Y;
        graphics.fill(x, y, x + width, y + height, 0xB6080812);
        graphics.fill(x, y, x + width, y + 2, 0xA0FFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xA0000000);
        Component round = Component.translatable("hud.astral_craft.board_round", snapshot.round());
        graphics.text(minecraft.font, round, x + 7, y + 7, 0xFFFFFFFF, true);
        int rowY = y + HEADER_HEIGHT;
        for (int index = 0; index < rowCount; index++) {
            renderParticipantRow(graphics, minecraft, snapshot, snapshot.pawns().get(index),
                    x + 4, rowY + index * ROW_HEIGHT, width - 8);
        }
        if (!objectiveLines.isEmpty()) {
            int objectiveY = rowY + rowCount * ROW_HEIGHT + 4;
            graphics.fill(x + 5, objectiveY - 3, x + width - 5, objectiveY - 2, 0x557F718A);
            for (FormattedCharSequence line : objectiveLines) {
                graphics.text(minecraft.font, line, x + 7, objectiveY, 0xFFFFE6A3, false);
                objectiveY += 10;
            }
        }
    }

    private static List<FormattedCharSequence> tutorialObjectiveLines(Minecraft minecraft, BoardHudSnapshotPayload snapshot, int width) {
        if (!BoardTutorialGuide.active(snapshot.boardId())) return List.of();
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(minecraft.font.split(Component.translatable("hud.astral_craft.board.tutorial_objective"), width));
        lines.addAll(minecraft.font.split(Component.translatable("hud.astral_craft.board.tutorial_star_rule"), width));
        return List.copyOf(lines);
    }

    private static void renderParticipantRow(GuiGraphicsExtractor graphics, Minecraft minecraft, BoardHudSnapshotPayload snapshot, PawnView pawn, int x, int y, int width) {
        boolean current = pawn.slotId().equals(snapshot.currentSlotId());
        graphics.fill(x, y, x + width, y + ROW_HEIGHT - 2, 0xA811121B);
        if (current) graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 2, CURRENT_COLOR);
        int portraitX = x + 7;
        int portraitY = y + 2;
        graphics.fill(portraitX - 1, portraitY - 1,
                portraitX + PORTRAIT_SIZE + 1, portraitY + PORTRAIT_SIZE + 1, 0xFF696976);
        graphics.fill(portraitX, portraitY,
                portraitX + PORTRAIT_SIZE, portraitY + PORTRAIT_SIZE, 0xFF07070B);
        AstralStatusIconRenderer.renderCharacterSkinHead(graphics, pawn.characterId(), pawn.skinId().getPath(),
                portraitX, portraitY, PORTRAIT_SIZE, 255);
        if (pawn.knockedDown()) renderKnockdownMask(graphics, portraitX, portraitY, PORTRAIT_SIZE);
        if (pawn.disconnectedHuman()) renderDisconnectedMark(graphics, portraitX, portraitY, PORTRAIT_SIZE);
        int textX = portraitX + PORTRAIT_SIZE + 7;
        Component hand = Component.translatable("hud.astral_craft.board_hand_count", pawn.handCount());
        int handX = x + width - minecraft.font.width(hand) - 5;
        int availableNameWidth = Math.max(20, handX - textX - 5);
        String name = minecraft.font.plainSubstrByWidth(pawn.controllerName(), availableNameWidth);
        graphics.text(minecraft.font, name, textX, y + 3, pawn.knockedDown() ? 0xFF9A9AA2 : 0xFFFFFFFF, true);
        Component values = Component.translatable("hud.astral_craft.board_stats", pawn.starCoins(), pawn.health(), pawn.maximumHealth());
        graphics.text(minecraft.font, values, textX, y + 14, pawn.knockedDown() ? 0xFF888892 : 0xFFE7DFFF, false);
        graphics.text(minecraft.font, hand, handX, y + 3, pawn.knockedDown() ? 0xFF888892 : 0xFF9FD8FF, false);
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

    private static double distanceToSqr(BlockPos center, double x, double y, double z) {
        double dx = center.getX() + 0.5D - x;
        double dy = center.getY() + 0.5D - y;
        double dz = center.getZ() + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private record TrackedSnapshot(BoardHudSnapshotPayload snapshot, double receivedAtTick) {}

}
