package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Shared board decision timer with a moving character portrait. */
public class BoardDecisionProgressBar {

    private static final int BAR_HEIGHT = 8;
    private static final int HEAD_SIZE = 18;

    public static void render(
            GuiGraphicsExtractor graphics, Font font, Identifier characterId, Identifier skinId,
            int remainingTicks, int durationTicks, int centerX, int y, int width) {
        if (durationTicks == Integer.MAX_VALUE) return;
        int safeDuration = Math.max(1, durationTicks);
        int safeRemaining = Math.clamp(remainingTicks, 0, safeDuration);
        float progress = 1.0F - safeRemaining / (float) safeDuration;
        int x = centerX - width / 2;
        int innerX = x + 2;
        int innerWidth = Math.max(1, width - 4);
        int fillWidth = Mth.clamp(Math.round(innerWidth * progress), 0, innerWidth);
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, 0xD8121420);
        graphics.fill(innerX, y + 2, x + width - 2, y + BAR_HEIGHT - 2, 0xD83A4059);
        if (fillWidth > 0) {
            graphics.fill(innerX, y + 2, innerX + fillWidth, y + BAR_HEIGHT - 2, 0xFFE85D75);
        }

        int headX = Mth.clamp(innerX + fillWidth - HEAD_SIZE / 2, x - HEAD_SIZE / 2, x + width - HEAD_SIZE / 2);
        Identifier safeCharacter = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        String safeSkin = skinId == null ? "default" : skinId.getPath();
        AstralStatusIconRenderer.renderCharacterSkinHead(graphics, safeCharacter, safeSkin, headX, y - 6, HEAD_SIZE, 255);
    }

}