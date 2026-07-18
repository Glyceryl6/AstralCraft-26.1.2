package com.astral_craft.client.gui;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.CardRangeResolver;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.s2c.OpenHandCardDeckPayload;
import com.astral_craft.common.network.c2s.UseHandCardFromDeckPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.text.AstralTextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class HandCardDeckScreen extends Screen {

    protected static final int PANEL_H = 140;
    protected static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    protected static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    protected static final int CARD_GAP = 6;
    protected static final int TOOLTIP_W = 250;
    protected static final int TOOLTIP_MAX_H = 124;
    protected static final int SCROLLBAR_H = 7;
    protected static final int CARD_ROW_TOP_OFFSET = 24;

    protected final List<CardEntry> cards = new ArrayList<>();
    protected boolean creativeMode;
    protected boolean draggingCard;
    protected CardEntry draggedCard;
    protected int dragX;
    protected int dragY;
    protected float scrollX;
    protected boolean draggingScrollbar;
    protected double dragStartX;
    protected float dragStartScrollX;

    public HandCardDeckScreen(List<OpenHandCardDeckPayload.HandCardEntry> cards, boolean creativeMode) {
        super(Component.translatable("gui.astral_craft.hand_card_deck.title"));
        this.creativeMode = creativeMode;
        for (OpenHandCardDeckPayload.HandCardEntry card : cards) {
            CardEntry entry = this.entryFor(card.stack(), card.count(), creativeMode);
            if (entry != null) {
                this.cards.add(entry);
            }
        }
    }

    public static void open(OpenHandCardDeckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new HandCardDeckScreen(payload.cards(), payload.creative())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.clampScroll();
        int panelY = this.panelY();
        graphics.fill(0, panelY, this.width, this.height, 0xE00B0B12);
        graphics.fill(0, panelY, this.width, panelY + 1, 0x90FFFFFF);
        graphics.text(this.font, this.title, 12, panelY + 10, 0xFFFFFFFF, false);
        Component hint = Component.translatable(this.creativeMode ? "gui.astral_craft.hand_card_deck.hint_creative" : "gui.astral_craft.hand_card_deck.hint");
        graphics.text(this.font, hint, this.width - this.font.width(hint) - 12, panelY + 10, 0xFFBFC8FF, false);

        if (this.cards.isEmpty()) {
            Component empty = Component.translatable(this.creativeMode ? "gui.astral_craft.hand_card_deck.empty_creative" : "gui.astral_craft.hand_card_deck.empty");
            graphics.text(this.font, empty, this.width / 2 - this.font.width(empty) / 2, panelY + 80, 0xFFA6A8BC, false);
            return;
        }

        CardLayout hovered = this.layoutAt(mouseX, mouseY);
        List<CardLayout> layouts = this.visibleLayouts();
        int listLeft = this.listLeft();
        int listTop = this.listTop();
        int listRight = this.listRight();
        int listBottom = this.listBottom();
        graphics.enableScissor(listLeft, listTop - 4, listRight, listBottom + 4);
        for (CardLayout layout : layouts) {
            if (this.draggingCard && layout.entry().equals(this.draggedCard)) continue;
            this.renderCard(graphics, layout.entry(), layout.left(), layout.top(), mouseX, mouseY, hovered != null && hovered.entry().equals(layout.entry()), false);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics);
        if (this.draggingCard && this.draggedCard != null) {
            this.renderCard(graphics, this.draggedCard, mouseX - this.dragX, mouseY - this.dragY, mouseX, mouseY, false, true);
        } else if (hovered != null) {
            this.renderCardTooltip(graphics, hovered.entry(), mouseX, mouseY);
        }
    }

    protected void renderCard(GuiGraphicsExtractor graphics, CardEntry card, int x, int y, int mouseX, int mouseY, boolean hovered, boolean dragging) {
        HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(), card.texture(), card.definition().displayName(card.stack()), x, y, mouseX, mouseY, dragging);
        if (!card.creative()) {
            HandCardRenderHelper.renderCardCount(graphics, this.font, card.count(), x, y);
        }

        if (hovered && !dragging) {
            graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x22FFF08A);
            graphics.fill(x, y, x + CARD_W, y + 1, 0xCCFFF08A);
            graphics.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, 0xCCFFF08A);
            graphics.fill(x, y, x + 1, y + CARD_H, 0xCCFFF08A);
            graphics.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, 0xCCFFF08A);
        }
    }

    protected void renderCardTooltip(GuiGraphicsExtractor graphics, CardEntry card, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = this.tooltipLines(card);
        int lineH = 10;
        int tooltipH = Math.min(TOOLTIP_MAX_H, lines.size() * lineH + 10);
        int tooltipX = Math.min(mouseX + 14, this.width - TOOLTIP_W - 8);
        int tooltipY = Math.max(8, Math.min(mouseY - 8, this.height - tooltipH - 8));
        graphics.fill(tooltipX, tooltipY, tooltipX + TOOLTIP_W, tooltipY + tooltipH, 0xF0141424);
        graphics.fill(tooltipX, tooltipY, tooltipX + TOOLTIP_W, tooltipY + 1, 0xB0FFFFFF);
        int y = tooltipY + 6;
        int maxLines = Math.max(1, (tooltipH - 10) / lineH);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(this.font, lines.get(i), tooltipX + 6, y, 0xFFFFFFFF, false);
            y += lineH;
        }

        if (lines.size() > maxLines) {
            Component more = Component.translatable("gui.astral_craft.hand_card_deck.tooltip.more");
            graphics.text(this.font, more, tooltipX + TOOLTIP_W - this.font.width(more) - 6, tooltipY + tooltipH - lineH, 0xFFBFC8FF, false);
        }
    }

    protected List<FormattedCharSequence> tooltipLines(CardEntry card) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        CardType cardType = card.definition().type();
        String name = cardType.getSerializedName();
        this.addWrappedTooltipLine(lines, card.definition().displayName(card.stack()).withStyle(ChatFormatting.BOLD));
        this.addWrappedTooltipLine(lines, Component.translatable("tooltips.astral_craft.handcard.card_type." + name).withColor(cardType.color));
        int effectiveRange = CardRangeResolver.effectiveRange(this.minecraft.player, card.stack, card.definition);
        Component component = card.definition().effectText(card.stack(), effectiveRange);
        for (Component line : AstralTextFormatter.lines(component)) {
            this.addWrappedTooltipLine(lines, line);
        }

        if (!card.definition().restrictions().unrestricted()) {
            this.addWrappedTooltipLine(lines, Component.translatable("tooltips.astral_craft.handcard.restricted").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (!card.creative()) {
            this.addWrappedTooltipLine(lines, Component.translatable("gui.astral_craft.hand_card_deck.tooltip.count", card.count()).withStyle(ChatFormatting.YELLOW));
        }

        return lines;
    }

    protected void addWrappedTooltipLine(List<FormattedCharSequence> lines, Component component) {
        lines.addAll(this.font.split(component, TOOLTIP_W - 12));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (this.startScrollbarDrag(event.x(), event.y())) {
            return true;
        }

        CardLayout layout = this.layoutAt(event.x(), event.y());
        if (layout != null) {
            this.draggingCard = true;
            this.draggedCard = layout.entry();
            this.dragX = Math.clamp((int) event.x() - layout.left(), 0, CARD_W);
            this.dragY = Math.clamp((int) event.y() - layout.top(), 0, CARD_H);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            this.updateScrollbarDrag(event.x());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        if (event.button() == 0 && this.draggingCard) {
            CardEntry card = this.draggedCard;
            this.draggingCard = false;
            this.draggedCard = null;
            if (card != null && event.y() < this.panelY() - 6 && !CardRevealOverlay.isActive()) {
                ClientPacketDistributor.sendToServer(new UseHandCardFromDeckPayload(card.itemId()));
                if (!card.creative()) {
                    this.removeOneLocal(card);
                    this.clampScroll();
                }

                return true;
            }

            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseY >= this.listTop() && mouseY <= this.listBottom()) {
            this.scrollX = Mth.clamp(this.scrollX - (float) deltaY * 42.0F - (float) deltaX * 42.0F, 0.0F, this.maxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    protected List<CardLayout> visibleLayouts() {
        List<CardLayout> result = new ArrayList<>();
        int y = this.listTop();
        int x = this.listLeft() - Math.round(this.scrollX);
        int minX = this.listLeft() - CARD_W - CARD_GAP;
        int maxX = this.listRight() + CARD_GAP;
        for (CardEntry card : this.cards) {
            if (x >= minX && x <= maxX) {
                result.add(new CardLayout(card, x, y));
            }
            x += CARD_W + CARD_GAP;
        }
        return result;
    }

    protected CardLayout layoutAt(double mouseX, double mouseY) {
        if (mouseY < this.listTop() || mouseY > this.listBottom()) return null;
        for (CardLayout layout : this.visibleLayouts()) {
            if (mouseX >= layout.left() && mouseX <= layout.left() + CARD_W && mouseY >= layout.top() && mouseY <= layout.top() + CARD_H) {
                return layout;
            }
        }
        return null;
    }

    protected int listLeft() {
        return 12;
    }

    protected int listRight() {
        return this.width - 12;
    }

    protected int listTop() {
        return this.panelY() + CARD_ROW_TOP_OFFSET;
    }

    protected int listBottom() {
        return this.listTop() + CARD_H;
    }

    protected int scrollbarLeft() {
        return this.listLeft();
    }

    protected int scrollbarTop() {
        return this.height - 12;
    }

    protected int scrollbarWidth() {
        return this.listRight() - this.listLeft();
    }

    protected int scrollbarThumbWidth() {
        int width = this.scrollbarWidth();
        int contentWidth = Math.max(width, this.cards.size() * (CARD_W + CARD_GAP) - CARD_GAP);
        return Mth.clamp(Math.round(width * (width / (float) contentWidth)), 24, width);
    }

    protected int scrollbarThumbLeft() {
        float maxScroll = this.maxScroll();
        if (maxScroll <= 0.0F) return this.scrollbarLeft();
        int width = this.scrollbarWidth();
        int thumbW = this.scrollbarThumbWidth();
        return this.scrollbarLeft() + Math.round((width - thumbW) * (this.scrollX / maxScroll));
    }

    protected int panelY() {
        return Math.max(0, this.height - PANEL_H);
    }

    protected float maxScroll() {
        int contentWidth = this.cards.size() * (CARD_W + CARD_GAP) - CARD_GAP;
        return Math.max(0.0F, contentWidth - (this.listRight() - this.listLeft()));
    }

    protected void clampScroll() {
        this.scrollX = Mth.clamp(this.scrollX, 0.0F, this.maxScroll());
    }

    protected void renderScrollbar(GuiGraphicsExtractor graphics) {
        float maxScroll = this.maxScroll();
        if (maxScroll <= 0.0F) return;
        int x = this.scrollbarLeft();
        int y = this.scrollbarTop();
        int width = this.scrollbarWidth();
        int thumbW = this.scrollbarThumbWidth();
        int thumbX = this.scrollbarThumbLeft();
        graphics.fill(x, y, x + width, y + SCROLLBAR_H, 0x55000000);
        graphics.fill(thumbX, y, thumbX + thumbW, y + SCROLLBAR_H, this.draggingScrollbar ? 0xEEFFFFFF : 0xAAFFFFFF);
    }

    protected boolean startScrollbarDrag(double mouseX, double mouseY) {
        if (this.maxScroll() <= 0.0F) return false;
        int x = this.scrollbarLeft();
        int y = this.scrollbarTop();
        int width = this.scrollbarWidth();
        if (mouseX < x || mouseX > x + width || mouseY < y - 2 || mouseY > y + SCROLLBAR_H + 2) {
            return false;
        }
        int thumbX = this.scrollbarThumbLeft();
        int thumbW = this.scrollbarThumbWidth();
        if (mouseX < thumbX || mouseX > thumbX + thumbW) {
            this.updateScrollbarFromThumbCenter(mouseX);
        }
        this.draggingScrollbar = true;
        this.dragStartX = mouseX;
        this.dragStartScrollX = this.scrollX;
        return true;
    }

    protected void updateScrollbarDrag(double mouseX) {
        int movable = Math.max(1, this.scrollbarWidth() - this.scrollbarThumbWidth());
        float next = this.dragStartScrollX + (float) ((mouseX - this.dragStartX) / movable * this.maxScroll());
        this.scrollX = Mth.clamp(next, 0.0F, this.maxScroll());
    }

    protected void updateScrollbarFromThumbCenter(double mouseX) {
        int movable = Math.max(1, this.scrollbarWidth() - this.scrollbarThumbWidth());
        double centerOffset = mouseX - this.scrollbarLeft() - this.scrollbarThumbWidth() / 2.0D;
        this.scrollX = Mth.clamp((float) (centerOffset / movable * this.maxScroll()), 0.0F, this.maxScroll());
    }

    protected CardEntry entryFor(ItemStack sourceStack, int count, boolean creative) {
        if (sourceStack == null || sourceStack.isEmpty() || !(sourceStack.getItem() instanceof BaseHandCard card)) {
            return null;
        }

        ItemStack stack = sourceStack.copyWithCount(1);
        if (stack.get(AstralDataComponents.CARD_TYPE) != CardType.EFFECT) {
            return null;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        CardDefinition definition = card.definition(stack);
        Identifier texture = definition.largeFrontTexture(stack);
        return new CardEntry(itemId, definition, texture, stack, Math.max(1, count), creative);
    }

    protected void removeOneLocal(CardEntry card) {
        for (int i = 0; i < this.cards.size(); i++) {
            CardEntry current = this.cards.get(i);
            if (current.itemId().equals(card.itemId())) {
                if (current.count() <= 1) {
                    this.cards.remove(i);
                } else {
                    this.cards.set(i, new CardEntry(current.itemId(), current.definition(), current.texture(), current.stack(), current.count() - 1, false));
                }
                return;
            }
        }
    }

    protected record CardEntry(Identifier itemId, CardDefinition definition, Identifier texture, ItemStack stack, int count, boolean creative) {}

    protected record CardLayout(CardEntry entry, int left, int top) {}

}