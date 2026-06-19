package com.astral_craft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public class AstralFancyButton {

    public static long handCursor;
    public static long arrowCursor;
    public static boolean handCursorActive;

    public static int buttonRadius = 8;
    public static int tabRadius = 7;
    public static int thickBorder = 3;
    public static int outerBorder = 2;

    public static void renderButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, int accentColor) {
        renderButton(graphics, font, label, x, y, width, height, selected, hovered, ButtonStyle.button(accentColor));
    }

    public static void renderButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, ButtonStyle style) {
        renderStyledBox(graphics, x, y, width, height, selected, hovered, style);
        drawCentered(graphics, font, label, x, y, width, height, style.textColor(selected, hovered), style.textShadowColor(selected, hovered), style.textOutlineColor(selected, hovered), style.textScale());
    }

    public static void renderTab(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, int accentColor) {
        renderTab(graphics, font, label, x, y, width, height, selected, hovered, ButtonStyle.tab(accentColor));
    }

    public static void renderTab(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, ButtonStyle style) {
        renderStyledBox(graphics, x, y, width, height, selected, hovered, style);
        if (style.diagonalCornerColor() != 0) {
            renderDiagonalCorner(graphics, x + style.outerThickness(), y + style.outerThickness(), Math.min(11, width / 4), Math.min(11, height / 2), style.diagonalCornerColor());
        }
        drawCentered(graphics, font, label, x, y, width, height, style.textColor(selected, hovered), style.textShadowColor(selected, hovered), style.textOutlineColor(selected, hovered), style.textScale());
    }

    public static void renderIconFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected, boolean hovered) {
        renderStyledBox(graphics, x, y, width, height, selected, hovered, ButtonStyle.iconFrame());
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0x66000000);
    }

    public static void renderStyledBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected, boolean hovered, ButtonStyle style) {
        renderBoxGradient(graphics, x, y, width, height,
                style.topColor(selected, hovered),
                style.bottomColor(selected, hovered),
                style.borderColor(selected, hovered),
                style.outerColor(selected, hovered),
                style.shadowColor(),
                style.borderThickness(),
                style.outerThickness(),
                style.shadowOffsetX(),
                style.shadowOffsetY(),
                style.highlightColor());
    }

    public static void renderBoxGradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int topColor, int bottomColor, int borderColor, int outerColor, int shadowColor, int borderThickness, int outerThickness) {
        renderBoxGradient(graphics, x, y, width, height, topColor, bottomColor, borderColor, outerColor, shadowColor, borderThickness, outerThickness, 3, 3, 0x30FFFFFF);
    }

    public static void renderBoxGradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int topColor, int bottomColor, int borderColor, int outerColor, int shadowColor, int borderThickness, int outerThickness, int shadowOffsetX, int shadowOffsetY, int highlightColor) {
        if (width <= 0 || height <= 0) return;
        int safeOuter = Math.clamp(outerThickness, 0, Math.max(0, Math.min(width, height) / 2));
        int safeBorder = Math.clamp(borderThickness, 0, Math.max(0, Math.min(width, height) / 2 - safeOuter));
        if ((shadowColor >>> 24) != 0 && (shadowOffsetX != 0 || shadowOffsetY != 0)) {
            graphics.fill(x + shadowOffsetX, y + shadowOffsetY, x + width + shadowOffsetX, y + height + shadowOffsetY, shadowColor);
        }
        graphics.fill(x, y, x + width, y + height, outerColor);
        if (safeOuter > 0 && width > safeOuter * 2 && height > safeOuter * 2) {
            graphics.fill(x + safeOuter, y + safeOuter, x + width - safeOuter, y + height - safeOuter, borderColor);
        }

        int inner = safeOuter + safeBorder;
        if (width <= inner * 2 || height <= inner * 2) return;
        int innerX0 = x + inner;
        int innerY0 = y + inner;
        int innerX1 = x + width - inner;
        int innerY1 = y + height - inner;
        int middle = innerY0 + Math.max(1, (innerY1 - innerY0) / 2);
        graphics.fill(innerX0, innerY0, innerX1, middle, topColor);
        graphics.fill(innerX0, middle, innerX1, innerY1, bottomColor);
        if ((highlightColor >>> 24) != 0) {
            graphics.fill(innerX0 + 1, innerY0 + 1, innerX1 - 1, Math.min(innerY0 + 3, innerY1), highlightColor);
        }
    }

    public static void renderFlatBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fillColor, int borderColor, int outerColor, int shadowColor, int borderThickness, int outerThickness) {
        renderBoxGradient(graphics, x, y, width, height, fillColor, fillColor, borderColor, outerColor, shadowColor, borderThickness, outerThickness);
    }

    public static void renderOutlinedBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fillColor, int borderColor, int outerColor, int borderThickness, int outerThickness) {
        renderBoxGradient(graphics, x, y, width, height, fillColor, fillColor, borderColor, outerColor, 0x00000000, borderThickness, outerThickness, 0, 0, 0x00000000);
    }

    public static void renderDiagonalCorner(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        for (int row = 0; row < height; row++) {
            int inset = Math.max(0, width - 1 - row);
            graphics.fill(x, y + row, x + inset, y + row + 1, color);
        }
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

    public static void renderLegacyRoundedButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean selected, boolean hovered, int accentColor) {
        int border = selected ? 0xFFE8FFE0 : hovered ? 0xFFFFE0F4 : 0xCC111118;
        int top = selected ? 0xFF92FF22 : hovered ? brighten(accentColor, 30) : accentColor;
        int bottom = selected ? 0xFF57C800 : darken(accentColor, hovered ? 10 : 28);
        int textColor = selected ? 0xFF101018 : hovered ? 0xFF101018 : 0xFFFFFFFF;
        thisRenderRoundedGradient(graphics, x, y, width, height, top, bottom, border, 0x77101018, buttonRadius);
        drawCentered(graphics, font, label, x, y, width, height, textColor);
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
        drawCentered(graphics, font, label, x, y, width, height, color, 0x00000000, 1.0F);
    }

    public static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, int color, int shadowColor, float textScale) {
        drawCentered(graphics, font, label, x, y, width, height, color, shadowColor, 0x00000000, textScale);
    }

    public static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, int color, int shadowColor, int outlineColor, float textScale) {
        float safeScale = Math.clamp(textScale, 0.45F, 2.25F);
        float scaledWidth = font.width(label) * safeScale;
        float scaledHeight = 8.0F * safeScale;
        float textX = x + Math.max(0.0F, (width - scaledWidth) / 2.0F);
        float textY = y + Math.max(0.0F, (height - scaledHeight) / 2.0F);
        drawText(graphics, font, label, textX, textY, color, shadowColor, outlineColor, safeScale);
    }

    public static void drawText(GuiGraphicsExtractor graphics, Font font, Component label, float x, float y, int color, int shadowColor, float textScale) {
        drawText(graphics, font, label, x, y, color, shadowColor, 0x00000000, textScale);
    }

    public static void drawText(GuiGraphicsExtractor graphics, Font font, Component label, float x, float y, int color, int shadowColor, int outlineColor, float textScale) {
        float safeScale = Math.clamp(textScale, 0.45F, 2.25F);
        graphics.pose().pushMatrix();
        graphics.pose().scale(safeScale, safeScale);
        int scaledX = Math.round(x / safeScale);
        int scaledY = Math.round(y / safeScale);
        if ((shadowColor >>> 24) != 0) {
            graphics.text(font, label, scaledX + 1, scaledY + 1, shadowColor, false);
        }
        if ((outlineColor >>> 24) != 0) {
            graphics.text(font, label, scaledX - 1, scaledY, outlineColor, false);
            graphics.text(font, label, scaledX + 1, scaledY, outlineColor, false);
            graphics.text(font, label, scaledX, scaledY - 1, outlineColor, false);
            graphics.text(font, label, scaledX, scaledY + 1, outlineColor, false);
        }
        graphics.text(font, label, scaledX, scaledY, color, false);
        graphics.pose().popMatrix();
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


    public static Button button(MutableComponent title, int x, int y, int width, int height, ButtonStyle style) {
        return new Button(title, x, y, width, height, style);
    }

    public static Button button(MutableComponent title, int x, int y, int width, int height, int accentColor) {
        return new Button(title, x, y, width, height, ButtonStyle.button(accentColor));
    }

    public static void renderButton(GuiGraphicsExtractor graphics, Font font, Button button, boolean selected, boolean hovered) {
        button.render(graphics, font, selected, hovered);
    }

    public static class Button {

        protected MutableComponent title;
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected ButtonStyle style;

        public Button(MutableComponent title, int x, int y, int width, int height, ButtonStyle style) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.style = style == null ? ButtonStyle.button(0xFFE83CA8) : style;
        }

        public MutableComponent title() {
            return this.title;
        }

        public Button withTitle(MutableComponent title) {
            this.title = title;
            return this;
        }

        public Button withBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Button withStyle(ButtonStyle style) {
            this.style = style == null ? this.style : style;
            return this;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
        }

        public void render(GuiGraphicsExtractor graphics, Font font, boolean selected, boolean hovered) {
            renderStyledBox(graphics, this.x, this.y, this.width, this.height, selected, hovered, this.style);
            drawCentered(graphics, font, this.title, this.x, this.y, this.width, this.height, this.style.textColor(selected, hovered), this.style.textShadowColor(selected, hovered), this.style.textOutlineColor(selected, hovered), this.style.textScale());
        }

    }

    public record ButtonStyle(
            int topColor,
            int bottomColor,
            int hoverTopColor,
            int hoverBottomColor,
            int selectedTopColor,
            int selectedBottomColor,
            int borderColor,
            int hoverBorderColor,
            int selectedBorderColor,
            int outerColor,
            int hoverOuterColor,
            int selectedOuterColor,
            int textColor,
            int hoverTextColor,
            int selectedTextColor,
            int textShadowColor,
            int hoverTextShadowColor,
            int selectedTextShadowColor,
            int textOutlineColor,
            int hoverTextOutlineColor,
            int selectedTextOutlineColor,
            int shadowColor,
            int shadowOffsetX,
            int shadowOffsetY,
            int borderThickness,
            int outerThickness,
            int highlightColor,
            int diagonalCornerColor,
            float textScale) {

        public static ButtonStyle button(int accentColor) {
            return new ButtonStyle(
                    accentColor,
                    darken(accentColor, 26),
                    brighten(accentColor, 38),
                    brighten(darken(accentColor, 10), 18),
                    0xFF92FF22,
                    0xFF57C800,
                    0xFFFFFFFF,
                    0xFFFFFFFF,
                    0xFFFFFFFF,
                    0xFF26304C,
                    0xFF101018,
                    0xFF101018,
                    0xFFFFFFFF,
                    0xFF101018,
                    0xFFFFFFFF,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x77101018,
                    3,
                    3,
                    thickBorder,
                    outerBorder,
                    0x30FFFFFF,
                    0x00000000,
                    1.0F);
        }

        public static ButtonStyle tab(int accentColor) {
            return new ButtonStyle(
                    accentColor,
                    darken(accentColor, 30),
                    brighten(accentColor, 28),
                    darken(accentColor, 8),
                    brighten(accentColor, 18),
                    darken(accentColor, 10),
                    0xFFFFF6FD,
                    0xFFFFFFFF,
                    0xFFFFFFFF,
                    0xFF101018,
                    0xFF101018,
                    0xFF101018,
                    0xFFFFFFFF,
                    0xFF101018,
                    0xFFFFFFFF,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x66101018,
                    3,
                    3,
                    thickBorder,
                    outerBorder,
                    0x30FFFFFF,
                    0xEEFFFFFF,
                    1.0F);
        }

        public static ButtonStyle iconFrame() {
            return new ButtonStyle(
                    0x55262632,
                    0x77161620,
                    0x66E83CA8,
                    0x884C163E,
                    0x665A5A74,
                    0xAA3F4058,
                    0xFF101018,
                    0xFFFFFFFF,
                    0xFFFFFFFF,
                    0xCC000000,
                    0xFF101018,
                    0xFF101018,
                    0xFFFFFFFF,
                    0xFFFFFFFF,
                    0xFFFFFFFF,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x00000000,
                    0x66101018,
                    3,
                    3,
                    2,
                    2,
                    0x22FFFFFF,
                    0x00000000,
                    1.0F);
        }

        public ButtonStyle withTextScale(float value) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, value);
        }

        public ButtonStyle withTextColors(int normal, int hover, int selected) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, normal, hover, selected, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withTextShadowColors(int normal, int hover, int selected) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, normal, hover, selected, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withTextOutlineColors(int normal, int hover, int selected) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, normal, hover, selected, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withBackgroundColors(int normal, int hover, int selected) {
            return new ButtonStyle(normal, normal, hover, hover, selected, selected, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withBackgroundGradientColors(int normalTop, int normalBottom, int hoverTop, int hoverBottom, int selectedTop, int selectedBottom) {
            return new ButtonStyle(normalTop, normalBottom, hoverTop, hoverBottom, selectedTop, selectedBottom, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withBorderColors(int normal, int hover, int selected) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, normal, hover, selected, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withOuterColors(int normal, int hover, int selected) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, normal, hover, selected, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withBoxMetrics(int borderThickness, int outerThickness, int shadowOffsetX, int shadowOffsetY) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, shadowOffsetX, shadowOffsetY, borderThickness, outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withShadow(int shadowColor, int shadowOffsetX, int shadowOffsetY) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, shadowColor, shadowOffsetX, shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withHighlightColor(int highlightColor) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, highlightColor, this.diagonalCornerColor, this.textScale);
        }

        public ButtonStyle withDiagonalCornerColor(int diagonalCornerColor) {
            return new ButtonStyle(this.topColor, this.bottomColor, this.hoverTopColor, this.hoverBottomColor, this.selectedTopColor, this.selectedBottomColor, this.borderColor, this.hoverBorderColor, this.selectedBorderColor, this.outerColor, this.hoverOuterColor, this.selectedOuterColor, this.textColor, this.hoverTextColor, this.selectedTextColor, this.textShadowColor, this.hoverTextShadowColor, this.selectedTextShadowColor, this.textOutlineColor, this.hoverTextOutlineColor, this.selectedTextOutlineColor, this.shadowColor, this.shadowOffsetX, this.shadowOffsetY, this.borderThickness, this.outerThickness, this.highlightColor, diagonalCornerColor, this.textScale);
        }

        public int topColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedTopColor;
            return hovered ? this.hoverTopColor : this.topColor;
        }

        public int bottomColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedBottomColor;
            return hovered ? this.hoverBottomColor : this.bottomColor;
        }

        public int borderColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedBorderColor;
            return hovered ? this.hoverBorderColor : this.borderColor;
        }

        public int outerColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedOuterColor;
            return hovered ? this.hoverOuterColor : this.outerColor;
        }

        public int textColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedTextColor;
            return hovered ? this.hoverTextColor : this.textColor;
        }

        public int textShadowColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedTextShadowColor;
            return hovered ? this.hoverTextShadowColor : this.textShadowColor;
        }

        public int textOutlineColor(boolean selected, boolean hovered) {
            if (selected) return this.selectedTextOutlineColor;
            return hovered ? this.hoverTextOutlineColor : this.textOutlineColor;
        }
    }

}
