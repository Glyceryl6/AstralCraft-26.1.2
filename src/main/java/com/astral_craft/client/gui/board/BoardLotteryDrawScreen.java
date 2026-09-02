package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.common.network.s2c.CloseBoardLotteryDrawPayload;
import com.astral_craft.common.network.s2c.OpenBoardLotteryDrawPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public class BoardLotteryDrawScreen extends Screen {

    private final UUID boardId;
    private OpenBoardLotteryDrawPayload.Phase phase;
    private int finalNumber;
    private int jackpot;
    private List<OpenBoardLotteryDrawPayload.Entry> entries;
    private List<String> winnerNames;
    private int awardEach;
    private int timeoutTicks;
    private int timeoutDurationTicks;
    private int lastAnimatedNumber = -1;
    private boolean resultSoundPlayed;

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
        if (this.phase == null || this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT
                && payload.phase() != OpenBoardLotteryDrawPayload.Phase.RESULT) {
            this.resultSoundPlayed = false;
            this.lastAnimatedNumber = -1;
        }

        this.phase = payload.phase();
        this.finalNumber = payload.finalNumber();
        this.jackpot = payload.jackpot();
        this.entries = payload.entries();
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
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT) {
            if (!this.resultSoundPlayed) {
                minecraft.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.9F, 1.45F);
                this.resultSoundPlayed = true;
            }
            return;
        }

        int number = this.animatedNumber();
        if (number != this.lastAnimatedNumber) {
            float pitch = 0.95F + number / 36.0F;
            minecraft.player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.45F, pitch);
            this.lastAnimatedNumber = number;
        }
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
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.lottery_draw.selected_numbers"),
                layout.listX(), layout.y() + 62, 0xFFFFD76A, true);
        for (int index = 0; index < Math.min(4, this.entries.size()); index++) {
            this.renderEntry(graphics, layout, index, this.entries.get(index));
        }

        boolean noEntriesResult = this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT && this.finalNumber == 0;
        if (!noEntriesResult) {
            int active = this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT ? this.finalNumber : this.animatedNumber();
            for (int number = 1; number <= 12; number++) {
                Cell cell = layout.cell(number);
                boolean highlighted = number == active;
                graphics.fill(cell.x(), cell.y(), cell.x() + cell.size(), cell.y() + cell.size(),
                        highlighted ? 0xFFDB9D35 : 0xFF303442);
                graphics.fill(cell.x() + 2, cell.y() + 2, cell.x() + cell.size() - 2, cell.y() + cell.size() - 2,
                        highlighted ? 0xFFFFC85B : 0xFF4A5064);
                graphics.centeredText(this.font, Component.literal(Integer.toString(number)),
                        cell.x() + cell.size() / 2, cell.y() + (cell.size() - 8) / 2,
                        highlighted ? 0xFF1C1D24 : 0xFFFFFFFF);
            }
        }

        int centerX = layout.boardCenterX();
        int centerY = layout.boardCenterY();
        if (this.phase == OpenBoardLotteryDrawPayload.Phase.RESULT) {
            Component result = this.winnerNames.isEmpty()
                    ? Component.translatable("gui.astral_craft.board.lottery_draw.no_winner")
                    : Component.translatable("gui.astral_craft.board.lottery_draw.winner", this.winnerNames.size(), this.awardEach);
            graphics.centeredText(this.font, result, centerX, centerY - 5,
                    this.winnerNames.isEmpty() ? 0xFFBFC2D0 : 0xFF72D27B);
            if (!this.winnerNames.isEmpty()) {
                List<FormattedCharSequence> lines = this.font.split(Component.literal(String.join(", ", this.winnerNames)),
                        layout.boardInnerWidth());
                for (int line = 0; line < Math.min(2, lines.size()); line++) {
                    graphics.text(this.font, lines.get(line), centerX - this.font.width(lines.get(line)) / 2,
                            centerY + 9 + line * 11, 0xFFFFFFFF, false);
                }
            }
        }
    }

    private void renderEntry(GuiGraphicsExtractor graphics, Layout layout, int index, OpenBoardLotteryDrawPayload.Entry entry) {
        int rowY = layout.entryRowY(index);
        int iconSize = layout.entryIconSize();
        AstralStatusIconRenderer.renderCharacterSkinHead(graphics, entry.characterId(), entry.skinId().getPath(),
                layout.listX(), rowY, iconSize, 255);
        int textX = layout.listX() + iconSize + 8;
        String numbers = entry.numbers().isEmpty() ? "-" : entry.numbers().stream()
                .map(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("-");
        Component firstLine = Component.translatable("gui.astral_craft.board.lottery_draw.entry", entry.name(), numbers);
        List<FormattedCharSequence> lines = this.font.split(firstLine, layout.listWidth() - iconSize - 10);
        for (int line = 0; line < Math.min(3, lines.size()); line++) {
            graphics.text(this.font, lines.get(line), textX, rowY + 5 + line * 11,
                    line == 0 ? 0xFFFFFFFF : 0xFFBFC7D5, line == 0);
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
        int width = Math.min(720, this.width - 24);
        int height = Math.min(370, this.height - 24);
        return new Layout((this.width - width) / 2, (this.height - height) / 2, width, height);
    }

    private record Cell(int x, int y, int size) {}

    private record Layout(int x, int y, int width, int height) {

        private int listX() { return this.x + 16; }
        private int listWidth() { return Math.min(270, Math.max(220, this.width * 2 / 5)); }
        private int entryIconSize() { return Math.clamp((this.height - 106) / 4, 28, 34); }
        private int entryRowY(int index) { return this.y + 76 + index * (this.entryIconSize() + 6); }
        private int boardX() { return this.x + this.listWidth() + 20; }
        private int boardWidth() { return this.width - this.listWidth() - 36; }
        private int boardCenterX() { return this.boardX() + this.boardWidth() / 2; }
        private int boardCenterY() { return this.y + 64 + (this.height - 76) / 2; }
        private int boardInnerWidth() { return 122; }

        private Cell cell(int number) {
            int widthSize = (this.boardWidth() - 48) / 6;
            int heightSize = (this.height - 92) / 5;
            int size = Math.clamp(Math.min(widthSize, heightSize), 26, 42);
            int gap = 5;
            int step = size + gap;
            int outerSize = size * 5 + gap * 4;
            int left = this.boardCenterX() - outerSize / 2;
            int top = this.boardCenterY() - outerSize / 2;
            return switch (number) {
                case 1, 2, 3 -> new Cell(left + number * step, top, size);
                case 4, 5, 6 -> new Cell(left + 4 * step, top + (number - 3) * step, size);
                case 7, 8, 9 -> new Cell(left + (10 - number) * step, top + 4 * step, size);
                default -> new Cell(left, top + (13 - number) * step, size);
            };
        }

    }

}