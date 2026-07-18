package com.astral_craft.client.gui.cardback;

import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import com.astral_craft.common.network.c2s.CardBackSelectionPayload;
import com.astral_craft.common.network.s2c.OpenCardBackSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CardBackSelectionScreen extends Screen {

    protected static final int PANEL_WIDTH = 310;
    protected static final int PANEL_HEIGHT = 230;
    protected static final int CARD_W = 72;
    protected static final int CARD_H = 104;
    protected static final int CARD_GAP = 12;

    protected final List<CardBackDefinition> definitions;
    protected Identifier selected;
    protected float scrollX;
    protected boolean draggingScrollbar;
    protected double dragStartX;
    protected float dragStartScrollX;

    public CardBackSelectionScreen(List<CardBackDefinition> definitions, Identifier selected) {
        super(Component.translatable("gui.astral_craft.card_back_selection.title"));
        this.definitions = definitions;
        this.selected = selected;
    }

    public static void open(OpenCardBackSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Identifier selected = payload.selectedId();
            Minecraft.getInstance().setScreen(new CardBackSelectionScreen(payload.options(), selected));
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.clampScroll();
        int x = this.panelX();
        int y = this.panelY();
        int right = x + PANEL_WIDTH;
        int bottom = y + PANEL_HEIGHT;
        graphics.fill(x, y, right, bottom, 0xC00A0A12);
        graphics.fill(x, y, right, y + 1, 0x80FFFFFF);
        graphics.text(this.font, this.title, x + 12, y + 10, 0xFFFFFFFF, false);
        int listX = x + 14;
        int listY = y + 38;
        int listW = PANEL_WIDTH - 28;
        int listH = 142;
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < this.definitions.size(); i++) {
            int cardX = listX + i * (CARD_W + CARD_GAP) - Math.round(this.scrollX);
            int cardY = listY + 6;
            if (cardX + CARD_W < listX || cardX > listX + listW) continue;
            CardBackDefinition definition = this.definitions.get(i);
            boolean hovered = mouseX >= cardX && mouseX <= cardX + CARD_W && mouseY >= cardY && mouseY <= cardY + CARD_H + 22;
            boolean isSelected = definition.id().equals(this.selected);
            graphics.fill(cardX - 3, cardY - 3, cardX + CARD_W + 3, cardY + CARD_H + 23, isSelected ? 0xAAFFF0A0 : hovered ? 0x665C5C72 : 0x44202028);
            graphics.blit(RenderPipelines.GUI_TEXTURED, definition.texture(), cardX, cardY, 0.0F, 0.0F, CARD_W, CARD_H, 256, 360, 256, 360, 0xFFFFFFFF);
            Component name = Component.translatable(definition.nameKey());
            graphics.text(this.font, this.ellipsize(name, CARD_W + 4), cardX + CARD_W / 2 - this.font.width(this.ellipsize(name, CARD_W + 4)) / 2, cardY + CARD_H + 8, 0xFFFFFFFF, false);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics, listX, y + PANEL_HEIGHT - 36, listW);
        Component tip = Component.translatable("gui.astral_craft.card_back_selection.tip");
        graphics.text(this.font, tip, x + PANEL_WIDTH / 2 - this.font.width(tip) / 2, bottom - 18, 0xFFBFC8FF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();
        int listX = this.panelX() + 14;
        int listY = this.panelY() + 38;
        int listW = PANEL_WIDTH - 28;
        int barY = this.panelY() + PANEL_HEIGHT - 36;
        if (this.isScrollbarVisible() && mx >= listX && mx <= listX + listW && my >= barY && my <= barY + 6) {
            this.draggingScrollbar = true;
            this.dragStartX = mx;
            this.dragStartScrollX = this.scrollX;
            return true;
        }

        for (int i = 0; i < this.definitions.size(); i++) {
            int cardX = listX + i * (CARD_W + CARD_GAP) - Math.round(this.scrollX);
            int cardY = listY + 6;
            if (mx >= cardX && mx <= cardX + CARD_W && my >= cardY && my <= cardY + CARD_H + 22) {
                this.selected = this.definitions.get(i).id();
                ClientPacketDistributor.sendToServer(new CardBackSelectionPayload(this.selected));
                this.onClose();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            int listW = PANEL_WIDTH - 28;
            int thumbW = this.scrollbarThumbWidth(listW);
            int movable = Math.max(1, listW - thumbW);
            this.scrollX = Mth.clamp(this.dragStartScrollX + (float) ((event.x() - this.dragStartX) / movable * this.maxScroll()), 0.0F, this.maxScroll());
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
        if (mouseX >= this.panelX() && mouseX <= this.panelX() + PANEL_WIDTH && mouseY >= this.panelY() && mouseY <= this.panelY() + PANEL_HEIGHT) {
            this.scrollX = Mth.clamp(this.scrollX - (float) deltaY * 30.0F - (float) deltaX * 30.0F, 0.0F, this.maxScroll());
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

    protected Component ellipsize(Component input, int maxWidth) {
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

    protected int panelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    protected int panelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    protected float maxScroll() {
        return Math.max(0, this.definitions.size() * (CARD_W + CARD_GAP) - CARD_GAP - (PANEL_WIDTH - 28));
    }

    protected void clampScroll() {
        this.scrollX = Mth.clamp(this.scrollX, 0.0F, this.maxScroll());
    }

    protected boolean isScrollbarVisible() {
        return this.maxScroll() > 0.0F;
    }

    protected int scrollbarThumbWidth(int width) {
        int content = Math.max(width, this.definitions.size() * (CARD_W + CARD_GAP) - CARD_GAP);
        return Mth.clamp(Math.round(width * (width / (float) content)), 24, width);
    }

    protected void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int width) {
        if (!this.isScrollbarVisible()) return;
        int thumbW = this.scrollbarThumbWidth(width);
        int thumbX = x + Math.round((width - thumbW) * (this.scrollX / this.maxScroll()));
        graphics.fill(x, y, x + width, y + 6, 0x66000000);
        graphics.fill(thumbX, y, thumbX + thumbW, y + 6, 0xCCFFFFFF);
    }

}