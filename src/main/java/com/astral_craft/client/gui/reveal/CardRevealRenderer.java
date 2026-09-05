package com.astral_craft.client.gui.reveal;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.board.BoardScreenEntityRenderer;
import com.astral_craft.client.jpgloader.LoadedJpgTexture;
import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.text.AstralTextFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CardRevealRenderer {

    private static final Identifier DEFAULT_FRAME_TEXTURE = AstralCraft.prefix("textures/item/template_handcard_effect.png");
    private static final Identifier EVENT_FALLBACK_ART = AstralCraft.prefix("textures/block/platform_event.png");

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Font font = this.minecraft.font;

    public void renderCard(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int baseModelSize, CardRevealFrame frame) {
        int modelSize = Math.max(8, Math.round(baseModelSize * settings.cardModelScale * frame.cardScale()));
        int textSize = Math.max(8, Math.round(baseModelSize * settings.cardTextScale * frame.textScale()));
        int shiftedY = centerY + frame.centerYOffset() + settings.responsiveOffset(baseModelSize, settings.cardCenterYOffsetRatio, settings.cardCenterYOffsetPixels);
        this.renderCardModel(graphics, reveal, settings, centerX, shiftedY, modelSize, frame.alpha(), frame.front(), frame.widthScale());
        if (frame.front() && frame.renderText()) {
            this.renderCardText(graphics, reveal, settings, centerX, shiftedY, textSize, frame.alpha(), frame.widthScale(), frame.heightScale());
        }
        if (frame.front() && frame.widthScale() > 0.72F) {
            this.renderRelationship(graphics, reveal, settings, centerX, shiftedY, modelSize, frame.alpha(), frame.widthScale());
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
            Identifier backTexture = ScopedJpgTextureCache.resolveOrFallback(reveal.backTexture(), CardBackPreferenceManager.DEFAULT_TEXTURE);
            this.renderCardTexture(graphics, backTexture, centerX, frameCenterY, frameW, frameH, alpha, 256, 360);
        }

        this.renderSideEdge(graphics, settings, centerX, frameCenterY, modelSize, alpha, widthScale);
    }


    protected void renderRelationship(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings,
                                      int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        if (!reveal.showRelationship() || reveal.sourceEntityId() < 0 || this.minecraft.level == null) return;
        Entity source = this.minecraft.level.getEntity(reveal.sourceEntityId());
        if (!(source instanceof LivingEntity sourceLiving)) return;
        List<LivingEntity> targets = new ArrayList<>();
        for (int targetId : reveal.targetEntityIds()) {
            Entity target = this.minecraft.level.getEntity(targetId);
            if (target instanceof LivingEntity living && targets.stream().noneMatch(value -> value.getId() == living.getId())) {
                targets.add(living);
            }
            if (targets.size() >= 2) break;
        }

        if (targets.isEmpty()) return;
        boolean selfOnly = targets.size() == 1 && targets.getFirst().getId() == sourceLiving.getId();
        int boxSize = Mth.clamp(Math.round(modelSize * 0.17F), 24, 42);
        int gap = Math.max(5, boxSize / 5);
        int arrowWidth = Math.max(18, this.font.width("→") + 8);
        int targetColumns = targets.size();
        int totalWidth = selfOnly ? boxSize
                : boxSize + gap + arrowWidth + gap + boxSize * targetColumns + gap * Math.max(0, targetColumns - 1);
        int frameHeight = Math.max(8, Math.round(modelSize * settings.cardFrameHeightRatio));
        int frameCenterY = centerY + Math.round(modelSize * settings.cardFrameYOffsetRatio);
        int top = frameCenterY - frameHeight / 2 - boxSize - Math.max(8, modelSize / 24);
        top = Math.max(5, top);
        int left = Mth.clamp(centerX - totalWidth / 2, 5,
                Math.max(5, this.minecraft.getWindow().getGuiScaledWidth() - totalWidth - 5));
        this.renderRelationshipEntity(graphics, sourceLiving, left, top, boxSize, alpha);
        if (selfOnly) return;
        int arrowX = left + boxSize + gap;
        int color = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xE5E5E5;
        graphics.text(this.font, Component.literal("→"), arrowX + (arrowWidth - this.font.width("→")) / 2,
                top + boxSize / 2 - 4, color, true);
        int targetX = arrowX + arrowWidth + gap;
        for (LivingEntity target : targets) {
            this.renderRelationshipEntity(graphics, target, targetX, top, boxSize, alpha);
            targetX += boxSize + gap;
        }
    }

    protected void renderRelationshipEntity(GuiGraphicsExtractor graphics, LivingEntity entity, int x, int y, int size, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, (a << 24) | 0x77777F);
        graphics.fill(x, y, x + size, y + size, (a << 24) | 0x090A0E);
        boolean rendered = false;
        if (entity instanceof AstralCharacterEntity character) {
            rendered = AstralStatusIconRenderer.renderCharacterSkinHead(graphics, character.characterId(),
                    character.skinId(), x + 2, y + 2, size - 4, a);
        } else if (entity instanceof Player player) {
            ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
            if (state.active()) {
                rendered = AstralStatusIconRenderer.renderCharacterSkinHead(graphics, state.characterId(),
                        state.skinId(), x + 2, y + 2, size - 4, a);
            }
        }

        if (!rendered) {
            BoardScreenEntityRenderer.render(graphics, entity, x + 1, y + 1, x + size - 1, y + size - 1, 180.0F);
        }
    }

    public Identifier frameTextureFor(String cardType) {
        String type = cardType == null || cardType.isBlank() ? "effect" : cardType.toLowerCase(Locale.ROOT);
        Identifier texture = AstralCraft.prefix("textures/item/template_handcard_" + type + ".png");
        return ScopedJpgTextureCache.resolveOrFallback(texture, DEFAULT_FRAME_TEXTURE);
    }

    public void renderCardTexture(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int centerY, int width, int height, float alpha, int textureWidth, int textureHeight) {
        int left = centerX - width / 2;
        int top = centerY - height / 2;
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        Identifier resolvedTexture = ScopedJpgTextureCache.resolveOrFallback(texture, DEFAULT_FRAME_TEXTURE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, resolvedTexture, left, top, 0.0F, 0.0F, width, height, textureWidth, textureHeight, textureWidth, textureHeight, argb);
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
        int left = centerX - artW / 2;
        int top = centerY - artSize / 2 + Math.round(modelSize * settings.frontArtYOffsetRatio);
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        try {
            LoadedJpgTexture loaded = ScopedJpgTextureCache.getOrLoad(texture);
            graphics.blit(RenderPipelines.GUI_TEXTURED, loaded.textureId(), left, top, 0.0F, 0.0F, artW, artSize, loaded.width(), loaded.height(), loaded.width(), loaded.height(), argb);
        } catch (IOException _) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, EVENT_FALLBACK_ART, left, top, 0.0F, 0.0F, artW, artSize, 32, 32, 32, 32, argb);
        }
    }

    public void renderCardText(GuiGraphicsExtractor graphics, CardReveal reveal, CardRevealSettings settings, int centerX, int centerY, int textSize, float alpha, float xScale, float yScale) {
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
        String title = reveal.title().getString();
        String shortTitle = this.ellipsize(this.font, title, titleMaxTextWidth);
        int titleWidth = Math.min(this.font.width(shortTitle), titleMaxTextWidth);
        this.renderTextBackdrop(graphics, settings, alpha, centerX - titleWidth / 2 - 4, titleY - 3, centerX + titleWidth / 2 + 4, titleY + 10);
        graphics.text(this.font, shortTitle, centerX - this.font.width(shortTitle) / 2, titleY, argbTitle, settings.textShadow);
        List<FormattedCharSequence> lines = this.wrappedLines(this.font, reveal.body(), bodyMaxTextWidth, settings.bodyMaxLines);
        int lineY = bodyY + 5;
        for (FormattedCharSequence line : lines) {
            int lineW = this.font.width(line);
            int x = centerX - lineW / 2;
            this.renderTextBackdrop(graphics, settings, alpha, x - 2, lineY - 1, x + lineW + 2, lineY + 9);
            graphics.text(this.font, line, x, lineY, argbBody, settings.textShadow);
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

    public List<FormattedCharSequence> wrappedLines(Font font, Component body, int maxWidth, int maxLines) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (Component segment : AstralTextFormatter.lines(body)) {
            if (result.size() >= maxLines) break;
            List<FormattedCharSequence> split = font.split(segment, maxWidth);
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