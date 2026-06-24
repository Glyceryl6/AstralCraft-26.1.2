package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.config.HandCardDeckClientSettings;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.OpenHandCardDeckPayload;
import com.astral_craft.common.network.UseHandCardFromDeckPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class HandCardDeckScreen extends Screen {

    protected static final int CARD_W = 66;
    protected static final int CARD_H = 96;
    protected static final int PANEL_H = 150;
    protected static final int CARD_ART_SIZE = 48;

    protected final List<CardEntry> cards = new ArrayList<>();
    protected boolean creativeMode;
    protected boolean draggingCard;
    protected CardEntry draggedCard;
    protected int dragX;
    protected int dragY;

    public HandCardDeckScreen(String encodedCards, boolean creativeMode) {
        super(Component.translatable("gui.astral_craft.hand_card_deck.title"));
        this.creativeMode = creativeMode;
        this.cards.addAll(this.decodeCards(encodedCards, creativeMode));
    }

    public static void open(OpenHandCardDeckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new HandCardDeckScreen(payload.encodedCards(), payload.creative())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelY = this.panelY();
        graphics.fill(0, panelY, this.width, this.height, 0xD00B0B12);
        graphics.fill(0, panelY, this.width, panelY + 1, 0x80FFFFFF);
        graphics.text(this.font, this.title, 12, panelY + 10, 0xFFFFFFFF, false);
        Component hint = Component.translatable(this.creativeMode ? "gui.astral_craft.hand_card_deck.hint_creative" : "gui.astral_craft.hand_card_deck.hint");
        graphics.text(this.font, hint, this.width - this.font.width(hint) - 12, panelY + 10, 0xFFBFC8FF, false);

        if (this.cards.isEmpty()) {
            Component empty = Component.translatable(this.creativeMode ? "gui.astral_craft.hand_card_deck.empty_creative" : "gui.astral_craft.hand_card_deck.empty");
            graphics.text(this.font, empty, this.width / 2 - this.font.width(empty) / 2, panelY + 72, 0xFFA6A8BC, false);
            return;
        }

        List<CardLayout> layouts = this.activeLayouts(mouseX, mouseY);
        graphics.enableScissor(0, Math.max(0, panelY - 56), this.width, this.height);
        for (CardLayout layout : layouts) {
            if (this.draggingCard && layout.entry().equals(this.draggedCard)) {
                continue;
            }

            this.renderLayoutCard(graphics, layout, mouseX, mouseY);
        }

        graphics.disableScissor();
        if (this.draggingCard && this.draggedCard != null) {
            this.renderCard(graphics, this.draggedCard, mouseX - this.dragX, mouseY - this.dragY, mouseX, mouseY, true);
        }
    }

    protected void renderLayoutCard(GuiGraphicsExtractor graphics, CardLayout layout, int mouseX, int mouseY) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(layout.centerX(), layout.centerY());
        if (Math.abs(layout.angleDegrees()) > 0.001F) {
            graphics.pose().rotate((float) Math.toRadians(layout.angleDegrees()));
        }

        graphics.pose().translate(-CARD_W / 2.0F, -CARD_H / 2.0F);
        this.renderCard(graphics, layout.entry(), 0, 0, mouseX, mouseY, false);
        graphics.pose().popMatrix();
    }

    protected void renderCard(GuiGraphicsExtractor graphics, CardEntry card, int x, int y, int mouseX, int mouseY, boolean dragging) {
        Identifier frame = AstralCraft.prefix("textures/item/template_handcard_effect.png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, frame, x, y, 0.0F, 0.0F, CARD_W, CARD_H, 256, 360, 256, 360, 0xFFFFFFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, card.texture(), x + 9, y + 10, 0.0F, 0.0F, CARD_ART_SIZE, CARD_ART_SIZE, 256, 360, 256, 360, 0xFFFFFFFF);
        Component name = Component.translatable(card.definition().nameKey());
        Component trimmed = this.ellipsize(name, CARD_W - 8);
        graphics.text(this.font, trimmed, x + CARD_W / 2 - this.font.width(trimmed) / 2, y + CARD_H - 24, 0xFFFFFFFF, true);
        if (!card.creative() && card.count() > 1) {
            Component count = Component.literal(String.valueOf(card.count()));
            int badgeW = Math.max(14, this.font.width(count) + 6);
            int badgeX = x + CARD_W - badgeW - 4;
            int badgeY = y + 4;
            graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 13, 0xE0111122);
            graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 1, 0xB0FFFFFF);
            graphics.text(this.font, count, badgeX + badgeW / 2 - this.font.width(count) / 2, badgeY + 3, 0xFFFFF08A, true);
        }

        if (dragging) {
            graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x33FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        CardLayout layout = this.layoutAt(event.x(), event.y());
        if (layout != null) {
            this.draggingCard = true;
            this.draggedCard = layout.entry();
            this.dragX = (int) event.x() - layout.left();
            this.dragY = (int) event.y() - layout.top();
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
            if (card != null && event.y() < this.panelY() - 6 && !CardRevealOverlay.isActive()) {
                ClientPacketDistributor.sendToServer(new UseHandCardFromDeckPayload(card.itemId().toString()));
                if (!card.creative()) {
                    this.removeOneLocal(card);
                }
                return true;
            }

            return true;
        }

        return super.mouseReleased(event);
    }

    protected CardLayout layoutAt(double mouseX, double mouseY) {
        List<CardLayout> layouts = this.activeLayouts((int) mouseX, (int) mouseY);
        for (int i = layouts.size() - 1; i >= 0; i--) {
            CardLayout layout = layouts.get(i);
            int padding = 8;
            if (mouseX >= layout.left() - padding && mouseX <= layout.left() + CARD_W + padding
                    && mouseY >= layout.top() - padding && mouseY <= layout.top() + CARD_H + padding) {
                return layout;
            }
        }

        return null;
    }

    protected List<CardLayout> activeLayouts(int mouseX, int mouseY) {
        return HandCardDeckClientSettings.fanLayout() ? this.fanLayouts(mouseX, mouseY) : this.classicLayouts(mouseX, mouseY);
    }

    protected List<CardLayout> classicLayouts(int mouseX, int mouseY) {
        List<CardLayout> result = new ArrayList<>(this.cards.size());
        int n = this.cards.size();
        if (n == 0) return result;
        float available = Math.max(1.0F, this.width - 38.0F);
        float spacing = n == 1 ? 0.0F : Mth.clamp(available / Math.max(1, n - 1), 24.0F, 72.0F);
        float totalWidth = (n - 1) * spacing + CARD_W;
        float startX = Math.max(12.0F + CARD_W / 2.0F, this.width / 2.0F - totalWidth / 2.0F + CARD_W / 2.0F);
        int centerY = this.panelY() + 92;
        for (int i = 0; i < n; i++) {
            int centerX = Math.round(startX + i * spacing);
            int left = centerX - CARD_W / 2;
            int top = centerY - CARD_H / 2;
            boolean hovered = mouseX >= left && mouseX <= left + CARD_W && mouseY >= top && mouseY <= top + CARD_H;
            if (hovered && !this.draggingCard) {
                top -= 8;
                centerY -= 8;
            }

            result.add(new CardLayout(this.cards.get(i), centerX, top + CARD_H / 2, left, top, 0.0F));
            if (hovered && !this.draggingCard) {
                centerY += 8;
            }
        }

        return result;
    }

    protected List<CardLayout> fanLayouts(int mouseX, int mouseY) {
        List<CardLayout> result = new ArrayList<>(this.cards.size());
        int n = this.cards.size();
        if (n == 0) return result;
        float maxOffset = Math.max(1.0F, (n - 1) / 2.0F);
        float available = Math.max(1.0F, this.width - 46.0F);
        float spacing = n == 1 ? 0.0F : Mth.clamp(available / Math.max(1, n - 1), 8.0F, 38.0F);
        float baseX = this.width / 2.0F;
        float baseY = this.panelY() + 104.0F;
        for (int i = 0; i < n; i++) {
            float offset = i - (n - 1) / 2.0F;
            float t = Math.abs(offset) / maxOffset;
            float angle = n == 1 ? 0.0F : Mth.clamp(offset * 4.4F, -30.0F, 30.0F);
            int centerX = Math.round(baseX + offset * spacing);
            int centerY = Math.round(baseY + t * 18.0F);
            int left = centerX - CARD_W / 2;
            int top = centerY - CARD_H / 2;
            boolean hovered = mouseX >= left && mouseX <= left + CARD_W && mouseY >= top && mouseY <= top + CARD_H;
            if (hovered && !this.draggingCard) {
                centerY -= 10;
                top -= 10;
            }

            result.add(new CardLayout(this.cards.get(i), centerX, centerY, left, top, angle));
        }

        return result;
    }

    protected int panelY() {
        return Math.max(0, this.height - PANEL_H);
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

    protected List<CardEntry> decodeCards(String encodedCards, boolean creative) {
        List<CardEntry> result = new ArrayList<>();
        if (encodedCards == null || encodedCards.isBlank()) {
            return result;
        }

        for (String encodedCard : encodedCards.split(";")) {
            String[] split = encodedCard.split("\\|", 2);
            if (split.length == 0 || split[0].isBlank()) continue;
            try {
                Identifier itemId = Identifier.parse(split[0]);
                int count = split.length > 1 ? Math.max(1, Integer.parseInt(split[1])) : 1;
                CardEntry entry = this.entryFor(itemId, count, creative);
                if (entry != null) {
                    result.add(entry);
                }
            } catch (Exception ignored) {}
        }

        return result;
    }

    protected CardEntry entryFor(Identifier itemId, int count, boolean creative) {
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (!(item instanceof BaseHandCard card)) {
            return null;
        }

        ItemStack stack = new ItemStack(item);
        if (stack.get(AstralDataComponents.CARD_TYPE) != CardType.EFFECT) {
            return null;
        }

        CardDefinition definition = card.definition(stack);
        Identifier texture = Identifier.parse(definition.largeFrontTexture());
        return new CardEntry(itemId, definition, texture, Math.max(1, count), creative);
    }

    protected void removeOneLocal(CardEntry card) {
        for (int i = 0; i < this.cards.size(); i++) {
            CardEntry current = this.cards.get(i);
            if (current.itemId().equals(card.itemId())) {
                if (current.count() <= 1) {
                    this.cards.remove(i);
                } else {
                    this.cards.set(i, new CardEntry(current.itemId(), current.definition(), current.texture(), current.count() - 1, false));
                }
                return;
            }
        }
    }

    protected record CardEntry(Identifier itemId, CardDefinition definition, Identifier texture, int count, boolean creative) {}

    protected record CardLayout(CardEntry entry, int centerX, int centerY, int left, int top, float angleDegrees) {}

}