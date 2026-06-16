package com.astral_craft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class AstralFancyButton {

    public static long handCursor;
    public static long arrowCursor;
    public static boolean handCursorActive;

    public static int buttonRadius = 8;
    public static int tabRadius = 7;

    public static void renderButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, int accentColor) {
        int border = selected ? 0xFFE8FFE0 : hovered ? 0xFFFFE0F4 : 0xCC111118;
        int top = selected ? 0xFF92FF22 : hovered ? brighten(accentColor, 30) : accentColor;
        int bottom = selected ? 0xFF57C800 : darken(accentColor, hovered ? 10 : 28);
        int textColor = selected ? 0xFF101018 : hovered ? 0xFF101018 : 0xFFFFFFFF;
        thisRenderRoundedGradient(graphics, x, y, width, height, top, bottom, border, 0x77101018, buttonRadius);
        drawCentered(graphics, font, label, x, y, width, height, textColor);
    }

    public static void renderTab(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, int accentColor) {
        int baseTop = selected ? 0xFF8CFF20 : hovered ? brighten(accentColor, 12) : darken(accentColor, 18);
        int baseBottom = selected ? 0xFF52B900 : hovered ? accentColor : darken(accentColor, 38);
        int border = selected ? 0xFFE7FFD6 : hovered ? 0xFFFFDDF4 : 0xAA111118;
        int textColor = selected ? 0xFF101018 : hovered ? 0xFF101018 : 0xFFFFFFFF;
        thisRenderRoundedGradient(graphics, x, y, width, height, baseTop, baseBottom, border, 0x66101018, tabRadius);
        renderRoundedHighlight(graphics, x, y, width, height, tabRadius, selected ? 0x66FFFFFF : 0x33FFFFFF);
        drawCentered(graphics, font, label, x, y, width, height, textColor);
    }

    public static void renderIconFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected, boolean hovered) {
        int border = selected ? 0xFFE7FFD6 : hovered ? 0xFFFFDDF4 : 0xAA111118;
        int top = selected ? 0x553DFF5A : hovered ? 0x55E83CA8 : 0x55262632;
        int bottom = selected ? 0x8831B33D : hovered ? 0x774C163E : 0x77161620;
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x66101018);
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height / 2, top);
        graphics.fill(x + 1, y + height / 2, x + width - 1, y + height - 1, bottom);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0x77000000);
    }

    public static void renderRoundedGradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int topColor, int bottomColor, int borderColor, int shadowColor, int radius) {
        thisRenderRoundedGradient(graphics, x, y, width, height, topColor, bottomColor, borderColor, shadowColor, radius);
    }

    public static void thisRenderRoundedGradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int topColor, int bottomColor, int borderColor, int shadowColor, int radius) {
        int r = Math.clamp(radius, 1, Math.min(width, height) / 2);
        fillRoundedRect(graphics, x + 3, y + 3, width, height, r, shadowColor);
        fillRoundedRect(graphics, x, y, width, height, r, borderColor);
        int innerX = x + 1;
        int innerY = y + 1;
        int innerW = Math.max(1, width - 2);
        int innerH = Math.max(1, height - 2);
        int innerR = Math.max(1, r - 1);
        for (int row = 0; row < innerH; row++) {
            int color = lerpColor(topColor, bottomColor, innerH == 1 ? 0.0F : (float) row / (float) (innerH - 1));
            int inset = roundedInset(row, innerH, innerR);
            graphics.fill(innerX + inset, innerY + row, innerX + innerW - inset, innerY + row + 1, color);
        }

        renderRoundedHighlight(graphics, x, y, width, height, r, 0x34FFFFFF);
    }

    public static void renderRoundedHighlight(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
        int r = Math.clamp(radius, 1, Math.min(width, height) / 2);
        int highlightHeight = Math.clamp(height / 4, 2, 4);
        for (int row = 0; row < highlightHeight; row++) {
            int inset = roundedInset(row + 1, height, r) + 2;
            if (x + inset < x + width - inset) {
                graphics.fill(x + inset, y + 1 + row, x + width - inset, y + 2 + row, color);
            }
        }
    }

    public static void fillRoundedRect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
        int r = Math.clamp(radius, 1, Math.min(width, height) / 2);
        for (int row = 0; row < height; row++) {
            int inset = roundedInset(row, height, r);
            if (x + inset < x + width - inset) {
                graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            }
        }
    }

    public static int roundedInset(int row, int height, int radius) {
        if (radius <= 1) return 0;
        float cy;
        if (row < radius) {
            cy = radius - row - 0.5F;
        } else if (row >= height - radius) {
            cy = row - (height - radius) + 0.5F;
        } else {
            return 0;
        }

        float rr = radius - 0.5F;
        float inside = Math.max(0.0F, rr * rr - cy * cy);
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(inside)));
    }

    public static int lerpColor(int from, int to, float t) {
        float value = Math.clamp(t, 0.0F, 1.0F);
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * value);
        int r = Math.round(((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * value);
        int g = Math.round(((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * value);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * value);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, int color) {
        int textX = x + Math.max(0, (width - font.width(label)) / 2);
        int textY = y + Math.max(0, (height - 8) / 2);
        graphics.text(font, label, textX, textY, color, false);
    }

    public static int brighten(int color, int amount) {
        int alpha = color & 0xFF000000;
        int red = Math.min(255, ((color >> 16) & 255) + amount);
        int green = Math.min(255, ((color >> 8) & 255) + amount);
        int blue = Math.min(255, (color & 255) + amount);
        return alpha | (red << 16) | (green << 8) | blue;
    }

    public static int darken(int color, int amount) {
        int alpha = color & 0xFF000000;
        int red = Math.max(0, ((color >> 16) & 255) - amount);
        int green = Math.max(0, ((color >> 8) & 255) - amount);
        int blue = Math.max(0, (color & 255) - amount);
        return alpha | (red << 16) | (green << 8) | blue;
    }

    public static void setHandCursor(boolean hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hand == handCursorActive) return;
        if (handCursor == 0L) {
            handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        }

        if (arrowCursor == 0L) {
            arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
        }

        long window = minecraft.getWindow().handle();
        GLFW.glfwSetCursor(window, hand ? handCursor : arrowCursor);
        handCursorActive = hand;
    }

}