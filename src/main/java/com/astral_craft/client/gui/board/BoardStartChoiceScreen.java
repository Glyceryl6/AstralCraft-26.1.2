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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public class BoardStartChoiceScreen extends Screen {

    private final String boardId;
    private final int health;
    private final int maximumHealth;
    private final int stars;
    private final int starCoins;
    private final int nextStarCost;
    private int timeoutTicks;
    private boolean submitted;

    public BoardStartChoiceScreen(OpenBoardStartChoicePayload payload) {
        super(Component.translatable("gui.astral_craft.board.start_choice"));
        this.boardId = payload.boardId();
        this.health = payload.health();
        this.maximumHealth = payload.maximumHealth();
        this.stars = payload.stars();
        this.starCoins = payload.starCoins();
        this.nextStarCost = payload.nextStarCost();
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
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
        if (!this.submitted && --this.timeoutTicks <= 0) {
            this.choose(false);
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(380, this.width - 28);
        int panelHeight = Math.min(205, this.height - 28);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xEE10111D);
        graphics.fill(x, y, x + panelWidth, y + 2, 0xD0FFFFFF);
        graphics.text(this.font, this.title, x + 16, y + 14, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.start_choice_stats",
                this.health, this.maximumHealth, this.starCoins, this.stars), x + 16, y + 39, 0xFFE9D9FF, false);
        Component benefit = this.nextStarCost > 0
                ? Component.translatable("gui.astral_craft.board.start_choice_benefit", this.nextStarCost)
                : Component.translatable("gui.astral_craft.board.start_choice_max_star");
        graphics.text(this.font, benefit, x + 16, y + 59, 0xFFFFD36B, false);
        int buttonWidth = Math.min(142, (panelWidth - 46) / 2);
        int buttonHeight = 38;
        int stopX = x + 16;
        int continueX = x + panelWidth - buttonWidth - 16;
        int buttonY = y + 102;
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.start_stop"), stopX, buttonY,
                buttonWidth, buttonHeight, this.submitted,
                inside(mouseX, mouseY, stopX, buttonY, buttonWidth, buttonHeight),
                ButtonStyle.button(0xFF56A85B));
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.start_continue"), continueX, buttonY,
                buttonWidth, buttonHeight, this.submitted,
                inside(mouseX, mouseY, continueX, buttonY, buttonWidth, buttonHeight),
                ButtonStyle.button(0xFF496AA5));
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.timeout",
                (this.timeoutTicks + 19) / 20), x + 16, y + panelHeight - 24, 0xFFBFC8FF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        int panelWidth = Math.min(380, this.width - 28);
        int panelHeight = Math.min(205, this.height - 28);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        int buttonWidth = Math.min(142, (panelWidth - 46) / 2);
        int buttonHeight = 38;
        int stopX = x + 16;
        int continueX = x + panelWidth - buttonWidth - 16;
        int buttonY = y + 102;
        if (inside(event.x(), event.y(), stopX, buttonY, buttonWidth, buttonHeight)) {
            this.choose(true);
            return true;
        }
        if (inside(event.x(), event.y(), continueX, buttonY, buttonWidth, buttonHeight)) {
            this.choose(false);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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

}