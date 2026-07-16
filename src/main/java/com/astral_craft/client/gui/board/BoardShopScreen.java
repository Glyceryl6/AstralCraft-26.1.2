package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.BoardShopActionPayload;
import com.astral_craft.common.network.s2c.OpenBoardShopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Three-card board shop with server-validated multi-purchase and persistent sold styling. */
public class BoardShopScreen extends Screen {

    private static final int CARD_WIDTH = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_HEIGHT = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int CARD_GAP = 18;
    private final String boardId;
    private final Set<Integer> selected = new LinkedHashSet<>();
    private List<ShopCard> cards = List.of();
    private int purchasedMask;
    private int starCoins;
    private int cardPrice;
    private int timeoutTicks;
    private int timeoutDurationTicks;
    private Identifier characterId;
    private Identifier skinId;
    private int noticeCode;
    private boolean submitted;

    public BoardShopScreen(OpenBoardShopPayload payload) {
        super(Component.translatable("gui.astral_craft.board.shop"));
        this.boardId = payload.boardId();
        this.apply(payload);
    }

    public static void open(OpenBoardShopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof BoardShopScreen shop && shop.boardId.equals(payload.boardId())) {
                shop.apply(payload);
            } else {
                Minecraft.getInstance().setScreen(new BoardShopScreen(payload));
            }
        });
    }

    private void apply(OpenBoardShopPayload payload) {
        this.cards = decode(payload.offers());
        this.purchasedMask = payload.purchasedMask();
        this.starCoins = payload.starCoins();
        this.cardPrice = payload.cardPrice();
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = Math.max(1, payload.timeoutDurationTicks());
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
        this.noticeCode = payload.noticeCode();
        this.submitted = false;
        this.selected.removeIf(index -> index < 0 || index >= this.cards.size() || this.purchased(index));
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
        if (this.timeoutTicks > 0 && --this.timeoutTicks <= 0 && !this.submitted) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF010111C);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 3, 0xD0FFFFFF);
        graphics.text(this.font, this.title, layout.x() + 18, layout.y() + 15, 0xFFFFFFFF, true);
        Component balance = Component.translatable("gui.astral_craft.board.shop_balance", this.starCoins);
        graphics.text(this.font, balance, layout.x() + layout.width() - 18 - this.font.width(balance),
                layout.y() + 15, 0xFFFFD36B, true);
        Component hint = Component.translatable("gui.astral_craft.board.shop_hint", this.cardPrice,
                OpenBoardShopPayload.MAXIMUM_OFFERS);
        graphics.text(this.font, hint, layout.x() + 18, layout.y() + 34, 0xFFDDD7F3, false);

        for (int index = 0; index < this.cards.size(); index++) {
            ShopCard card = this.cards.get(index);
            int x = layout.cardX(index);
            int y = layout.cardY();
            HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(),
                    card.definition().largeFrontTexture(card.stack()), card.definition().displayName(card.stack()),
                    x, y, mouseX, mouseY, false);
            if (this.purchased(index)) {
                graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xE08B8B8B);
                graphics.fill(x, y, x + CARD_WIDTH, y + 2, 0xFF222222);
                Component sold = Component.translatable("gui.astral_craft.board.shop_sold");
                graphics.fill(x + 4, y + CARD_HEIGHT / 2 - 9, x + CARD_WIDTH - 4,
                        y + CARD_HEIGHT / 2 + 9, 0xD0000000);
                graphics.text(this.font, sold, x + CARD_WIDTH / 2 - this.font.width(sold) / 2,
                        y + CARD_HEIGHT / 2 - 4, 0xFFFFFFFF, true);
            } else if (this.selected.contains(index)) {
                graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x4472FF72);
                graphics.fill(x, y, x + CARD_WIDTH, y + 3, 0xFF72FF72);
            }
            Component price = Component.translatable("gui.astral_craft.board.shop_price", this.cardPrice);
            graphics.fill(x + 4, y + 4, x + 8 + this.font.width(price), y + 17, 0xD0000000);
            graphics.text(this.font, price, x + 6, y + 7, 0xFFFFD36B, true);
        }

        int total = this.selected.size() * this.cardPrice;
        Component totalText = Component.translatable("gui.astral_craft.board.shop_total", total);
        graphics.text(this.font, totalText, layout.x() + 18, layout.buttonY() - 22,
                total <= this.starCoins ? 0xFF9DFFB2 : 0xFFFF7777, false);
        Component notice = this.notice();
        if (notice != null) {
            int noticeColor = this.noticeCode == 3 ? 0xFF9DFFB2 : 0xFFFF7777;
            graphics.text(this.font, notice, layout.x() + layout.width() / 2 - this.font.width(notice) / 2,
                    layout.buttonY() - 22, noticeColor, true);
        }
        boolean canBuy = !this.selected.isEmpty() && total <= this.starCoins && !this.submitted;
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.shop_buy"), layout.buyX(), layout.buttonY(),
                layout.buttonWidth(), 30, false,
                canBuy && inside(mouseX, mouseY, layout.buyX(), layout.buttonY(), layout.buttonWidth(), 30),
                canBuy ? ButtonStyle.button(0xFF56A85B) : AstralFancyButton.disabledButtonStyle());
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.shop_leave"), layout.leaveX(), layout.buttonY(),
                layout.buttonWidth(), 30, false,
                !this.submitted && inside(mouseX, mouseY, layout.leaveX(), layout.buttonY(), layout.buttonWidth(), 30),
                this.submitted ? AstralFancyButton.disabledButtonStyle() : ButtonStyle.button(0xFF496AA5));
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                this.timeoutTicks, this.timeoutDurationTicks, layout.x() + layout.width() / 2,
                layout.y() + layout.height() - 15, Math.min(260, layout.width() - 50));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || this.submitted) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        for (int index = 0; index < this.cards.size(); index++) {
            if (!this.purchased(index) && inside(event.x(), event.y(), layout.cardX(index), layout.cardY(),
                    CARD_WIDTH, CARD_HEIGHT)) {
                if (!this.selected.remove(index) && this.selected.size() < OpenBoardShopPayload.MAXIMUM_OFFERS) {
                    this.selected.add(index);
                }
                this.noticeCode = 0;
                return true;
            }
        }
        if (inside(event.x(), event.y(), layout.buyX(), layout.buttonY(), layout.buttonWidth(), 30)) {
            int total = this.selected.size() * this.cardPrice;
            if (this.selected.isEmpty()) {
                this.noticeCode = 2;
            } else if (total > this.starCoins) {
                this.noticeCode = 1;
            } else {
                this.submitted = true;
                ClientPacketDistributor.sendToServer(new BoardShopActionPayload(this.boardId,
                        List.copyOf(this.selected), false));
            }
            return true;
        }
        if (inside(event.x(), event.y(), layout.leaveX(), layout.buttonY(), layout.buttonWidth(), 30)) {
            this.leave();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void leave() {
        if (this.submitted) return;
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardShopActionPayload(this.boardId, List.of(), true));
        this.onClose();
    }

    private boolean purchased(int index) {
        return (this.purchasedMask & 1 << index) != 0;
    }

    private Component notice() {
        return switch (this.noticeCode) {
            case 1 -> Component.translatable("gui.astral_craft.board.shop_insufficient");
            case 2 -> Component.translatable("gui.astral_craft.board.shop_select_card");
            case 3 -> Component.translatable("gui.astral_craft.board.shop_purchase_success");
            default -> null;
        };
    }

    private Layout layout() {
        int cardsWidth = this.cards.size() * CARD_WIDTH + Math.max(0, this.cards.size() - 1) * CARD_GAP;
        int width = Math.clamp(Math.max(cardsWidth + 50, 430), 300, Math.max(300, this.width - 20));
        int height = Math.clamp(CARD_HEIGHT + 126, 230, Math.max(230, this.height - 20));
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int cardsStart = x + (width - cardsWidth) / 2;
        int cardY = y + 55;
        int buttonWidth = Math.min(132, (width - 54) / 2);
        int buttonY = y + height - 52;
        return new Layout(x, y, width, height, cardsStart, cardY, buttonWidth, buttonY);
    }

    private static List<ShopCard> decode(List<Identifier> offers) {
        List<ShopCard> result = new ArrayList<>();
        for (Identifier id : offers) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (!(item instanceof BaseHandCard card)) continue;
            ItemStack stack = new ItemStack(item);
            result.add(new ShopCard(stack, card.definition(stack)));
        }
        return List.copyOf(result);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record ShopCard(ItemStack stack, CardDefinition definition) {}

    private record Layout(int x, int y, int width, int height, int cardsStartX, int cardY,
                          int buttonWidth, int buttonY) {
        private int cardX(int index) { return this.cardsStartX + index * (CARD_WIDTH + CARD_GAP); }
        private int buyX() { return this.x + 18; }
        private int leaveX() { return this.x + this.width - this.buttonWidth - 18; }
    }
}
