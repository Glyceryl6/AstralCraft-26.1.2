package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.BoardRouteStatePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Bottom HUD timer shown while the player is choosing a branch in the world. */
public class BoardRouteDecisionOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_route_decision");
    private static DecisionState state = DecisionState.EMPTY;

    public static void accept(BoardRouteStatePayload payload) {
        if (!payload.active() || payload.branches().isEmpty() || payload.decisionTicks() <= 0) {
            state = DecisionState.EMPTY;
            return;
        }
        state = new DecisionState(payload.decisionTicks(), payload.decisionDurationTicks(),
                payload.characterId(), payload.skinId(), ClientAnimationClock.nowTicks(), true);
    }

    public static void clear() {
        state = DecisionState.EMPTY;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        DecisionState current = state;
        if (!current.active() || minecraft.player == null || minecraft.options.hideGui) return;
        int elapsedTicks = (int) Math.max(0.0D,
                ClientAnimationClock.elapsedTicks(current.receivedAtTick()));
        int remainingTicks = Math.max(0, current.remainingTicks() - elapsedTicks);
        if (remainingTicks <= 0) {
            state = DecisionState.EMPTY;
            return;
        }
        BoardDecisionProgressBar.render(graphics, minecraft.font,
                current.characterId(), current.skinId(), remainingTicks, current.durationTicks(),
                graphics.guiWidth() / 2, graphics.guiHeight() - 18,
                Math.min(270, graphics.guiWidth() - 44));
    }

    private record DecisionState(int remainingTicks, int durationTicks,
                                 Identifier characterId, Identifier skinId,
                                 double receivedAtTick, boolean active) {
        private static final DecisionState EMPTY = new DecisionState(
                0, 1, AstralCraft.prefix("mimi"), Identifier.withDefaultNamespace("default"), 0.0D, false);
    }

}