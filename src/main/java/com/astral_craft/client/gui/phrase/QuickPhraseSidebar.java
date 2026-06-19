package com.astral_craft.client.gui.phrase;

import com.astral_craft.client.gui.components.AstralFancyButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Reusable quick-phrase drawer used by the chat screen mixin and the standalone debug screen. */
public class QuickPhraseSidebar {

    public static final int PANEL_WIDTH = 120;
    public static final int PANEL_MARGIN_RIGHT = 10;
    public static final int PANEL_MARGIN_TOP = 32;
    public static final int PANEL_MARGIN_BOTTOM = 38;
    public static final int ROW_HEIGHT = 22;
    public static final int SCROLLBAR_WIDTH = 5;
    public static final int TOGGLE_WIDTH = 24;
    public static final int TOGGLE_HEIGHT = 44;

    protected final List<Component> phrases = new ArrayList<>();
    protected boolean expanded;
    protected float scrollY;
    protected boolean draggingScrollbar;
    protected double dragStartY;
    protected float dragStartScrollY;

    public QuickPhraseSidebar() {
        for (int i = 0; i < 24; i++) {
            this.phrases.add(Component.translatable("quick_phrase.astral_craft." + i));
        }
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        this.clampScroll(screenHeight);
        this.renderToggle(graphics, font, screenWidth, screenHeight, mouseX, mouseY);
        if (!this.expanded) return;

        int x = this.panelX(screenWidth);
        int y = this.panelY();
        int h = this.panelHeight(screenHeight);
        int right = x + PANEL_WIDTH;
        int bottom = y + h;
        graphics.fill(x - 2, y - 2, right + 2, bottom + 2, 0xDD101018);
        graphics.fill(x, y, right, bottom, 0xE00B0B14);
        graphics.fill(x, y, right, y + 1, 0x80FFFFFF);
        graphics.fill(x, bottom - 1, right, bottom, 0x90000000);
        graphics.fill(x, y, x + 1, bottom, 0x60FFFFFF);
        graphics.fill(right - 1, y, right, bottom, 0x70000000);
        graphics.text(font, Component.translatable("gui.astral_craft.quick_phrases.title"), x + 10, y + 8, 0xFFFFFFFF, false);
        int listX = x + 8;
        int listY = y + 28;
        int listW = PANEL_WIDTH - 20;
        int listH = h - 38;
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < this.phrases.size(); i++) {
            int rowY = listY + i * ROW_HEIGHT - Math.round(this.scrollY);
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listH) continue;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW - SCROLLBAR_WIDTH - 2 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            graphics.fill(listX, rowY, listX + listW - SCROLLBAR_WIDTH - 2, rowY + ROW_HEIGHT - 2, hovered ? 0x884AFF20 : 0x663A3A48);
            graphics.text(font, this.ellipsize(font, this.phrases.get(i), listW - 18), listX + 6, rowY + 7, hovered ? 0xFF101018 : 0xFFEFEFFF, false);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics, screenHeight, listX + listW - SCROLLBAR_WIDTH, listY, listH);
    }

    protected void renderToggle(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int x = this.toggleX(screenWidth);
        int y = this.toggleY(screenHeight);
        boolean hovered = this.isInside(mouseX, mouseY, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        MutableComponent label = Component.literal(this.expanded ? "<" : ">");
        AstralFancyButton.ButtonStyle style = AstralFancyButton.ButtonStyle.button(0xFFE83CA8)
                .withBackgroundGradientColors(0xFFE83CA8, 0xFFC92588, 0xFFFF77C8, 0xFFE83CA8, 0xFFFF77C8, 0xFFE83CA8)
                .withTextColors(0xFFFFFFFF, 0xFF101018, 0xFFFFFFFF)
                .withTextShadowColors(0x00000000, 0x00000000, 0x00000000)
                .withBorderColors(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF)
                .withBoxMetrics(3, 2, 3, 3);
        AstralFancyButton.button(label, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, style).render(graphics, font, this.expanded, hovered);
    }

    public boolean mouseClicked(MouseButtonEvent event, int screenWidth, int screenHeight) {
        if (event.button() != 0) return false;
        double mx = event.x();
        double my = event.y();
        int toggleX = this.toggleX(screenWidth);
        int toggleY = this.toggleY(screenHeight);
        if (this.isInside(mx, my, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            this.expanded = !this.expanded;
            return true;
        }
        if (!this.expanded) return false;

        int listX = this.panelX(screenWidth) + 8;
        int listY = this.panelY() + 28;
        int listW = PANEL_WIDTH - 20;
        int listH = this.panelHeight(screenHeight) - 38;
        if (this.isScrollbarVisible(screenHeight) && mx >= listX + listW - SCROLLBAR_WIDTH
                && mx <= listX + listW && my >= listY && my <= listY + listH) {
            this.draggingScrollbar = true;
            this.dragStartY = my;
            this.dragStartScrollY = this.scrollY;
            return true;
        }

        if (mx >= listX && mx <= listX + listW - SCROLLBAR_WIDTH - 2 && my >= listY && my <= listY + listH) {
            int index = (int) ((my - listY + this.scrollY) / ROW_HEIGHT);
            if (index >= 0 && index < this.phrases.size()) {
                this.sendPhrase(this.phrases.get(index));
                this.expanded = false;
                return true;
            }
        }

        return this.isInside(mx, my, this.panelX(screenWidth), this.panelY(), PANEL_WIDTH, this.panelHeight(screenHeight));
    }

    public boolean mouseDragged(MouseButtonEvent event, int screenHeight) {
        if (!this.draggingScrollbar) return false;
        int listH = this.panelHeight(screenHeight) - 38;
        int thumbH = this.scrollbarThumbHeight(screenHeight, listH);
        int movable = Math.max(1, listH - thumbH);
        this.scrollY = Mth.clamp(this.dragStartScrollY + (float) ((event.y() - this.dragStartY) / movable * this.maxScroll(screenHeight)), 0.0F, this.maxScroll(screenHeight));
        return true;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY, int screenWidth, int screenHeight) {
        if (!this.expanded) return false;
        if (this.isInside(mouseX, mouseY, this.panelX(screenWidth), this.panelY(), PANEL_WIDTH, this.panelHeight(screenHeight))) {
            this.scrollY = Mth.clamp(this.scrollY - (float) deltaY * 24.0F, 0.0F, this.maxScroll(screenHeight));
            return true;
        }
        return false;
    }

    protected void sendPhrase(Component phrase) {
        String text = phrase.getString();
        if (text.isBlank()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.connection.sendChat(text);
        }
    }

    protected Component ellipsize(Font font, Component input, int maxWidth) {
        String text = input.getString();
        if (font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(out.toString()) + font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }
        return Component.literal(out + suffix);
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    protected int panelX(int screenWidth) { return screenWidth - PANEL_MARGIN_RIGHT - PANEL_WIDTH; }
    protected int panelY() { return PANEL_MARGIN_TOP; }
    protected int panelHeight(int screenHeight) { return Math.max(120, screenHeight - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM); }
    protected int toggleX(int screenWidth) { return screenWidth - PANEL_MARGIN_RIGHT - TOGGLE_WIDTH; }
    protected int toggleY(int screenHeight) { return Math.max(28, screenHeight - 68); }
    protected float maxScroll(int screenHeight) { return Math.max(0, this.phrases.size() * ROW_HEIGHT - (this.panelHeight(screenHeight) - 38)); }
    protected void clampScroll(int screenHeight) { this.scrollY = Mth.clamp(this.scrollY, 0.0F, this.maxScroll(screenHeight)); }
    protected boolean isScrollbarVisible(int screenHeight) { return this.maxScroll(screenHeight) > 0.0F; }
    protected int scrollbarThumbHeight(int screenHeight, int listH) { return Mth.clamp(Math.round(listH * (listH / (float) Math.max(listH, this.phrases.size() * ROW_HEIGHT))), 18, listH); }

    protected void renderScrollbar(GuiGraphicsExtractor graphics, int screenHeight, int x, int y, int h) {
        if (!this.isScrollbarVisible(screenHeight)) return;
        int thumbH = this.scrollbarThumbHeight(screenHeight, h);
        int thumbY = y + Math.round((h - thumbH) * (this.scrollY / this.maxScroll(screenHeight)));
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + h, 0x66000000);
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, 0xCCFFFFFF);
    }

    protected boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

}