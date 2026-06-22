package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.UseHandCardFromDeckPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class HandCardDeckScreen extends Screen {

    protected static final int CARD_W = 56;
    protected static final int CARD_H = 78;
    protected static final int CARD_GAP = 10;
    protected static final int PANEL_H = 140;

    protected final List<CardEntry> cards;
    protected float scrollX;
    protected boolean draggingCard;
    protected CardEntry draggedCard;
    protected int dragX;
    protected int dragY;

    public HandCardDeckScreen() {
        super(Component.translatable("gui.astral_craft.hand_card_deck.title"));
        this.cards = collectCards();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.scrollX = Mth.clamp(this.scrollX, 0.0F, this.maxScroll());
        int panelY = this.panelY();
        graphics.fill(0, panelY, this.width, this.height, 0xD00B0B12);
        graphics.fill(0, panelY, this.width, panelY + 1, 0x80FFFFFF);
        graphics.text(this.font, this.title, 12, panelY + 10, 0xFFFFFFFF, false);
        Component hint = Component.translatable("gui.astral_craft.hand_card_deck.hint");
        graphics.text(this.font, hint, this.width - this.font.width(hint) - 12, panelY + 10, 0xFFBFC8FF, false);
        int listX = 12;
        int listY = panelY + 32;
        int listW = this.width - 24;
        graphics.enableScissor(listX, panelY, listX + listW, listY + CARD_H + 24);
        for (int i = 0; i < this.cards.size(); i++) {
            int x = listX + i * (CARD_W + CARD_GAP) - Math.round(this.scrollX);
            if (x + CARD_W < listX || x > listX + listW) continue;
            this.renderCard(graphics, this.cards.get(i), x, listY, mouseX, mouseY, false);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics, listX, this.height - 12, listW);
        if (this.draggingCard && this.draggedCard != null) {
            this.renderCard(graphics, this.draggedCard, mouseX - this.dragX, mouseY - this.dragY, mouseX, mouseY, true);
        }
    }

    protected void renderCard(GuiGraphicsExtractor graphics, CardEntry card, int x, int y, int mouseX, int mouseY, boolean dragging) {
        boolean hovered = !dragging && mouseX >= x && mouseX <= x + CARD_W && mouseY >= y - 10 && mouseY <= y + CARD_H + 7;
        Identifier frame = AstralCraft.prefix("textures/item/template_handcard_effect.png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, frame, x, y - 10, 0.0F, 0.0F, Mth.ceil(44 * 1.5F), Mth.ceil(64 * 1.5F), 256, 360, 256, 360, 0xFFFFFFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, card.texture(), x + 9, y, 0.0F, 0.0F, 48, 48, 256, 360, 256, 360, 0xFFFFFFFF);
        Component name = Component.translatable(card.definition().nameKey());
        Component trimmed = this.ellipsize(name, CARD_W + 10);
        graphics.text(this.font, trimmed, x + CARD_W / 2 - this.font.width(trimmed) / 2 + 5, y + CARD_H - 10, 0xFFFFFFFF, true);
        graphics.fill(x, y - 10, x + Mth.ceil(44 * 1.5F), y + CARD_H + 8, dragging ? 0xCCFFFFFF : hovered ? 0x66FFFFFF : 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        CardEntry card = this.cardAt(event.x(), event.y());
        if (card != null) {
            this.draggingCard = true;
            this.draggedCard = card;
            int[] pos = this.cardPos(card);
            this.dragX = (int) event.x() - pos[0];
            this.dragY = (int) event.y() - pos[1];
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingCard) {
            CardEntry card = this.draggedCard;
            this.draggingCard = false;
            this.draggedCard = null;
            if (card != null && event.y() < this.panelY() - 6) {
                ClientPacketDistributor.sendToServer(new UseHandCardFromDeckPayload(card.itemId().toString()));
                return true;
            }

            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseY >= this.panelY()) {
            this.scrollX = Mth.clamp(this.scrollX - (float) deltaY * 30.0F - (float) deltaX * 30.0F, 0.0F, this.maxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    protected CardEntry cardAt(double mouseX, double mouseY) {
        int listX = 12;
        int listY = this.panelY() + 32;
        for (int i = 0; i < this.cards.size(); i++) {
            int x = listX + i * (CARD_W + CARD_GAP) - Math.round(this.scrollX);
            if (mouseX >= x && mouseX <= x + CARD_W && mouseY >= listY && mouseY <= listY + CARD_H + 18) {
                return this.cards.get(i);
            }
        }

        return null;
    }

    protected int[] cardPos(CardEntry card) {
        int index = this.cards.indexOf(card);
        return new int[]{12 + index * (CARD_W + CARD_GAP) - Math.round(this.scrollX), this.panelY() + 32};
    }

    protected int panelY() {
        return Math.max(0, this.height - PANEL_H);
    }

    protected float maxScroll() {
        int content = this.cards.size() * (CARD_W + CARD_GAP) - CARD_GAP;
        return Math.max(0, content - (this.width - 24));
    }

    protected void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int width) {
        if (this.maxScroll() <= 0.5F) return;
        int content = Math.max(width, this.cards.size() * (CARD_W + CARD_GAP) - CARD_GAP);
        int thumbW = Mth.clamp(Math.round(width * (width / (float) content)), 24, width);
        int thumbX = x + Math.round((width - thumbW) * (this.scrollX / this.maxScroll()));
        graphics.fill(x, y, x + width, y + 5, 0x66000000);
        graphics.fill(thumbX, y, thumbX + thumbW, y + 5, 0xCCFFFFFF);
    }

    protected Component ellipsize(Component input, int maxWidth) {
        String text = input.getString();
        if (this.font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (this.font.width(out.toString()) + this.font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }

        return Component.literal(out + suffix);
    }

    protected static List<CardEntry> collectCards() {
        List<CardEntry> result = new ArrayList<>();
        for (AstralItems.ModelledCardItem modelledCardItem : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = modelledCardItem.item().get();
            if (item instanceof BaseHandCard card) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
                ItemStack stack = new ItemStack(item);
                CardType cardType = stack.get(AstralDataComponents.CARD_TYPE);
                if (cardType == CardType.EFFECT) {
                    CardDefinition definition = card.definition(stack);
                    Identifier texture = Identifier.parse(definition.largeFrontTexture());
                    result.add(new CardEntry(itemId, definition, texture));
                }
            }
        }

        return result;
    }

    protected record CardEntry(Identifier itemId, CardDefinition definition, Identifier texture) {}

}