package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.network.c2s.BoardLotteryNumberPayload;
import com.astral_craft.common.network.s2c.OpenBoardLotteryNumberPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BoardLotteryNumberScreen extends Screen {

    private final UUID boardId;
    private final Set<Integer> selectedNumbers;
    private final int timeoutDurationTicks;
    private final Identifier characterId;
    private final Identifier skinId;
    private int timeoutTicks;
    private int pendingNumber;
    private boolean submitted;

    public BoardLotteryNumberScreen(OpenBoardLotteryNumberPayload payload) {
        super(Component.translatable("gui.astral_craft.board.lottery.title"));
        this.boardId = payload.boardId();
        this.selectedNumbers = new HashSet<>(payload.selectedNumbers());
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = payload.timeoutDurationTicks();
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
    }

    public static void open(OpenBoardLotteryNumberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardLotteryNumberScreen(payload)));
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardLotteryNumberScreen screen && screen.boardId.equals(boardId)) screen.onClose();
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
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xED10111B);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xD8FFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 12, 0xFFFFFFFF);
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                this.timeoutTicks, this.timeoutDurationTicks, this.width / 2, layout.y() + 31, layout.width() - 54);
        for (int number = 1; number <= 12; number++) {
            Cell cell = layout.cell(number);
            boolean unavailable = this.selectedNumbers.contains(number);
            boolean active = this.pendingNumber == number;
            int color = active ? 0xFFDB9D35 : unavailable ? 0xFF3B3C48 : 0xFF477EAB;
            AstralFancyButton.renderButton(graphics, this.font, Component.literal(Integer.toString(number)),
                    cell.x(), cell.y(), cell.width(), cell.height(), unavailable || this.submitted,
                    inside(mouseX, mouseY, cell.x(), cell.y(), cell.width(), cell.height()), ButtonStyle.button(color));
        }

        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.lottery.confirm"),
                layout.confirmX(), layout.confirmY(), layout.confirmWidth(), layout.confirmHeight(),
                this.pendingNumber == 0 || this.submitted,
                inside(mouseX, mouseY, layout.confirmX(), layout.confirmY(), layout.confirmWidth(), layout.confirmHeight()),
                ButtonStyle.button(0xFF59A05D));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        for (int number = 1; number <= 12; number++) {
            Cell cell = layout.cell(number);
            if (!this.selectedNumbers.contains(number)
                    && inside(event.x(), event.y(), cell.x(), cell.y(), cell.width(), cell.height())) {
                this.pendingNumber = number;
                return true;
            }
        }

        if (this.pendingNumber != 0 && inside(event.x(), event.y(), layout.confirmX(), layout.confirmY(),
                layout.confirmWidth(), layout.confirmHeight())) {
            this.submitted = true;
            ClientPacketDistributor.sendToServer(new BoardLotteryNumberPayload(this.boardId, this.pendingNumber));
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private Layout layout() {
        int width = Math.min(390, this.width - 24);
        int height = 250;
        int x = (this.width - width) / 2;
        int y = Math.max(10, (this.height - height) / 2);
        return new Layout(x, y, width, height);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Cell(int x, int y, int width, int height) {}

    private record Layout(int x, int y, int width, int height) {

        private Cell cell(int number) {
            int index = Math.clamp(number, 1, 12) - 1;
            int gap = 8;
            int cellWidth = (this.width - 52 - gap * 3) / 4;
            int cellHeight = 38;
            int startX = this.x + 26;
            int startY = this.y + 54;
            return new Cell(startX + index % 4 * (cellWidth + gap),
                    startY + index / 4 * (cellHeight + gap), cellWidth, cellHeight);
        }

        private int confirmWidth() { return 150; }
        private int confirmHeight() { return 34; }
        private int confirmX() { return this.x + (this.width - this.confirmWidth()) / 2; }
        private int confirmY() { return this.y + this.height - 46; }

    }

}