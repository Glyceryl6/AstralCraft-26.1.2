package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.jpgloader.LoadedJpgTexture;
import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.common.components.CardType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class HandCardRenderHelper {

    public static final int FRAMED_CARD_W = Mth.ceil(44 * 1.5F);
    public static final int FRAMED_CARD_H = Mth.ceil(64 * 1.5F);
    public static final int FRAMED_ART_SIZE = 48;
    public static final int ITEM_ICON_SIZE = 16;

    protected static final Map<CardType, Identifier> FRAME_TEXTURES = new EnumMap<>(CardType.class);
    protected static final Identifier DEFAULT_FRAME_TEXTURE = AstralCraft.prefix("textures/item/template_handcard_effect.png");

    static {
        for (CardType type : CardType.values()) {
            FRAME_TEXTURES.put(type, AstralCraft.prefix("textures/item/template_handcard_" + type.getSerializedName() + ".png"));
        }
    }

    public static Identifier frameTexture(CardType type) {
        return FRAME_TEXTURES.getOrDefault(type, DEFAULT_FRAME_TEXTURE);
    }

    public static void renderFramedCard(GuiGraphicsExtractor graphics, Font font, CardType type, Identifier artTexture, Component name, int x, int y, int mouseX, int mouseY, boolean dragging) {
        boolean hovered = !dragging && mouseX >= x && mouseX <= x + FRAMED_CARD_W && mouseY >= y && mouseY <= y + FRAMED_CARD_H;
        Identifier frame = frameTexture(type);
        graphics.blit(RenderPipelines.GUI_TEXTURED, frame, x, y, 0.0F, 0.0F, FRAMED_CARD_W, FRAMED_CARD_H, 256, 360, 256, 360, 0xFFFFFFFF);
        if (artTexture != null) {
            try {
                LoadedJpgTexture loaded = ScopedJpgTextureCache.getOrLoad(artTexture);
                graphics.blit(RenderPipelines.GUI_TEXTURED, loaded.textureId(), x + 9, y + 10, 0.0F, 0.0F, FRAMED_ART_SIZE, FRAMED_ART_SIZE, 256, 256, loaded.width(), loaded.height(), 0xFFFFFFFF);
            } catch (IOException _) {}
        }

        Component trimmed = ellipsize(font, name, FRAMED_CARD_W - 8);
        graphics.text(font, trimmed, x + FRAMED_CARD_W / 2 - font.width(trimmed) / 2, y + FRAMED_CARD_H - 24, 0xFFFFFFFF, true);
        if (dragging || hovered) {
            graphics.fill(x, y, x + FRAMED_CARD_W, y + FRAMED_CARD_H, dragging ? 0x33FFFFFF : 0x22FFFFFF);
        }
    }

    public static void renderCardCount(GuiGraphicsExtractor graphics, Font font, int count, int x, int y) {
        if (count <= 1) return;
        Component countText = Component.literal("x" + count);
        int countX = x + FRAMED_CARD_W - font.width(countText) - 4;
        int countY = y + 4;
        graphics.fill(countX - 3, countY - 2, x + FRAMED_CARD_W - 2, countY + 10, 0xC0000000);
        graphics.text(font, countText, countX, countY, 0xFFFFF08A, true);
    }

    public static void renderItemIcon(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y, boolean decorations) {
        if (stack == null || stack.isEmpty()) return;
        graphics.item(stack, x, y);
        if (decorations) {
            graphics.itemDecorations(font, stack, x, y);
        }
    }

    public static Component ellipsize(Font font, Component input, int maxWidth) {
        String text = input.getString();
        if (font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(out.toString()) + font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }
        return Component.literal(out + suffix).withStyle(input.getStyle());
    }

}