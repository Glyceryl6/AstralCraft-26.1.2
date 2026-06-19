package com.astral_craft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class AstralHorizontalScrollbar {

    public static final int DEFAULT_HEIGHT = 5;

    public static void render(GuiGraphicsExtractor graphics, int x, int y, int width, float scroll, float maxScroll) {
        if (maxScroll <= 0.5F || width <= 0) return;
        int trackH = DEFAULT_HEIGHT;
        graphics.fill(x, y, x + width, y + trackH, 0x6633333A);
        graphics.fill(x + 1, y + 1, x + width - 1, y + trackH - 1, 0x44101018);
        int thumbW = thumbWidth(width, maxScroll);
        int thumbX = thumbX(x, width, scroll, maxScroll, thumbW);
        graphics.fill(thumbX, y, thumbX + thumbW, y + trackH, 0xEEFFFFFF);
        graphics.fill(thumbX + 1, y + 1, thumbX + thumbW - 1, y + trackH - 1, 0xFFE83CA8);
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y, int width, float maxScroll) {
        if (maxScroll <= 0.5F || width <= 0) return false;
        return mouseX >= x && mouseX <= x + width && mouseY >= y - 2 && mouseY <= y + DEFAULT_HEIGHT + 2;
    }

    public static float scrollFromMouse(double mouseX, int x, int width, float maxScroll) {
        if (maxScroll <= 0.5F || width <= 0) return 0.0F;
        int thumbW = thumbWidth(width, maxScroll);
        int travel = Math.max(1, width - thumbW);
        float relative = (float) ((mouseX - x - thumbW / 2.0F) / travel);
        return Mth.clamp(relative * maxScroll, 0.0F, maxScroll);
    }

    protected static int thumbWidth(int width, float maxScroll) {
        return Math.max(22, (int) (width * (width / (width + maxScroll))));
    }

    protected static int thumbX(int x, int width, float scroll, float maxScroll, int thumbW) {
        float normalized = maxScroll <= 0.5F ? 0.0F : Mth.clamp(scroll / maxScroll, 0.0F, 1.0F);
        return x + Math.round((width - thumbW) * normalized);
    }

}