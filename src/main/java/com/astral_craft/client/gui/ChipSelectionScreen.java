package com.astral_craft.client.gui;

import com.astral_craft.common.network.c2s.ChipSelectionPayload;
import com.astral_craft.common.network.s2c.OpenChipSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Three-option chip selector. The player must pick one option; closing with ESC is disabled. */
public class ChipSelectionScreen extends Screen {

    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 176;
    private static final int CARD_GAP = 16;
    private static final int ICON_SIZE = 54;

    private final List<OpenChipSelectionPayload.Choice> choices;
    private boolean confirmed;

    public ChipSelectionScreen(OpenChipSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.chip_selection.title"));
        this.choices = payload.choices();
    }

    public static void open(OpenChipSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ChipSelectionScreen(payload)));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.confirmed) {
            super.onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible behind the mandatory chip selector.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int totalW = this.choices.size() * CARD_WIDTH + Math.max(0, this.choices.size() - 1) * CARD_GAP;
        int panelW = Math.max(560, totalW + 46);
        int panelH = 238;
        int panelX = (this.width - panelW) / 2;
        int panelY = Math.max(14, (this.height - panelH) / 2);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101018);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0x80FFFFFF);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0x80000000);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, panelY + 12, 0xFFFFFFFF, true);
        Component hint = Component.translatable("gui.astral_craft.chip_selection.hint");
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, panelY + 29, 0xFFE0E0E0, false);
        int x = this.width / 2 - totalW / 2;
        int y = panelY + 52;
        for (int i = 0; i < this.choices.size(); i++) {
            renderChoice(graphics, this.font, this.choices.get(i), x + i * (CARD_WIDTH + CARD_GAP), y, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            OpenChipSelectionPayload.Choice choice = this.choiceAt(event.x(), event.y());
            if (choice != null) {
                this.confirmed = true;
                ClientPacketDistributor.sendToServer(new ChipSelectionPayload(choice.id()));
                super.onClose();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private OpenChipSelectionPayload.Choice choiceAt(double mouseX, double mouseY) {
        int totalW = this.choices.size() * CARD_WIDTH + Math.max(0, this.choices.size() - 1) * CARD_GAP;
        int x = this.width / 2 - totalW / 2;
        int panelY = Math.max(14, (this.height - 238) / 2);
        int y = panelY + 52;
        for (int i = 0; i < this.choices.size(); i++) {
            int cx = x + i * (CARD_WIDTH + CARD_GAP);
            if (mouseX >= cx && mouseX <= cx + CARD_WIDTH && mouseY >= y && mouseY <= y + CARD_HEIGHT) {
                return this.choices.get(i);
            }
        }

        return null;
    }

    private static void renderChoice(GuiGraphicsExtractor graphics, Font font, OpenChipSelectionPayload.Choice choice, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + CARD_WIDTH && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        int bg = hovered ? 0xCC303038 : 0xAA202028;
        int border = hovered ? 0xFFFFD66B : 0xFF808080;
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bg);
        graphics.fill(x, y, x + CARD_WIDTH, y + 1, border);
        graphics.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        graphics.fill(x, y, x + 1, y + CARD_HEIGHT, border);
        graphics.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        int iconX = x + CARD_WIDTH / 2 - ICON_SIZE / 2;
        int iconY = y + 12;
        graphics.fill(iconX - 4, iconY - 4, iconX + ICON_SIZE + 4, iconY + ICON_SIZE + 4, 0x66000000);
        graphics.blit(RenderPipelines.GUI_TEXTURED, choice.icon(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, 64, 64, 64, 64, 0xFFFFFFFF);
        Component name = Component.translatable(choice.nameKey());
        graphics.text(font, name, x + CARD_WIDTH / 2 - font.width(name) / 2, y + 76, 0xFFFFF0B0, true);
        List<FormattedCharSequence> lines = font.split(Component.translatable(choice.effectKey()), CARD_WIDTH - 18);
        int lineY = y + 96;
        for (int i = 0; i < Math.min(6, lines.size()); i++) {
            graphics.text(font, lines.get(i), x + 9, lineY, 0xFFE0E0E0, false);
            lineY += 10;
        }
    }

}