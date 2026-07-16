package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.network.c2s.BoardStartChoicePayload;
import com.astral_craft.common.network.s2c.OpenBoardStartChoicePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public class BoardStartChoiceScreen extends Screen {

    private final String boardId;
    private int timeoutTicks;
    private final int timeoutDurationTicks;
    private final Identifier characterId;
    private final Identifier skinId;
    private boolean submitted;

    public BoardStartChoiceScreen(OpenBoardStartChoicePayload payload) {
        super(Component.translatable("gui.astral_craft.board.start_choice"));
        this.boardId = payload.boardId();
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
        this.timeoutDurationTicks = Math.max(1, payload.timeoutDurationTicks());
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
    }

    public static void open(OpenBoardStartChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardStartChoiceScreen(payload)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.submitted && this.timeoutTicks > 0 && --this.timeoutTicks <= 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(),
                layout.y() + layout.height(), 0xE610111D);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xC8FFFFFF);
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                this.timeoutTicks, this.timeoutDurationTicks, layout.x() + layout.width() / 2,
                layout.y() + 18, Math.min(250, layout.width() - 54));
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.start_stop"), layout.stopX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight(), this.submitted,
                inside(mouseX, mouseY, layout.stopX(), layout.buttonY(),
                        layout.buttonWidth(), layout.buttonHeight()),
                ButtonStyle.button(0xFF56A85B));
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.start_continue"), layout.continueX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight(), this.submitted,
                inside(mouseX, mouseY, layout.continueX(), layout.buttonY(),
                        layout.buttonWidth(), layout.buttonHeight()),
                ButtonStyle.button(0xFF496AA5));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.stopX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight())) {
            this.choose(true);
            return true;
        }
        if (inside(event.x(), event.y(), layout.continueX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight())) {
            this.choose(false);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }


    private Layout layout() {
        int panelWidth = Math.min(370, this.width - 24);
        int panelHeight = 94;
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(8, this.height - panelHeight - 18);
        int buttonWidth = Math.min(150, (panelWidth - 46) / 2);
        int buttonHeight = 34;
        return new Layout(x, y, panelWidth, panelHeight, buttonWidth, buttonHeight, y + 48);
    }

    private void choose(boolean stop) {
        if (this.submitted) return;
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardStartChoicePayload(this.boardId, stop));
        this.onClose();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Layout(int x, int y, int width, int height,
                          int buttonWidth, int buttonHeight, int buttonY) {
        private int stopX() { return this.x + 14; }
        private int continueX() { return this.x + this.width - this.buttonWidth - 14; }
    }

}