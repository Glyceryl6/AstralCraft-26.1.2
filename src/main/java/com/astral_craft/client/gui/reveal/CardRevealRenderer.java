package com.astral_craft.client.gui.reveal;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class CardRevealRenderer {

    public void renderCard(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int modelSize, CardRevealFrame frame) {
        this.renderCardModel(graphics, reveal, settings, centerX, centerY + frame.centerYOffset(), modelSize, frame.alpha(), frame.front(), frame.xScale());
        if (frame.front() && frame.renderText()) {
            this.renderCardText(graphics, reveal, centerX, centerY + frame.centerYOffset(), modelSize, frame.alpha(), frame.xScale(), frame.yScale());
        }
    }

    public void renderCardModel(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int modelSize, float alpha, boolean front, float widthScale) {
        float itemScale = modelSize / settings.itemGuiBaseSize;
        int shadowAlpha = (int) (alpha * 150.0F) & 0xFF;
        int shadowW = Math.max(4, Math.round(modelSize * 0.54F * widthScale));
        int shadowH = Math.max(4, Math.round(modelSize * 0.84F));
        graphics.fill(centerX - shadowW / 2 - 7, centerY - shadowH / 2 + 8, centerX + shadowW / 2 + 7, centerY + shadowH / 2 + 10, (shadowAlpha << 24));
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(widthScale * itemScale, itemScale);
        graphics.pose().translate(-settings.itemGuiBaseSize / 2.0F, -settings.itemGuiBaseSize / 2.0F);
        if (!reveal.stack().isEmpty()) {
            graphics.fakeItem(reveal.stack(), 0, 0);
        }

        graphics.pose().popMatrix();
        this.renderSideEdge(graphics, settings, centerX, centerY, modelSize, alpha, widthScale);
        if (front) {
            this.renderFrontInsetArt(graphics, reveal.frontTexture(), settings, centerX, centerY, modelSize, alpha, widthScale);
        } else {
            this.renderBackInsetArt(graphics, reveal.backTexture(), settings, centerX, centerY, modelSize, alpha, widthScale);
        }
    }

    public void renderSideEdge(GuiGraphicsExtractor graphics, CardRevealSettings settings, int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        float edgeT = Mth.clamp((0.82F - widthScale) / 0.82F, 0.0F, 1.0F);
        if (edgeT <= 0.001F) return;
        int edgeAlpha = (int) (alpha * edgeT * 230.0F) & 0xFF;
        int edgeW = Mth.clamp(Math.round(modelSize * settings.sideEdgeWidthRatio * (0.45F + edgeT)), 3, 26);
        int edgeH = Math.max(24, Math.round(modelSize * settings.sideEdgeHeightRatio));
        int left = centerX - edgeW / 2;
        int right = left + edgeW;
        int top = centerY - edgeH / 2;
        int bottom = centerY + edgeH / 2;
        int dark = (edgeAlpha << 24) | 0x3A2B45;
        int mid = (edgeAlpha << 24) | 0x8F74C8;
        int light = (edgeAlpha << 24) | 0xD8CAFF;
        graphics.fill(left, top, right, bottom, dark);
        graphics.fill(left + 1, top + 2, left + Math.max(2, edgeW / 3), bottom - 2, mid);
        graphics.fill(right - Math.max(2, edgeW / 4), top + 4, right - 1, bottom - 4, light);
        graphics.fill(left, top, right, top + 2, light);
        graphics.fill(left, bottom - 2, right, bottom, dark);
    }

    public void renderFrontInsetArt(GuiGraphicsExtractor graphics, Identifier texture, CardRevealSettings settings, int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        int artSize = Math.max(18, Math.round(modelSize * settings.frontArtSizeRatio));
        int artW = Math.max(2, Math.round(artSize * widthScale));
        int artH = artSize;
        int left = centerX - artW / 2;
        int top = centerY - artH / 2 + Math.round(modelSize * settings.frontArtYOffsetRatio);
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0.0F, 0.0F, artW, artH, 256, 256, 256, 256, argb);
    }

    public void renderBackInsetArt(GuiGraphicsExtractor graphics, Identifier texture, CardRevealSettings settings, int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        int artH = Math.max(18, Math.round(modelSize * settings.backArtHeightRatio));
        int artW = Math.max(2, Math.round(modelSize * settings.backArtWidthRatio * widthScale));
        int left = centerX - artW / 2;
        int top = centerY - artH / 2 + Math.round(modelSize * settings.backArtYOffsetRatio);
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0.0F, 0.0F, artW, artH, 256, 360, 256, 360, argb);
    }

    public void renderCardText(GuiGraphicsExtractor graphics, CardReveal reveal, int centerX, int centerY, int modelSize, float alpha, float xScale, float yScale) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int maxTextWidth = Math.max(58, Math.round(modelSize * 0.46F));
        int titleY = centerY - Math.round(modelSize * 0.32F);
        int bodyY = centerY + Math.round(modelSize * 0.22F);
        int argbWhite = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        int argbTitle = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFF0B0;
        int argbShadow = (((int) (alpha * 135.0F) & 0xFF) << 24);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(xScale, yScale);
        graphics.pose().translate(-centerX, -centerY);
        String title = reveal.title().isBlank() ? reveal.cardId() : reveal.title();
        String shortTitle = this.ellipsize(font, title, maxTextWidth);
        int titleWidth = Math.min(font.width(shortTitle), maxTextWidth);
        graphics.fill(centerX - titleWidth / 2 - 4, titleY - 3, centerX + titleWidth / 2 + 4, titleY + 10, argbShadow);
        graphics.text(font, shortTitle, centerX - font.width(shortTitle) / 2, titleY, argbTitle, true);
        List<FormattedCharSequence> lines = this.wrappedLines(font, reveal.body(), maxTextWidth, 5);
        int lineY = bodyY;
        for (FormattedCharSequence line : lines) {
            int lineW = font.width(line);
            int x = centerX - lineW / 2;
            graphics.fill(x - 2, lineY - 1, x + lineW + 2, lineY + 9, argbShadow);
            graphics.text(font, line, x, lineY, argbWhite, false);
            lineY += 10;
        }

        graphics.pose().popMatrix();
    }

    public List<FormattedCharSequence> wrappedLines(Font font, String body, int maxWidth, int maxLines) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (String segment : body.split("\\n")) {
            if (result.size() >= maxLines) break;
            List<FormattedCharSequence> split = font.split(Component.literal(segment), maxWidth);
            for (FormattedCharSequence line : split) {
                result.add(line);
                if (result.size() >= maxLines) {
                    break;
                }
            }
        }

        return result;
    }

    public String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString()) + font.width(suffix) >= maxWidth) break;
            builder.append(text.charAt(i));
        }

        return builder + suffix;
    }

}