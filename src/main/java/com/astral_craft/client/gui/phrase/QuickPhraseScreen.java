package com.astral_craft.client.gui.phrase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Right-side quick phrase panel. Fill the phrase translation keys in lang_extra later. */
public class QuickPhraseScreen extends Screen {

    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_MARGIN_RIGHT = 10;
    private static final int PANEL_MARGIN_TOP = 32;
    private static final int PANEL_MARGIN_BOTTOM = 38;
    private static final int ROW_HEIGHT = 22;
    private static final int SCROLLBAR_WIDTH = 5;

    private final List<Component> phrases = new ArrayList<>();
    private float scrollY;
    private boolean draggingScrollbar;
    private double dragStartY;
    private float dragStartScrollY;

    public QuickPhraseScreen() {
        super(Component.translatable("gui.astral_craft.quick_phrases.title"));
        for (int i = 0; i < 24; i++) {
            this.phrases.add(Component.translatable("quick_phrase.astral_craft." + i));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Do not darken the world. This panel is intended to behave like the original side drawer.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.clampScroll();
        int x = panelX();
        int y = panelY();
        int h = panelHeight();
        int right = x + PANEL_WIDTH;
        int bottom = y + h;
        graphics.fill(x, y, right, bottom, 0xB00B0B14);
        graphics.fill(x, y, right, y + 1, 0x80FFFFFF);
        graphics.fill(x, bottom - 1, right, bottom, 0x90000000);
        graphics.fill(x, y, x + 1, bottom, 0x60FFFFFF);
        graphics.fill(right - 1, y, right, bottom, 0x70000000);
        graphics.text(this.font, this.title, x + 10, y + 8, 0xFFFFFFFF, true);
        int listX = x + 8;
        int listY = y + 28;
        int listW = PANEL_WIDTH - 20;
        int listH = h - 38;
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < phrases.size(); i++) {
            int rowY = listY + i * ROW_HEIGHT - Math.round(scrollY);
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listH) continue;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW - SCROLLBAR_WIDTH - 2 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 2;
            graphics.fill(listX, rowY, listX + listW - SCROLLBAR_WIDTH - 2, rowY + ROW_HEIGHT - 2, hovered ? 0x663A3A48 : 0x3A202028);
            graphics.text(this.font, ellipsize(phrases.get(i), listW - 18), listX + 6, rowY + 7, 0xFFEFEFFF, false);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics, listX + listW - SCROLLBAR_WIDTH, listY, listH);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int listX = panelX() + 8;
        int listY = panelY() + 28;
        int listW = PANEL_WIDTH - 20;
        int listH = panelHeight() - 38;
        if (this.isScrollbarVisible() && mx >= listX + listW - SCROLLBAR_WIDTH
                && mx <= listX + listW && my >= listY && my <= listY + listH) {
            this.draggingScrollbar = true;
            this.dragStartY = my;
            this.dragStartScrollY = this.scrollY;
            return true;
        }

        if (mx >= listX && mx <= listX + listW - SCROLLBAR_WIDTH - 2 && my >= listY && my <= listY + listH) {
            int index = (int) ((my - listY + scrollY) / ROW_HEIGHT);
            if (index >= 0 && index < phrases.size()) {
                this.sendPhrase(this.phrases.get(index));
                this.onClose();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            int listH = this.panelHeight() - 38;
            int thumbH = this.scrollbarThumbHeight(listH);
            int movable = Math.max(1, listH - thumbH);
            this.scrollY = Mth.clamp(
                    this.dragStartScrollY + (float) ((event.y() - this.dragStartY)
                            / movable * this.maxScroll()), 0.0F, this.maxScroll());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= panelX() && mouseX <= panelX() + PANEL_WIDTH && mouseY >= panelY() && mouseY <= panelY() + this.panelHeight()) {
            this.scrollY = Mth.clamp(this.scrollY - (float) deltaY * 24.0F, 0.0F, this.maxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    private void sendPhrase(Component phrase) {
        String text = phrase.getString();
        if (text.isBlank()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.connection.sendChat(text);
        }
    }

    private Component ellipsize(Component input, int maxWidth) {
        String text = input.getString();
        if (this.font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (this.font.width(out.toString()) + this.font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }

        return Component.literal(out + suffix);
    }

    private int panelX() { return this.width - PANEL_MARGIN_RIGHT - PANEL_WIDTH; }
    private int panelY() { return PANEL_MARGIN_TOP; }
    private int panelHeight() { return Math.max(120, this.height - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM); }
    private float maxScroll() { return Math.max(0, this.phrases.size() * ROW_HEIGHT - (panelHeight() - 38)); }
    private void clampScroll() { this.scrollY = Mth.clamp(this.scrollY, 0.0F, maxScroll()); }
    private boolean isScrollbarVisible() { return maxScroll() > 0.0F; }
    private int scrollbarThumbHeight(int listH) { return Mth.clamp(Math.round(listH * (listH / (float) Math.max(listH, phrases.size() * ROW_HEIGHT))), 18, listH); }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int h) {
        if (!this.isScrollbarVisible()) return;
        int thumbH = this.scrollbarThumbHeight(h);
        int thumbY = y + Math.round((h - thumbH) * (this.scrollY / this.maxScroll()));
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + h, 0x66000000);
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, 0xCCFFFFFF);
    }

}