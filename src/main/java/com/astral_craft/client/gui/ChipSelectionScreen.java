package com.astral_craft.client.gui;

import com.astral_craft.client.gui.board.BoardDecisionProgressBar;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
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
import java.util.UUID;

/** Three-option chip selector. Exactly one option must be selected before confirmation. */
public class ChipSelectionScreen extends Screen {

    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 176;
    private static final int CARD_GAP = 16;
    private static final int ICON_SIZE = 54;
    private final UUID boardId;
    private final List<OpenChipSelectionPayload.Choice> choices;
    private int timeoutTicks;
    private final int timeoutDurationTicks;
    private int selectedIndex = -1;
    private boolean submitted;

    public ChipSelectionScreen(OpenChipSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.chip_selection.title"));
        this.boardId = payload.boardId();
        this.choices = payload.choices();
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = payload.timeoutDurationTicks();
    }

    public static void open(OpenChipSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ChipSelectionScreen(payload)));
    }

    public static void closePresentation(UUID boardId) {
        Screen current = Minecraft.getInstance().screen;
        if (current instanceof ChipSelectionScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.boardSelection() && this.submitted;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(),
                layout.panelY() + layout.panelHeight(), 0xF0101018);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(), layout.panelY() + 2, 0x80FFFFFF);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, layout.panelY() + 12, 0xFFFFFFFF, true);
        Component hint = Component.translatable(this.selectedIndex < 0
                ? "gui.astral_craft.chip_selection.hint" : "gui.astral_craft.chip_selection.selected_hint");
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, layout.panelY() + 29, 0xFFE0E0E0, false);
        for (int index = 0; index < this.choices.size(); index++) {
            renderChoice(graphics, this.font, this.choices.get(index), layout.cardX(index), layout.cardY(),
                    mouseX, mouseY, this.selectedIndex == index);
        }
        boolean canConfirm = this.selectedIndex >= 0 && !this.submitted;
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.chip_selection.confirm"), layout.confirmX(), layout.confirmY(),
                layout.confirmWidth(), 30, false,
                canConfirm && inside(mouseX, mouseY, layout.confirmX(), layout.confirmY(), layout.confirmWidth(), 30),
                canConfirm ? ButtonStyle.button(0xFF56A85B) : AstralFancyButton.disabledButtonStyle());
        if (this.boardSelection()) BoardDecisionProgressBar.render(graphics, this.font, null, null, this.timeoutTicks,
                this.timeoutDurationTicks, this.width / 2, layout.panelY() + layout.panelHeight() - 13,
                Math.min(260, layout.panelWidth() - 50));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || this.submitted) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        for (int index = 0; index < this.choices.size(); index++) {
            if (inside(event.x(), event.y(), layout.cardX(index), layout.cardY(), CARD_WIDTH, CARD_HEIGHT)) {
                this.selectedIndex = index;
                return true;
            }
        }
        if (this.selectedIndex >= 0 && inside(event.x(), event.y(), layout.confirmX(), layout.confirmY(), layout.confirmWidth(), 30)) {
            this.submitted = true;
            ClientPacketDistributor.sendToServer(new ChipSelectionPayload(this.boardId, this.choices.get(this.selectedIndex).id()));
            if (!this.boardSelection()) this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean boardSelection() {
        return !OpenChipSelectionPayload.NO_BOARD.equals(this.boardId);
    }

    private Layout layout() {
        int totalWidth = this.choices.size() * CARD_WIDTH + Math.max(0, this.choices.size() - 1) * CARD_GAP;
        int panelWidth = Math.max(560, totalWidth + 46);
        int panelHeight = 286;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = Math.max(10, (this.height - panelHeight) / 2);
        return new Layout(panelX, panelY, panelWidth, panelHeight, this.width / 2 - totalWidth / 2,
                panelY + 52, this.width / 2 - 88, panelY + 236, 176);
    }

    private static void renderChoice(GuiGraphicsExtractor graphics, Font font, OpenChipSelectionPayload.Choice choice,
                                     int x, int y, int mouseX, int mouseY, boolean selected) {
        boolean hovered = inside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
        int background = selected ? 0xCC294435 : hovered ? 0xCC303038 : 0xAA202028;
        int border = selected ? 0xFF72FF9A : hovered ? 0xFFFFD66B : 0xFF808080;
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, background);
        graphics.fill(x, y, x + CARD_WIDTH, y + 2, border);
        graphics.fill(x, y + CARD_HEIGHT - 2, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        graphics.fill(x, y, x + 2, y + CARD_HEIGHT, border);
        graphics.fill(x + CARD_WIDTH - 2, y, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        int iconX = x + CARD_WIDTH / 2 - ICON_SIZE / 2;
        int iconY = y + 12;
        graphics.fill(iconX - 4, iconY - 4, iconX + ICON_SIZE + 4, iconY + ICON_SIZE + 4, 0x66000000);
        graphics.blit(RenderPipelines.GUI_TEXTURED, choice.icon(), iconX, iconY, 0, 0,
                ICON_SIZE, ICON_SIZE, 64, 64, 64, 64, 0xFFFFFFFF);
        Component name = Component.translatable(choice.nameKey());
        graphics.text(font, name, x + CARD_WIDTH / 2 - font.width(name) / 2, y + 76, 0xFFFFF0B0, true);
        List<FormattedCharSequence> lines = font.split(Component.translatable(choice.effectKey()), CARD_WIDTH - 18);
        int lineY = y + 96;
        for (int index = 0; index < Math.min(6, lines.size()); index++) {
            graphics.text(font, lines.get(index), x + 9, lineY, 0xFFE0E0E0, false);
            lineY += 10;
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record Layout(int panelX, int panelY, int panelWidth, int panelHeight, int cardsX, int cardY,
                          int confirmX, int confirmY, int confirmWidth) {
        private int cardX(int index) {
            return this.cardsX + index * (CARD_WIDTH + CARD_GAP);
        }
    }

}