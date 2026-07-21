package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.network.s2c.CloseBoardHospitalPayload;
import com.astral_craft.common.network.s2c.OpenBoardHospitalPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class BoardHospitalScreen extends Screen {

    private final UUID boardId;
    private OpenBoardHospitalPayload.Phase phase;
    private OpenBoardHospitalPayload.Result result;
    private int timeoutTicks;
    private int timeoutDurationTicks;

    public BoardHospitalScreen(OpenBoardHospitalPayload payload) {
        super(Component.translatable("gui.astral_craft.board.hospital.title"));
        this.boardId = payload.boardId();
        this.update(payload);
    }

    public static void open(OpenBoardHospitalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardHospitalScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.update(payload);
            } else {
                minecraft.setScreen(new BoardHospitalScreen(payload));
            }
        });
    }

    public static void close(CloseBoardHospitalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> closePresentation(payload.boardId()));
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardHospitalScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    private void update(OpenBoardHospitalPayload payload) {
        this.phase = payload.phase();
        this.result = payload.result();
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = payload.timeoutDurationTicks();
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
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(440, this.width - 24);
        int panelHeight = 190;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF0131822);
        graphics.fill(x, y, x + panelWidth, y + 2, 0xFFE7F8FF);
        graphics.centeredText(this.font, this.title, this.width / 2, y + 16, 0xFFFFFFFF);
        Component message = Component.translatable(this.phase == OpenBoardHospitalPayload.Phase.CHECKING
                ? "gui.astral_craft.board.hospital.checking"
                : this.result == OpenBoardHospitalPayload.Result.INJECTION
                ? "gui.astral_craft.board.hospital.injection_result"
                : "gui.astral_craft.board.hospital.stay_result");
        graphics.centeredText(this.font, message, this.width / 2, y + 44, 0xFFE9EEF4);

        int buttonWidth = 140;
        int buttonHeight = 38;
        int gap = 24;
        int leftX = this.width / 2 - buttonWidth - gap / 2;
        int rightX = this.width / 2 + gap / 2;
        int buttonY = y + 90;
        int highlighted = this.highlightedButton();
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.hospital.injection"),
                leftX, buttonY, buttonWidth, buttonHeight, highlighted == 0, false, 0xFF3F9CC7);
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.hospital.stay"),
                rightX, buttonY, buttonWidth, buttonHeight, highlighted == 1, false, 0xFFC75E80);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.hospital.read_only"),
                this.width / 2, y + 148, 0xFF9FA9B8);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int highlightedButton() {
        int finalButton = this.result == OpenBoardHospitalPayload.Result.INJECTION ? 0 : 1;
        if (this.phase == OpenBoardHospitalPayload.Phase.RESULT) return finalButton;
        float progress = 1.0F - this.timeoutTicks / (float) Math.max(1, this.timeoutDurationTicks);
        progress = Math.clamp(progress, 0.0F, 1.0F);
        int totalSteps = 18 + finalButton;
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        int step = Math.min(totalSteps, (int) Math.floor(totalSteps * eased));
        return step % 2;
    }
}
