package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.network.c2s.BoardGambleChoicePayload;
import com.astral_craft.common.network.s2c.CloseBoardGamblePayload;
import com.astral_craft.common.network.s2c.OpenBoardGamblePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public class BoardGambleScreen extends Screen {

    private final UUID boardId;
    private OpenBoardGamblePayload.Phase phase;
    private List<OpenBoardGamblePayload.Entry> entries;
    private boolean localCanChoose;
    private int dieResult;
    private int totalReward;
    private int phaseAgeTicks;
    private int phaseDurationTicks = 1;
    private boolean submitted;

    public BoardGambleScreen(OpenBoardGamblePayload payload) {
        super(Component.translatable("gui.astral_craft.board.gamble.title"));
        this.boardId = payload.boardId();
        this.update(payload);
    }

    public static void open(OpenBoardGamblePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardGambleScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.update(payload);
            } else {
                minecraft.setScreen(new BoardGambleScreen(payload));
            }
        });
    }

    public static void close(CloseBoardGamblePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardGambleScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.onClose();
            }
        });
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardGambleScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    private void update(OpenBoardGamblePayload payload) {
        OpenBoardGamblePayload.Phase previousPhase = this.phase;
        this.phase = payload.phase();
        this.entries = payload.entries();
        this.localCanChoose = payload.localCanChoose();
        this.dieResult = payload.dieResult();
        this.totalReward = payload.totalReward();
        this.phaseDurationTicks = Math.max(1, payload.timeoutDurationTicks());
        if (previousPhase != this.phase) this.phaseAgeTicks = 0;
        if (this.localCanChoose) this.submitted = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.phaseAgeTicks++;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF010111B);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xD8FFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.gamble.reward", this.totalReward),
                this.width / 2, layout.y() + 30, 0xFFFFD76A);
        for (int index = 0; index < Math.min(4, this.entries.size()); index++) {
            this.renderEntry(graphics, layout.entry(index), this.entries.get(index));
        }

        if (this.phase == OpenBoardGamblePayload.Phase.CHOOSING) {
            if (this.localCanChoose && !this.submitted) {
                AstralFancyButton.renderButton(graphics, this.font,
                        Component.translatable("gui.astral_craft.board.gamble.odd"),
                        layout.oddX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight(), false,
                        inside(mouseX, mouseY, layout.oddX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight()),
                        ButtonStyle.button(0xFFB24D68));
                AstralFancyButton.renderButton(graphics, this.font,
                        Component.translatable("gui.astral_craft.board.gamble.even"),
                        layout.evenX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight(), false,
                        inside(mouseX, mouseY, layout.evenX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight()),
                        ButtonStyle.button(0xFF477EAB));
            } else {
                graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.gamble.waiting"),
                        this.width / 2, layout.buttonY() + 9, 0xFFBFC2D0);
            }
        } else if (this.phase == OpenBoardGamblePayload.Phase.ROLLING) {
            this.renderDiceRoll(graphics, layout, true);
        } else {
            this.renderDiceRoll(graphics, layout, false);
            int shown = Math.clamp(this.dieResult, 1, 6);
            graphics.centeredText(this.font, Component.translatable(shown % 2 == 0
                            ? "gui.astral_craft.board.gamble.result_even"
                            : "gui.astral_craft.board.gamble.result_odd", shown),
                    this.width / 2, layout.buttonY() + 50, 0xFFFFD76A);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.localCanChoose || this.submitted || this.phase != OpenBoardGamblePayload.Phase.CHOOSING
                || event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.oddX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight())) {
            this.choose(true);
            return true;
        }

        if (inside(event.x(), event.y(), layout.evenX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight())) {
            this.choose(false);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void renderDiceRoll(GuiGraphicsExtractor graphics, Layout layout, boolean rolling) {
        int settleTick = Math.max(8, this.phaseDurationTicks - 14);
        int shown = rolling && this.phaseAgeTicks < settleTick
                ? 1 + Math.floorMod(this.phaseAgeTicks / 2 * 5 + 1, 6)
                : Math.clamp(this.dieResult, 1, 6);
        float pulse = rolling && this.phaseAgeTicks < settleTick
                ? 1.0F + (float) Math.sin(this.phaseAgeTicks * 0.55F) * 0.05F : 1.0F;
        int centerX = this.width / 2;
        int centerY = layout.buttonY() + 14;
        int size = 70;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(pulse, pulse);
        graphics.fill(-size / 2, -size / 2, size / 2, size / 2, 0xFFE7D7FF);
        graphics.fill(-size / 2 + 3, -size / 2 + 3, size / 2 - 3, size / 2 - 3, 0xFF2B2538);
        Component value = Component.literal(Integer.toString(shown));
        float textScale = 4.0F;
        graphics.pose().pushMatrix();
        graphics.pose().scale(textScale, textScale);
        graphics.text(this.font, value, -this.font.width(value) / 2, -4, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
        graphics.pose().popMatrix();
        if (rolling) graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.gamble.rolling"),
                centerX, centerY + size / 2 + 8, 0xFFBFC2D0);
    }

    private void renderEntry(GuiGraphicsExtractor graphics, EntryLayout layout, OpenBoardGamblePayload.Entry entry) {
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xD8242633);
        AstralStatusIconRenderer.renderCharacterSkinHead(graphics, entry.characterId(), entry.skinId().getPath(),
                layout.x() + (layout.width() - 42) / 2, layout.y() + 8, 42, 255);
        if (!entry.eligible()) graphics.fill(layout.x() + (layout.width() - 42) / 2, layout.y() + 8,
                layout.x() + (layout.width() + 42) / 2, layout.y() + 50, 0xA8D0D0D0);
        graphics.centeredText(this.font, Component.literal(entry.name()), layout.x() + layout.width() / 2,
                layout.y() + 55, entry.eligible() ? 0xFFFFFFFF : 0xFF85858D);
        Component status;
        int color;
        if (!entry.eligible()) {
            status = Component.translatable("gui.astral_craft.board.gamble.ineligible");
            color = 0xFF85858D;
        } else if (this.phase == OpenBoardGamblePayload.Phase.RESULT) {
            status = Component.literal(entry.winner() ? "✓" : "✕");
            color = entry.winner() ? 0xFF72D27B : 0xFFE06464;
        } else if (entry.chosen()) {
            status = Component.translatable("gui.astral_craft.board.gamble.ready");
            color = 0xFF72D27B;
        } else {
            status = Component.translatable("gui.astral_craft.board.gamble.thinking");
            color = 0xFFBFC2D0;
        }

        graphics.centeredText(this.font, status, layout.x() + layout.width() / 2, layout.y() + 69, color);
    }

    private void choose(boolean odd) {
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardGambleChoicePayload(this.boardId, odd));
    }

    private Layout layout() {
        int width = Math.min(520, this.width - 24);
        int height = Math.min(360, this.height - 24);
        return new Layout((this.width - width) / 2, (this.height - height) / 2, width, height);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record EntryLayout(int x, int y, int width, int height) {}

    private record Layout(int x, int y, int width, int height) {

        private EntryLayout entry(int index) {
            int entryWidth = 132;
            int entryHeight = 86;
            int left = this.x + 22;
            int right = this.x + this.width - 22 - entryWidth;
            int top = this.y + 52;
            int bottom = this.y + this.height - 116;
            return switch (index) {
                case 0 -> new EntryLayout(left, top, entryWidth, entryHeight);
                case 1 -> new EntryLayout(right, top, entryWidth, entryHeight);
                case 2 -> new EntryLayout(left, bottom, entryWidth, entryHeight);
                default -> new EntryLayout(right, bottom, entryWidth, entryHeight);
            };
        }

        private int buttonWidth() { return 94; }
        private int buttonHeight() { return 34; }
        private int buttonY() { return this.y + this.height / 2 - 17; }
        private int oddX() { return this.x + this.width / 2 - this.buttonWidth() - 8; }
        private int evenX() { return this.x + this.width / 2 + 8; }

    }

}