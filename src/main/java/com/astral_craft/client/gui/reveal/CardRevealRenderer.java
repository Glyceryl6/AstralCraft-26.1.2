package com.astral_craft.client.gui.reveal;

import net.minecraft.client.Minecraft;
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

    public void renderCard(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int baseModelSize, CardRevealFrame frame) {
        int modelSize = Math.max(8, Math.round(baseModelSize * settings.cardModelScale * frame.cardScale()));
        int textSize = Math.max(8, Math.round(baseModelSize * settings.cardTextScale * frame.textScale()));
        int shiftedY = centerY + frame.centerYOffset() + settings.responsiveOffset(baseModelSize, settings.cardCenterYOffsetRatio, settings.cardCenterYOffsetPixels);
        this.renderCardModel(graphics, reveal, settings, centerX, shiftedY, modelSize, frame.alpha(), frame.front(), frame.widthScale());
        if (frame.front() && frame.renderText()) {
            this.renderCardText(graphics, reveal, settings, centerX, shiftedY, textSize, frame.alpha(), frame.widthScale(), frame.heightScale());
        }
    }

    public void renderCardModel(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int modelSize, float alpha, boolean front, float widthScale) {
        int frameW = Math.max(2, Math.round(modelSize * settings.cardFrameWidthRatio * widthScale));
        int frameH = Math.max(8, Math.round(modelSize * settings.cardFrameHeightRatio));
        int frameCenterY = centerY + Math.round(modelSize * settings.cardFrameYOffsetRatio);
        int shadowAlpha = (int) (alpha * 150.0F) & 0xFF;
        int shadowW = Math.max(4, Math.round(frameW * 0.90F));
        int shadowH = Math.max(4, Math.round(frameH * 0.92F));
        graphics.fill(centerX - shadowW / 2 - 7, frameCenterY - shadowH / 2 + 8, centerX + shadowW / 2 + 7, frameCenterY + shadowH / 2 + 10, shadowAlpha << 24);

        if (front) {
            this.renderCardTexture(graphics, this.frameTextureFor(reveal.cardType()), centerX, frameCenterY, frameW, frameH, alpha, 44, 64);
            this.renderFrontInsetArt(graphics, reveal.frontTexture(), settings, centerX, frameCenterY, modelSize, alpha, widthScale);
        } else {
            this.renderCardTexture(graphics, reveal.backTexture(), centerX, frameCenterY, frameW, frameH, alpha, 256, 360);
        }

        this.renderSideEdge(graphics, settings, centerX, frameCenterY, modelSize, alpha, widthScale);
    }

    public Identifier frameTextureFor(String cardType) {
        String type = cardType == null || cardType.isBlank() ? "effect" : cardType.toLowerCase(java.util.Locale.ROOT);
        return Identifier.fromNamespaceAndPath("astral_craft", "textures/item/template_handcard_" + type + ".png");
    }

    public void renderCardTexture(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int centerY, int width, int height, float alpha, int textureWidth, int textureHeight) {
        int left = centerX - width / 2;
        int top = centerY - height / 2;
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0.0F, 0.0F, width, height, textureWidth, textureHeight, textureWidth, textureHeight, argb);
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

    public void renderCardText(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int textSize, float alpha, float xScale, float yScale) {
        Font font = Minecraft.getInstance().font;
        int titleMaxTextWidth = this.textWidth(textSize, settings.titleTextMaxWidthRatio, settings.minTitleTextWidth, settings.maxTitleTextWidth);
        int bodyMaxTextWidth = this.textWidth(textSize, settings.bodyTextMaxWidthRatio, settings.minBodyTextWidth, settings.maxBodyTextWidth);
        int titleY = centerY + Math.round(textSize * settings.titleYOffsetRatio) + settings.responsiveOffset(textSize, settings.titleExtraYOffsetRatio, settings.titleExtraYOffsetPixels);
        int bodyY = centerY + Math.round(textSize * settings.bodyYOffsetRatio) + settings.responsiveOffset(textSize, settings.bodyExtraYOffsetRatio, settings.bodyExtraYOffsetPixels);
        int argbBody = (((int) (alpha * 255.0F) & 0xFF) << 24) | (settings.bodyTextColor & 0xFFFFFF);
        int argbTitle = (((int) (alpha * 255.0F) & 0xFF) << 24) | (settings.titleTextColor & 0xFFFFFF);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(xScale, yScale);
        graphics.pose().translate(-centerX, -centerY);
        String title = reveal.title().isBlank() ? reveal.cardId() : reveal.title();
        String shortTitle = this.ellipsize(font, title, titleMaxTextWidth);
        int titleWidth = Math.min(font.width(shortTitle), titleMaxTextWidth);
        this.renderTextBackdrop(graphics, settings, alpha, centerX - titleWidth / 2 - 4, titleY - 3, centerX + titleWidth / 2 + 4, titleY + 10);
        graphics.text(font, shortTitle, centerX - font.width(shortTitle) / 2, titleY, argbTitle, settings.textShadow);
        List<FormattedCharSequence> lines = this.wrappedLines(font, reveal.body(), bodyMaxTextWidth, settings.bodyMaxLines);
        int lineY = bodyY;
        for (FormattedCharSequence line : lines) {
            int lineW = font.width(line);
            int x = centerX - lineW / 2;
            this.renderTextBackdrop(graphics, settings, alpha, x - 2, lineY - 1, x + lineW + 2, lineY + 9);
            graphics.text(font, line, x, lineY, argbBody, settings.textShadow);
            lineY += 10;
        }

        graphics.pose().popMatrix();
    }

    public int textWidth(int textSize, float ratio, int minWidth, int maxWidth) {
        int computed = Math.round(textSize * ratio);
        return Mth.clamp(computed, Math.max(1, minWidth), Math.max(minWidth, maxWidth));
    }

    public void renderTextBackdrop(GuiGraphicsExtractor graphics, CardRevealSettings settings, float alpha, int left, int top, int right, int bottom) {
        if (settings.textBackdropAlpha <= 0.001F) return;
        int backdropAlpha = (int) (alpha * settings.textBackdropAlpha * 255.0F) & 0xFF;
        graphics.fill(left, top, right, bottom, backdropAlpha << 24);
    }

    public List<FormattedCharSequence> wrappedLines(Font font, String body, int maxWidth, int maxLines) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (String segment : body.split("\\n")) {
            if (result.size() >= maxLines) break;
            List<FormattedCharSequence> split = font.split(Component.literal(segment), maxWidth);
            for (FormattedCharSequence line : split) {
                result.add(line);
                if (result.size() >= maxLines) break;
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