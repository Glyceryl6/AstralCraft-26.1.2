package com.astral_craft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class AstralDropdown {

    public static final int ROW_HEIGHT = 20;
    public static final int LABEL_GAP = 6;

    public record Entry(String key, MutableComponent label) {
        public static Entry of(String key, MutableComponent label) {
            return new Entry(key == null ? "" : key, label == null ? Component.empty() : label);
        }
    }

    public record Layout(int labelX, int labelY, int labelWidth, int x, int y, int width, int height) {
        public boolean containsButton(double mouseX, double mouseY) {
            return contains(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }

        public boolean containsMenu(double mouseX, double mouseY, int rows) {
            return rows > 0 && contains(mouseX, mouseY, this.x, this.y + this.height + 2, this.width, rows * ROW_HEIGHT);
        }
    }

    public static Layout layout(int x, int y, int labelWidth, int width) {
        return new Layout(x, y + 6, labelWidth, x + labelWidth + LABEL_GAP, y, width, ROW_HEIGHT);
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, MutableComponent label, List<Entry> entries, String selectedKey, Layout layout, boolean open, boolean hovered, AstralFancyButton.ButtonStyle style) {
        graphics.text(font, label, layout.labelX, layout.labelY, 0xFFE8E8F4, false);
        MutableComponent selectedText = labelFor(entries, selectedKey);
        MutableComponent title = Component.literal(selectedText.getString() + (open ? " ▲" : " ▼"));
        AstralFancyButton.button(title, layout.x, layout.y, layout.width, layout.height, style).render(graphics, font, open, hovered);
        if (open) {
            renderMenu(graphics, font, entries, selectedKey, layout, style);
        }
    }

    public static void renderMenu(GuiGraphicsExtractor graphics, Font font, List<Entry> entries, String selectedKey, Layout layout, AstralFancyButton.ButtonStyle style) {
        int rows = Math.min(entries.size(), maxVisibleRows(entries));
        int menuY = layout.y + layout.height + 2;
        AstralFancyButton.renderOutlinedBox(graphics, layout.x, menuY, layout.width, rows * ROW_HEIGHT, 0xF014141D, 0xFFFFFFFF, 0xFF101018, 1, 1);
        for (int i = 0; i < rows; i++) {
            Entry entry = entries.get(i);
            int rowY = menuY + i * ROW_HEIGHT;
            boolean selected = entry.key().equals(selectedKey);
            int fill = selected ? style.selectedBottomColor() : (i % 2 == 0 ? 0x22101018 : 0x33101018);
            if ((fill >>> 24) != 0) {
                graphics.fill(layout.x + 2, rowY + 1, layout.x + layout.width - 2, rowY + ROW_HEIGHT - 1, fill);
            }

            MutableComponent text = ellipsize(font, entry.label(), layout.width - 10);
            graphics.text(font, text, layout.x + 5, rowY + 6, selected ? 0xFFFFFFFF : 0xFFD8D8E8, false);
        }
    }

    public static String clickedEntry(List<Entry> entries, Layout layout, double mouseX, double mouseY) {
        int rows = Math.min(entries.size(), maxVisibleRows(entries));
        int menuY = layout.y + layout.height + 2;
        if (!layout.containsMenu(mouseX, mouseY, rows)) return null;
        int index = (int) ((mouseY - menuY) / ROW_HEIGHT);
        if (index < 0 || index >= rows) return null;
        return entries.get(index).key();
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static MutableComponent labelFor(List<Entry> entries, String key) {
        for (Entry entry : entries) {
            if (entry.key().equals(key == null ? "" : key)) {
                return entry.label();
            }
        }

        return entries.isEmpty() ? Component.empty() : entries.getFirst().label();
    }

    public static MutableComponent ellipsize(Font font, MutableComponent input, int maxWidth) {
        String text = input.getString();
        if (font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(out.toString()) + font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }

        return Component.literal(out + suffix);
    }

    private static int maxVisibleRows(List<Entry> entries) {
        return Math.clamp(entries.size(), 0, 8);
    }

}