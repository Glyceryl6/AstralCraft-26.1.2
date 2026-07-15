package com.astral_craft.client.gui;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.network.c2s.CardNumberSelectionPayload;
import com.astral_craft.common.network.s2c.OpenCardNumberSelectionPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.text.AstralTextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CardNumberSelectionScreen extends Screen {

    private static final int OPTION_SIZE = 44;
    private static final int OPTION_GAP = 8;
    private static final int PANEL_PADDING = 16;
    private static final int PANEL_HEIGHT = 126;

    private final OpenCardNumberSelectionPayload payload;

    public CardNumberSelectionScreen(OpenCardNumberSelectionPayload payload) {
        super(payload.cardStack().getHoverName());
        this.payload = payload;
    }

    public static void open(OpenCardNumberSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new CardNumberSelectionScreen(payload)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelX = this.panelX();
        int panelY = this.panelY();
        int panelWidth = this.panelWidth();
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xC0101018);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xA0FFFFFF);
        graphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xA0000000);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2,
                panelY + 10, 0xFFFFFFFF, true);
        CardDefinition definition = this.payload.cardStack().getOrDefault(
                AstralDataComponents.CARD_DEFINITION, CardDefinition.fallback());
        Component hint = AstralTextFormatter.format(definition.effectText(this.payload.cardStack()));
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2,
                panelY + 27, 0xFFD0D0D0, false);
        for (int value = this.payload.minValue(); value <= this.payload.maxValue(); value++) {
            int x = this.optionX(value);
            int y = panelY + 50;
            boolean hovered = mouseX >= x && mouseX < x + OPTION_SIZE && mouseY >= y && mouseY < y + OPTION_SIZE;
            int background = hovered ? 0xDD4B3A78 : 0xCC262634;
            int border = hovered ? 0xFFFFD5FF : 0xFF8D829E;
            graphics.fill(x, y, x + OPTION_SIZE, y + OPTION_SIZE, background);
            graphics.fill(x, y, x + OPTION_SIZE, y + 1, border);
            graphics.fill(x, y + OPTION_SIZE - 1, x + OPTION_SIZE, y + OPTION_SIZE, border);
            graphics.fill(x, y, x + 1, y + OPTION_SIZE, border);
            graphics.fill(x + OPTION_SIZE - 1, y, x + OPTION_SIZE, y + OPTION_SIZE, border);
            AstralFancyButton.drawCentered(graphics, this.font, Component.literal(Integer.toString(value)).withStyle(ChatFormatting.BOLD),
                    x, y, OPTION_SIZE, OPTION_SIZE, 0xFFFFFFFF, 0xA0000000, 3.0F);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int y = this.panelY() + 50;
            for (int value = this.payload.minValue(); value <= this.payload.maxValue(); value++) {
                int x = this.optionX(value);
                if (event.x() >= x && event.x() < x + OPTION_SIZE && event.y() >= y && event.y() < y + OPTION_SIZE) {
                    ClientPacketDistributor.sendToServer(new CardNumberSelectionPayload(this.payload.cardStack(), value));
                    this.onClose();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private int optionCount() {
        return Math.max(1, this.payload.maxValue() - this.payload.minValue() + 1);
    }

    private int panelWidth() {
        return PANEL_PADDING * 2 + this.optionCount() * OPTION_SIZE + Math.max(0, this.optionCount() - 1) * OPTION_GAP;
    }

    private int panelX() {
        return (this.width - this.panelWidth()) / 2;
    }

    private int panelY() {
        return Math.max(12, (this.height - PANEL_HEIGHT) / 2);
    }

    private int optionX(int value) {
        return this.panelX() + PANEL_PADDING + (value - this.payload.minValue()) * (OPTION_SIZE + OPTION_GAP);
    }

}