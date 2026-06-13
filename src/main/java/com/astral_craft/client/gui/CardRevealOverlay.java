package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.CardRevealPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Totem-like reveal for playable effect cards.
 *
 * <p>The reveal uses the actual card item model as the visible card frame. The detailed card art is
 * drawn as an inset layer inside that frame. GUI item rendering only gives us 2D matrix transforms,
 * so the flip is implemented as a squash flip plus an explicit edge band that makes the card
 * thickness readable while the card is side-on. If you later move this to a custom 3D PiP renderer,
 * keep the timing and texture conventions from this class.</p>
 */
public class CardRevealOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("card_reveal_overlay");

    /** Default timings. Reduce {@link #flipTicks} to make the card turn to the front faster. */
    public static final int DEFAULT_INTRO_HOLD_TICKS = 30;
    public static final int DEFAULT_FLIP_TICKS = 20;
    public static final int DEFAULT_OUTRO_HOLD_TICKS = 38;
    private static final int EXTRA_FADE_TICKS = 10;
    private static final long TICK_NANOS = 50_000_000L;

    /** Mutable so a client config screen can tune the animation without touching payloads. */
    private static int introHoldTicks = DEFAULT_INTRO_HOLD_TICKS;
    private static int flipTicks = DEFAULT_FLIP_TICKS;
    private static int outroHoldTicks = DEFAULT_OUTRO_HOLD_TICKS;

    /** Item renderers render into a 16x16 GUI box; this scales that box into the large reveal. */
    private static final float ITEM_GUI_BASE_SIZE = 16.0F;

    /** Front art is a square detail image. Tweak only these when the front detail needs moving/resizing. */
    private static final float FRONT_ART_SIZE_RATIO = 0.54F;
    private static final float FRONT_ART_Y_OFFSET_RATIO = -0.040F;

    /** Back art uses its own proportions so it is not coupled to the square front art. */
    private static final float BACK_ART_WIDTH_RATIO = 0.58F;
    private static final float BACK_ART_HEIGHT_RATIO = 0.80F;
    private static final float BACK_ART_Y_OFFSET_RATIO = 0.010F;

    /** Fake side thickness for the side-on part of the flip. Increase if the edge is still too subtle. */
    private static final float SIDE_EDGE_WIDTH_RATIO = 0.070F;
    private static final float SIDE_EDGE_HEIGHT_RATIO = 0.865F;

    private static Reveal active;

    public static void configureTimings(int introHold, int flip, int outroHold) {
        introHoldTicks = Math.max(0, introHold);
        flipTicks = Math.max(1, flip);
        outroHoldTicks = Math.max(0, outroHold);
    }

    public static int defaultDurationTicks() {
        return introHoldTicks + flipTicks + outroHoldTicks + EXTRA_FADE_TICKS;
    }

    public static void show(CardRevealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> show(payload));
    }

    public static void show(CardRevealPayload payload) {
        int duration = payload.durationTicks() > 0 ? payload.durationTicks() : defaultDurationTicks();
        active = new Reveal(payload.cardId(),
                Component.translatable(payload.titleKey()).getString(),
                Component.translatable(payload.bodyKey()).getString(),
                makeItemStack(payload.itemId()),
                Identifier.parse(payload.largeFrontTexture()),
                Identifier.parse(payload.largeBackTexture()),
                System.nanoTime(), duration);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) {
            return;
        }

        float ageTicks = (System.nanoTime() - active.startedAtNanos()) / (float) TICK_NANOS;
        if (ageTicks >= active.durationTicks()) {
            active = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            active = null;
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int modelSize = Mth.clamp((int) (graphics.guiHeight() * 0.62F), 150, 292);
        float alpha = fade(ageTicks, active.durationTicks());
        FlipFrame frame = flipFrame(ageTicks);
        renderCardModel(graphics, active, centerX, centerY, modelSize, alpha, frame);
        if (frame.front() && frame.widthScale() > 0.33F) {
            renderCardText(graphics, minecraft.font, active, centerX, centerY, modelSize, alpha, frame.widthScale());
        }
    }

    private static ItemStack makeItemStack(String itemId) {
        try {
            Identifier id = Identifier.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            return new ItemStack(item);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static void renderCardModel(GuiGraphicsExtractor graphics, Reveal reveal, int centerX, int centerY, int modelSize, float alpha, FlipFrame frame) {
        float itemScale = modelSize / ITEM_GUI_BASE_SIZE;
        int shadowAlpha = (int) (alpha * 150.0F) & 0xFF;
        int shadowW = Math.max(4, Math.round(modelSize * 0.54F * frame.widthScale()));
        int shadowH = Math.max(4, Math.round(modelSize * 0.84F));
        graphics.fill(centerX - shadowW / 2 - 7, centerY - shadowH / 2 + 8, centerX + shadowW / 2 + 7, centerY + shadowH / 2 + 10, (shadowAlpha << 24));
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(frame.widthScale() * itemScale, itemScale);
        graphics.pose().translate(-ITEM_GUI_BASE_SIZE / 2.0F, -ITEM_GUI_BASE_SIZE / 2.0F);
        if (!reveal.stack().isEmpty()) {
            graphics.fakeItem(reveal.stack(), 0, 0);
        }

        graphics.pose().popMatrix();
        renderSideEdge(graphics, centerX, centerY, modelSize, alpha, frame.widthScale());
        if (frame.front()) {
            renderFrontInsetArt(graphics, reveal.frontTexture(), centerX, centerY, modelSize, alpha, frame.widthScale());
        } else {
            renderBackInsetArt(graphics, reveal.backTexture(), centerX, centerY, modelSize, alpha, frame.widthScale());
        }
    }

    private static void renderSideEdge(GuiGraphicsExtractor graphics, int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        float edgeT = Mth.clamp((0.82F - widthScale) / 0.82F, 0.0F, 1.0F);
        if (edgeT <= 0.001F) return;
        int edgeAlpha = (int) (alpha * edgeT * 230.0F) & 0xFF;
        int edgeW = Mth.clamp(Math.round(modelSize * SIDE_EDGE_WIDTH_RATIO * (0.45F + edgeT)), 3, 26);
        int edgeH = Math.max(24, Math.round(modelSize * SIDE_EDGE_HEIGHT_RATIO));
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

    private static void renderFrontInsetArt(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        int artSize = Math.max(18, Math.round(modelSize * FRONT_ART_SIZE_RATIO));
        int artW = Math.max(2, Math.round(artSize * widthScale));
        int artH = artSize;
        int left = centerX - artW / 2;
        int top = centerY - artH / 2 + Math.round(modelSize * FRONT_ART_Y_OFFSET_RATIO);
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0.0F, 0.0F, artW, artH, 256, 256, 256, 256, argb);
    }

    private static void renderBackInsetArt(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int centerY, int modelSize, float alpha, float widthScale) {
        int artH = Math.max(18, Math.round(modelSize * BACK_ART_HEIGHT_RATIO));
        int artW = Math.max(2, Math.round(modelSize * BACK_ART_WIDTH_RATIO * widthScale));
        int left = centerX - artW / 2;
        int top = centerY - artH / 2 + Math.round(modelSize * BACK_ART_Y_OFFSET_RATIO);
        int argb = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0.0F, 0.0F, artW, artH, 256, 360, 256, 360, argb);
    }

    private static void renderCardText(GuiGraphicsExtractor graphics, Font font, Reveal reveal, int centerX, int centerY, int modelSize, float alpha, float squash) {
        int maxTextWidth = Math.max(58, Math.round(modelSize * 0.46F * squash));
        int titleY = centerY - Math.round(modelSize * 0.32F);
        int bodyY = centerY + Math.round(modelSize * 0.22F);
        int argbWhite = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFFFFF;
        int argbTitle = (((int) (alpha * 255.0F) & 0xFF) << 24) | 0xFFF0B0;
        int argbShadow = (((int) (alpha * 135.0F) & 0xFF) << 24);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(squash, 1.0F);
        graphics.pose().translate(-centerX, -centerY);
        String title = reveal.title().isBlank() ? reveal.cardId() : reveal.title();
        String shortTitle = ellipsize(font, title, maxTextWidth);
        int titleWidth = Math.min(font.width(shortTitle), maxTextWidth);
        graphics.fill(centerX - titleWidth / 2 - 4, titleY - 3, centerX + titleWidth / 2 + 4, titleY + 10, argbShadow);
        graphics.text(font, shortTitle, centerX - font.width(shortTitle) / 2, titleY, argbTitle, true);
        List<FormattedCharSequence> lines = wrappedLines(font, reveal.body(), maxTextWidth, 5);
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

    private static List<FormattedCharSequence> wrappedLines(Font font, String body, int maxWidth, int maxLines) {
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

    private static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString()) + font.width(suffix) >= maxWidth) break;
            builder.append(text.charAt(i));
        }

        return builder + suffix;
    }

    private static float fade(float ageTicks, int durationTicks) {
        float in = Mth.clamp(ageTicks / 10.0F, 0.0F, 1.0F);
        float out = Mth.clamp((durationTicks - ageTicks) / 14.0F, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    private static FlipFrame flipFrame(float ageTicks) {
        if (ageTicks < introHoldTicks) {
            return new FlipFrame(false, 1.0F);
        }

        if (ageTicks < introHoldTicks + flipTicks) {
            float t = (ageTicks - introHoldTicks) / flipTicks;
            float eased = easeInOut(t);
            float widthScale = Math.max(0.035F, Math.abs(Mth.cos(eased * Mth.PI)));
            return new FlipFrame(eased >= 0.5F, widthScale);
        }

        return new FlipFrame(true, 1.0F);
    }

    private static float easeInOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private record FlipFrame(boolean front, float widthScale) {}

    private record Reveal(String cardId, String title, String body, ItemStack stack, Identifier frontTexture, Identifier backTexture, long startedAtNanos, int durationTicks) {}
}
