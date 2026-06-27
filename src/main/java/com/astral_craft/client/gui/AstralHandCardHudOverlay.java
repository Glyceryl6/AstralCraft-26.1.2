package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AstralHandCardHudOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("hand_card_hud");
    protected static final int MAX_CARD_ICONS_PER_ROW = 8;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        ActiveCharacterState activeCharacter = minecraft.player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!activeCharacter.active()) return;
        renderInventoryCardSummary(graphics, minecraft, graphics.guiWidth() - 10, graphics.guiHeight() - 50);
    }

    protected static void renderInventoryCardSummary(GuiGraphicsExtractor graphics, Minecraft minecraft, int right, int bottom) {
        List<CardIconEntry> cards = inventoryCards(minecraft);
        if (cards.isEmpty()) {
            int x = right - 22;
            int y = bottom - 22;
            graphics.fill(x - 5, y - 5, right + 5, bottom + 14, 0xA80B0B12);
            graphics.fill(x - 5, y - 5, right + 5, y - 4, 0x60FFFFFF);
            graphics.fill(x, y, x + HandCardRenderHelper.ITEM_ICON_SIZE, y + HandCardRenderHelper.ITEM_ICON_SIZE, 0xFF31364D);
            graphics.fill(x + 2, y + 2, x + HandCardRenderHelper.ITEM_ICON_SIZE - 2, y + HandCardRenderHelper.ITEM_ICON_SIZE - 2, 0xFF171A28);
            Component hint = Component.translatable("hud.astral_craft.hand_card_deck.hint");
            graphics.text(minecraft.font, hint, right - minecraft.font.width(hint), bottom + 2, 0xFFBFC8FF, true);
            return;
        }

        int columns = Math.min(MAX_CARD_ICONS_PER_ROW, cards.size());
        int rows = (int) Math.ceil(cards.size() / (double) columns);
        int iconStep = 19;
        int width = columns * iconStep - 3;
        int height = rows * iconStep - 3;
        int left = right - width;
        int top = bottom - height;
        graphics.fill(left - 5, top - 5, right + 5, bottom + 16, 0xA80B0B12);
        graphics.fill(left - 5, top - 5, right + 5, top - 4, 0x60FFFFFF);
        for (int i = 0; i < cards.size(); i++) {
            CardIconEntry entry = cards.get(i);
            int column = i % columns;
            int row = i / columns;
            int x = left + column * iconStep;
            int y = top + row * iconStep;
            ItemStack stack = entry.displayStack();
            HandCardRenderHelper.renderItemIcon(graphics, minecraft.font, stack, x, y, entry.count() <= stack.getMaxStackSize());
            if (entry.count() > stack.getMaxStackSize()) {
                Component count = Component.literal("x" + entry.count());
                graphics.text(minecraft.font, count, x + 17 - minecraft.font.width(count), y + 10, 0xFFFFFFFF, true);
            }
        }

        Component hint = Component.translatable("hud.astral_craft.hand_card_deck.hint");
        graphics.text(minecraft.font, hint, right - minecraft.font.width(hint), bottom + 4, 0xFFBFC8FF, true);
    }

    protected static List<CardIconEntry> inventoryCards(Minecraft minecraft) {
        Map<Identifier, CardIconEntry> entries = new LinkedHashMap<>();
        if (minecraft.player == null) return List.of();
        for (ItemStack stack : minecraft.player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !(stack.getItem() instanceof BaseHandCard)) continue;
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            CardIconEntry current = entries.get(id);
            if (current == null) {
                ItemStack display = stack.copy();
                display.setCount(Math.min(stack.getCount(), stack.getMaxStackSize()));
                entries.put(id, new CardIconEntry(id, display, stack.getCount()));
            } else {
                entries.put(id, current.withAdded(stack.getCount()));
            }
        }

        return new ArrayList<>(entries.values());
    }

    protected record CardIconEntry(Identifier id, ItemStack stack, int count) {

        public CardIconEntry withAdded(int amount) {
            return new CardIconEntry(this.id, this.stack, this.count + Math.max(0, amount));
        }

        public ItemStack displayStack() {
            int max = Math.max(1, this.stack.getMaxStackSize());
            ItemStack copy = this.stack.copy();
            copy.setCount(Math.max(1, Math.min(this.count, max)));
            return copy;
        }

    }

}
