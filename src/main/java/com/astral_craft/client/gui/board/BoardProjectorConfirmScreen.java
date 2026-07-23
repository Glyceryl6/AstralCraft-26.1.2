package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.network.c2s.BoardProjectorConfirmPayload;
import com.astral_craft.common.network.s2c.OpenBoardProjectorConfirmPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public class BoardProjectorConfirmScreen extends Screen {

    private final OpenBoardProjectorConfirmPayload payload;
    private boolean submitted;

    public BoardProjectorConfirmScreen(OpenBoardProjectorConfirmPayload payload) {
        super(Component.translatable("gui.astral_craft.board_projector.confirm.title"));
        this.payload = payload;
    }

    public static void open(OpenBoardProjectorConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardProjectorConfirmScreen(payload)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF0131822);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xFFE7F8FF);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 18, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board_projector.confirm.warning"),
                this.width / 2, layout.y() + 48, 0xFFFFD27A);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board_projector.confirm.details",
                        this.payload.panelCount(), this.payload.width(), this.payload.depth()),
                this.width / 2, layout.y() + 68, 0xFFD7E4F2);
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board_projector.confirm.create"),
                layout.confirmX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight(), this.submitted,
                inside(mouseX, mouseY, layout.confirmX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight()),
                ButtonStyle.button(0xFF4F9D69));
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board_projector.confirm.cancel"),
                layout.cancelX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight(), this.submitted,
                inside(mouseX, mouseY, layout.cancelX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight()),
                ButtonStyle.button(0xFF9B5360));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.confirmX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight())) {
            this.submitted = true;
            ClientPacketDistributor.sendToServer(new BoardProjectorConfirmPayload(
                    this.payload.groundPos(), this.payload.facing(), this.payload.offhand()));
            this.onClose();
            return true;
        }

        if (inside(event.x(), event.y(), layout.cancelX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight())) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private Layout layout() {
        int panelWidth = Math.min(430, this.width - 24);
        int panelHeight = 150;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        int buttonWidth = Math.min(150, (panelWidth - 50) / 2);
        return new Layout(x, y, panelWidth, panelHeight, buttonWidth, 34, y + 102);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Layout(int x, int y, int width, int height, int buttonWidth, int buttonHeight, int buttonY) {
        private int confirmX() { return this.x + 16; }
        private int cancelX() { return this.x + this.width - this.buttonWidth - 16; }
    }

}