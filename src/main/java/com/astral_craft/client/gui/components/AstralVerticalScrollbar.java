package com.astral_craft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class AstralVerticalScrollbar {

    public static final int DEFAULT_WIDTH = 5;

    public static void render(GuiGraphicsExtractor graphics, int x, int y, int height, float scroll, float maxScroll) {
        if (maxScroll <= 0.5F || height <= 0) return;
        int trackW = DEFAULT_WIDTH;
        graphics.fill(x, y, x + trackW, y + height, 0x6633333A);
        graphics.fill(x + 1, y + 1, x + trackW - 1, y + height - 1, 0x44101018);
        int thumbH = thumbHeight(height, maxScroll);
        int thumbY = thumbY(y, height, scroll, maxScroll, thumbH);
        graphics.fill(x, thumbY, x + trackW, thumbY + thumbH, 0xEEFFFFFF);
        graphics.fill(x + 1, thumbY + 1, x + trackW - 1, thumbY + thumbH - 1, 0xFFE83CA8);
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y, int height, float maxScroll) {
        if (maxScroll <= 0.5F || height <= 0) return false;
        return mouseX >= x - 2 && mouseX <= x + DEFAULT_WIDTH + 2 && mouseY >= y && mouseY <= y + height;
    }

    public static float scrollFromMouse(double mouseY, int y, int height, float maxScroll) {
        if (maxScroll <= 0.5F || height <= 0) return 0.0F;
        int thumbH = thumbHeight(height, maxScroll);
        int travel = Math.max(1, height - thumbH);
        float relative = (float) ((mouseY - y - thumbH / 2.0F) / travel);
        return Mth.clamp(relative * maxScroll, 0.0F, maxScroll);
    }

    protected static int thumbHeight(int height, float maxScroll) {
        return Math.max(18, (int) (height * (height / (height + maxScroll))));
    }

    protected static int thumbY(int y, int height, float scroll, float maxScroll, int thumbH) {
        float normalized = maxScroll <= 0.5F ? 0.0F : Mth.clamp(scroll / maxScroll, 0.0F, 1.0F);
        return y + Math.round((height - thumbH) * normalized);
    }

}