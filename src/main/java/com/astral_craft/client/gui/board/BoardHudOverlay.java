package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardHudSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Receives board snapshots. The former large 2D HUD is now rendered as an in-world board-side panel. */
public class BoardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_hud_overlay");

    public static void acceptSnapshot(BoardHudSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BoardProtectionWorldRenderer.acceptSnapshot(payload.encoded());
            BoardWorldStatusRenderer.acceptSnapshot(payload.encoded());
        });
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        // Kept as a registered layer for packet/layer compatibility. The status display is world-space now.
    }
}
