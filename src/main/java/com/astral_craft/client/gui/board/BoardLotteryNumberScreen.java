package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.network.c2s.BoardLotteryNumberPayload;
import com.astral_craft.common.network.s2c.CloseBoardLotteryNumberPayload;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BoardLotteryNumberScreen extends Screen {

    private final UUID boardId;
    private final Set<Integer> selectedNumbers = new HashSet<>();
    private int timeoutDurationTicks;
    private Identifier characterId;
    private Identifier skinId;
    private boolean sharedEvent;
    private boolean localCanChoose;
    private List<OpenBoardLotteryNumberPayload.Entry> entries = List.of();
    private int timeoutTicks;
    private int pendingNumber;
    private boolean submitted;

    public BoardLotteryNumberScreen(OpenBoardLotteryNumberPayload payload) {
        super(Component.translatable("gui.astral_craft.board.lottery.title"));
        this.boardId = payload.boardId();
        this.update(payload);
    }

    public static void open(OpenBoardLotteryNumberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardLotteryNumberScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.update(payload);
            } else {
                minecraft.setScreen(new BoardLotteryNumberScreen(payload));
            }
        });
    }

    public static void close(CloseBoardLotteryNumberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> closePresentation(payload.boardId()));
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardLotteryNumberScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    private void update(OpenBoardLotteryNumberPayload payload) {
        this.selectedNumbers.clear();
        this.selectedNumbers.addAll(payload.selectedNumbers());
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = payload.timeoutDurationTicks();
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
        this.sharedEvent = payload.sharedEvent();
        this.localCanChoose = payload.localCanChoose();
        this.entries = payload.entries();
        this.submitted = this.sharedEvent && !this.localCanChoose;
        if (this.selectedNumbers.contains(this.pendingNumber)) this.pendingNumber = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.sharedEvent;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
        if (!this.sharedEvent && !this.submitted && this.timeoutTicks <= 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xED10111B);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xD8FFFFFF);
        graphics.centeredText(this.font, this.sharedEvent
                ? Component.translatable("gui.astral_craft.board.lottery.event_title") : this.title,
                this.width / 2, layout.y() + 12, 0xFFFFFFFF);
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                this.timeoutTicks, this.timeoutDurationTicks, layout.gridCenterX(), layout.y() + 31, layout.gridWidth() - 28);

        if (this.sharedEvent) this.renderEntries(graphics, layout);
        for (int number = 1; number <= 12; number++) {
            Cell cell = layout.cell(number);
            boolean unavailable = this.selectedNumbers.contains(number);
            boolean active = this.pendingNumber == number;
            boolean disabled = unavailable || this.submitted || !this.localCanChoose;
            int color = active ? 0xFFDB9D35 : unavailable ? 0xFF3B3C48 : 0xFF477EAB;
            AstralFancyButton.renderButton(graphics, this.font, Component.literal(Integer.toString(number)),
                    cell.x(), cell.y(), cell.width(), cell.height(), disabled,
                    inside(mouseX, mouseY, cell.x(), cell.y(), cell.width(), cell.height()), ButtonStyle.button(color));
        }

        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.lottery.confirm"),
                layout.confirmX(), layout.confirmY(), layout.confirmWidth(), layout.confirmHeight(),
                this.pendingNumber == 0 || this.submitted || !this.localCanChoose,
                inside(mouseX, mouseY, layout.confirmX(), layout.confirmY(), layout.confirmWidth(), layout.confirmHeight()),
                ButtonStyle.button(0xFF59A05D));
        if (this.sharedEvent && !this.localCanChoose) {
            graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.lottery.waiting"),
                    layout.gridCenterX(), layout.confirmY() + layout.confirmHeight() + 7, 0xFFBFC7D5);
        }
    }

    private void renderEntries(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.lottery.selection_status"),
                layout.listX(), layout.y() + 52, 0xFFFFD76A, true);
        for (int index = 0; index < Math.min(4, this.entries.size()); index++) {
            OpenBoardLotteryNumberPayload.Entry entry = this.entries.get(index);
            int rowY = layout.y() + 72 + index * 43;
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, entry.characterId(), entry.skinId().getPath(),
                    layout.listX(), rowY, 32, entry.chosen() ? 255 : 150);
            graphics.text(this.font, Component.literal(entry.name()), layout.listX() + 40, rowY + 5,
                    entry.chosen() ? 0xFFFFFFFF : 0xFFBFC7D5, true);
            graphics.text(this.font, Component.translatable(entry.chosen()
                            ? "gui.astral_craft.board.lottery.chosen" : "gui.astral_craft.board.lottery.choosing"),
                    layout.listX() + 40, rowY + 19, entry.chosen() ? 0xFF72D27B : 0xFFFFC85B, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || !this.localCanChoose || event.button() != 0) return super.mouseClicked(event, doubleClick);
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
            this.localCanChoose = false;
            ClientPacketDistributor.sendToServer(new BoardLotteryNumberPayload(this.boardId, this.pendingNumber));
            if (!this.sharedEvent) this.onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private Layout layout() {
        int width = Math.min(this.sharedEvent ? 650 : 390, this.width - 24);
        int height = 270;
        int x = (this.width - width) / 2;
        int y = Math.max(10, (this.height - height) / 2);
        return new Layout(x, y, width, height, this.sharedEvent);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Cell(int x, int y, int width, int height) {}

    private record Layout(int x, int y, int width, int height, boolean sharedEvent) {

        private int listX() { return this.x + 18; }
        private int listWidth() { return this.sharedEvent ? 220 : 0; }
        private int gridX() { return this.x + this.listWidth() + 20; }
        private int gridWidth() { return this.width - this.listWidth() - 40; }
        private int gridCenterX() { return this.gridX() + this.gridWidth() / 2; }

        private Cell cell(int number) {
            int index = Math.clamp(number, 1, 12) - 1;
            int gap = 8;
            int cellWidth = Math.min(72, (this.gridWidth() - gap * 3) / 4);
            int cellHeight = 38;
            int startX = this.gridCenterX() - (cellWidth * 4 + gap * 3) / 2;
            int startY = this.y + 62;
            return new Cell(startX + index % 4 * (cellWidth + gap),
                    startY + index / 4 * (cellHeight + gap), cellWidth, cellHeight);
        }

        private int confirmWidth() { return 150; }
        private int confirmHeight() { return 34; }
        private int confirmX() { return this.gridCenterX() - this.confirmWidth() / 2; }
        private int confirmY() { return this.y + this.height - 56; }
    }
}
