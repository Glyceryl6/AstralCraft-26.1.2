package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.gameplay.board.BoardMatchmakingMode;
import com.astral_craft.common.network.c2s.BoardMatchmakingModeSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class BoardMatchmakingModeSelectionScreen extends Screen {

    private static final int WIDTH = 330;
    private static final int BUTTON_HEIGHT = 30;
    private final UUID boardId;
    private boolean tutorial;

    private BoardMatchmakingModeSelectionScreen(UUID boardId) {
        super(Component.translatable("gui.astral_craft.board.matchmaking.title"));
        this.boardId = boardId;
        BoardTutorialGuide.clear(boardId);
    }

    public static void open(OpenBoardMatchmakingModeSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardMatchmakingModeSelectionScreen(payload.boardId())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        AstralFancyButton.renderOutlinedBox(graphics, layout.x(), layout.y(), WIDTH, layout.height(),
                0xF0181822, 0xFFEEDFFF, 0xFF493D58, 1, 2);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 14, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.single_hint"),
                layout.x() + 16, layout.y() + 42, 0xFFD9D9E4, false);
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.multiplayer_hint"),
                layout.x() + 16, layout.y() + 58, this.tutorial ? 0xFF777782 : 0xFFD9D9E4, false);

        int checkboxX = layout.x() + 18;
        int checkboxY = layout.tutorialY();
        graphics.fill(checkboxX, checkboxY, checkboxX + 12, checkboxY + 12, 0xFFEAEAF2);
        graphics.fill(checkboxX + 1, checkboxY + 1, checkboxX + 11, checkboxY + 11, 0xFF161620);
        if (this.tutorial) {
            graphics.fill(checkboxX + 3, checkboxY + 5, checkboxX + 6, checkboxY + 9, 0xFFFFD85A);
            graphics.fill(checkboxX + 5, checkboxY + 7, checkboxX + 10, checkboxY + 9, 0xFFFFD85A);
        }
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.tutorial"),
                checkboxX + 18, checkboxY + 2, 0xFFFFFFFF, false);
        if (this.tutorial) {
            graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.tutorial_single_only"),
                    checkboxX + 18, checkboxY + 18, 0xFFFFD87A, false);
        }

        boolean singleHover = inside(mouseX, mouseY, layout.x() + 16, layout.singleY(), WIDTH - 32, BUTTON_HEIGHT);
        boolean multiHover = !this.tutorial && inside(mouseX, mouseY, layout.x() + 16, layout.multiY(), WIDTH - 32, BUTTON_HEIGHT);
        boolean cancelHover = inside(mouseX, mouseY, layout.x() + 16, layout.cancelY(), WIDTH - 32, 25);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.matchmaking.single"),
                layout.x() + 16, layout.singleY(), WIDTH - 32, BUTTON_HEIGHT, false, singleHover,
                ButtonStyle.button(0xFFB83C82));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.matchmaking.multiplayer"),
                layout.x() + 16, layout.multiY(), WIDTH - 32, BUTTON_HEIGHT, false, multiHover,
                ButtonStyle.button(this.tutorial ? 0xFF555560 : 0xFF4B72B6));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.cancel"),
                layout.x() + 16, layout.cancelY(), WIDTH - 32, 25, false, cancelHover,
                ButtonStyle.button(0xFF5B5267));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.x() + 16, layout.tutorialY() - 3, WIDTH - 32, 35)) {
            this.tutorial = !this.tutorial;
            return true;
        }
        if (inside(event.x(), event.y(), layout.x() + 16, layout.singleY(), WIDTH - 32, BUTTON_HEIGHT)) {
            this.submit(BoardMatchmakingMode.SINGLE_PLAYER);
            return true;
        }
        if (!this.tutorial && inside(event.x(), event.y(), layout.x() + 16, layout.multiY(), WIDTH - 32, BUTTON_HEIGHT)) {
            this.submit(BoardMatchmakingMode.MULTIPLAYER);
            return true;
        }
        if (inside(event.x(), event.y(), layout.x() + 16, layout.cancelY(), WIDTH - 32, 25)) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void submit(BoardMatchmakingMode mode) {
        if (this.tutorial) BoardTutorialGuide.start(this.boardId);
        else BoardTutorialGuide.clear(this.boardId);
        ClientPacketDistributor.sendToServer(new BoardMatchmakingModeSelectionPayload(this.boardId, mode, this.tutorial));
        this.onClose();
    }

    private Layout layout() {
        int height = 238;
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - height) / 2;
        return new Layout(x, y, height, y + 82, y + 121, y + 158, y + 199);
    }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private record Layout(int x, int y, int height, int tutorialY, int singleY, int multiY, int cancelY) {}
}
