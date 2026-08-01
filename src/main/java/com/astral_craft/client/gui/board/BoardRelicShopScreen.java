package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.network.c2s.BoardRelicShopActionPayload;
import com.astral_craft.common.network.s2c.OpenBoardRelicShopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class BoardRelicShopScreen extends Screen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 154;
    private final UUID boardId;
    private int price;
    private int starCoins;
    private int timeoutTicks;
    private int timeoutDurationTicks;
    private Identifier characterId;
    private Identifier skinId;
    private int noticeCode;
    private boolean submitted;

    public BoardRelicShopScreen(OpenBoardRelicShopPayload payload) {
        super(Component.translatable("gui.astral_craft.board.relic_shop.title"));
        this.boardId = payload.boardId();
        this.apply(payload);
    }

    public static void open(OpenBoardRelicShopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof BoardRelicShopScreen screen && screen.boardId.equals(payload.boardId())) screen.apply(payload);
            else Minecraft.getInstance().setScreen(new BoardRelicShopScreen(payload));
        });
    }

    public static void closePresentation(UUID boardId) {
        Screen current = Minecraft.getInstance().screen;
        if (current instanceof BoardRelicShopScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    private void apply(OpenBoardRelicShopPayload payload) {
        this.price = payload.price();
        this.starCoins = payload.starCoins();
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = Math.max(1, payload.timeoutDurationTicks());
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
        this.noticeCode = payload.noticeCode();
        this.submitted = false;
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
        int x = (this.width - PANEL_WIDTH) / 2;
        int y = Math.max(14, (this.height - PANEL_HEIGHT) / 2);
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xF010111C);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 3, 0xD0FFFFFF);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, y + 17, 0xFFFFFFFF, true);
        Component balance = Component.translatable("gui.astral_craft.board.relic_shop.balance", this.starCoins);
        graphics.text(this.font, balance, this.width / 2 - this.font.width(balance) / 2, y + 43, 0xFFFFD36B, false);
        Component notice = this.notice();
        if (notice != null) graphics.text(this.font, notice, this.width / 2 - this.font.width(notice) / 2,
                y + 61, 0xFFFF7777, true);

        int buttonY = y + 82;
        int buttonWidth = 142;
        int gap = 18;
        int leaveX = this.width / 2 - buttonWidth - gap / 2;
        int buyX = this.width / 2 + gap / 2;
        boolean canBuy = this.starCoins >= this.price && !this.submitted;
        Component buy = Component.translatable("gui.astral_craft.board.relic_shop.buy", this.price);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.relic_shop.leave"),
                leaveX, buttonY, buttonWidth, 30, false,
                !this.submitted && inside(mouseX, mouseY, leaveX, buttonY, buttonWidth, 30),
                this.submitted ? AstralFancyButton.disabledButtonStyle() : ButtonStyle.button(0xFF496AA5));
        AstralFancyButton.renderButton(graphics, this.font, buy, buyX, buttonY, buttonWidth, 30, false,
                canBuy && inside(mouseX, mouseY, buyX, buttonY, buttonWidth, 30),
                canBuy ? ButtonStyle.button(0xFF56A85B) : AstralFancyButton.disabledButtonStyle());
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId, this.timeoutTicks,
                this.timeoutDurationTicks, this.width / 2, y + PANEL_HEIGHT - 13, 260);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || this.submitted) return super.mouseClicked(event, doubleClick);
        int y = Math.max(14, (this.height - PANEL_HEIGHT) / 2) + 82;
        int buttonWidth = 142;
        int gap = 18;
        int leaveX = this.width / 2 - buttonWidth - gap / 2;
        int buyX = this.width / 2 + gap / 2;
        if (inside(event.x(), event.y(), leaveX, y, buttonWidth, 30)) {
            this.submit(false);
            return true;
        }
        if (inside(event.x(), event.y(), buyX, y, buttonWidth, 30)) {
            if (this.starCoins < this.price) this.noticeCode = 1;
            else this.submit(true);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void submit(boolean buy) {
        if (this.submitted) return;
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardRelicShopActionPayload(this.boardId, buy));
    }

    private Component notice() {
        return switch (this.noticeCode) {
            case 1 -> Component.translatable("gui.astral_craft.board.relic_shop.insufficient");
            case 2 -> Component.translatable("gui.astral_craft.board.relic_shop.empty");
            default -> null;
        };
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

}