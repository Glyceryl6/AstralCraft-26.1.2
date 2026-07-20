package com.astral_craft.client.gui.board;

import com.astral_craft.common.network.s2c.CloseBoardLotteryDrawPayload;
import com.astral_craft.common.network.s2c.OpenBoardLotteryDrawPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public class BoardLotteryDrawScreen extends Screen {

    private final UUID boardId;
    private OpenBoardLotteryDrawPayload.Phase phase;
    private int finalNumber;
    private int jackpot;
    private List<String> winnerNames;
    private int awardEach;
    private int timeoutTicks;
    private int timeoutDurationTicks;

    public BoardLotteryDrawScreen(OpenBoardLotteryDrawPayload payload) {
        super(Component.translatable("gui.astral_craft.board.lottery_draw.title"));
        this.boardId = payload.boardId();
        this.update(payload);
    }

    public static void open(OpenBoardLotteryDrawPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardLotteryDrawScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.update(payload);
            } else {
                minecraft.setScreen(new BoardLotteryDrawScreen(payload));
            }
        });
    }

    public static void close(CloseBoardLotteryDrawPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardLotteryDrawScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.onClose();
            }
        });
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardLotteryDrawScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    private void update(OpenBoardLotteryDrawPayload payload) {
        this.phase = payload.phase();
        this.finalNumber = payload.finalNumber();
        this.jackpot = payload.jackpot();
        this.winnerNames = payload.winnerNames();
        this.awardEach = payload.awardEach();
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = payload.timeoutDurationTicks();
    }

    @Override
    public boolean isPauseScreen() {
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
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF010111B);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xD8FFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 14, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.lottery_draw.jackpot", this.jackpot),
                this.width / 2, layout.y() + 34, 0xFFFFD76A);
        int active = this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT ? this.finalNumber : this.animatedNumber();
        for (int number = 1; number <= 12; number++) {
            Cell cell = layout.cell(number);
            boolean highlighted = number == active;
            graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + cell.height(),
                    highlighted ? 0xFFDB9D35 : 0xFF303442);
            graphics.fill(cell.x() + 2, cell.y() + 2, cell.x() + cell.width() - 2, cell.y() + cell.height() - 2,
                    highlighted ? 0xFFFFC85B : 0xFF4A5064);
            graphics.centeredText(this.font, Component.literal(Integer.toString(number)),
                    cell.x() + cell.width() / 2, cell.y() + 10, highlighted ? 0xFF1C1D24 : 0xFFFFFFFF);
        }
        int centerX = this.width / 2;
        int centerY = layout.y() + layout.height() / 2 + 6;
//        graphics.fill(centerX - 48, centerY - 34, centerX + 48, centerY + 34, 0xFF20232E);
//        graphics.centeredText(this.font, Component.literal(Integer.toString(active)), centerX, centerY - 12, 0xFFFFD76A);
        if (this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT) {
            Component result = this.winnerNames.isEmpty()
                    ? Component.translatable("gui.astral_craft.board.lottery_draw.no_winner")
                    : Component.translatable("gui.astral_craft.board.lottery_draw.winner", this.winnerNames.size(), this.awardEach);
            graphics.centeredText(this.font, result, centerX, centerY + 10,
                    this.winnerNames.isEmpty() ? 0xFFBFC2D0 : 0xFF72D27B);
            if (!this.winnerNames.isEmpty()) {
                graphics.centeredText(this.font, Component.literal(String.join("、", this.winnerNames)),
                        centerX, centerY + 24, 0xFFFFFFFF);
            }
        }
    }

    private int animatedNumber() {
        float progress = 1.0F - this.timeoutTicks / (float) Math.max(1, this.timeoutDurationTicks);
        progress = Math.clamp(progress, 0.0F, 1.0F);
        int offset = Math.floorMod(this.finalNumber - 1, 12);
        int totalSteps = 36 + offset;
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        int step = Math.min(totalSteps, (int) Math.floor(totalSteps * eased));
        return step % 12 + 1;
    }

    private Layout layout() {
        int width = Math.min(500, this.width - 24);
        int height = Math.min(360, this.height - 24);
        return new Layout((this.width - width) / 2, (this.height - height) / 2, width, height);
    }

    private record Cell(int x, int y, int width, int height) {}

    private record Layout(int x, int y, int width, int height) {
        private Cell cell(int number) {
            int w = 58;
            int h = 32;
            int gap = 8;
            int left = this.x + 34;
            int right = this.x + this.width - 34 - w;
            int top = this.y + 62;
            int bottom = this.y + this.height - 54 - h;
            return switch (number) {
                case 1, 2, 3 -> new Cell(this.x + this.width / 2 - (w * 3 + gap * 2) / 2
                        + (number - 1) * (w + gap), top, w, h);
                case 4, 5, 6 -> new Cell(right, this.y + this.height / 2 - (h * 3 + gap * 2) / 2
                        + (number - 4) * (h + gap), w, h);
                case 7, 8, 9 -> new Cell(this.x + this.width / 2 + (w * 3 + gap * 2) / 2 - w
                        - (number - 7) * (w + gap), bottom, w, h);
                default -> new Cell(left, this.y + this.height / 2 + (h * 3 + gap * 2) / 2 - h
                        - (number - 10) * (h + gap), w, h);
            };
        }
    }
}
