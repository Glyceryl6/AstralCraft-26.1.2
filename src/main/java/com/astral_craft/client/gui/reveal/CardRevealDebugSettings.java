package com.astral_craft.client.gui.reveal;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Hot-reloadable development tuning file for card reveal animation.
 * The file is created at config/astral_craft/card_reveal.properties on first use.
 */
public class CardRevealDebugSettings {
    public Path path;
    public long lastLoadedModified = Long.MIN_VALUE;
    public long nextCheckNanos;

    public CardRevealDebugSettings() {
        this.path = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("astral_craft").resolve("card_reveal.properties");
    }

    public void tick(CardRevealSettings settings) {
        long now = System.nanoTime();
        if (now < this.nextCheckNanos) {
            return;
        }
        this.nextCheckNanos = now + 500_000_000L;
        this.createDefaultFile(settings);
        long modified = this.lastModified();
        if (modified != this.lastLoadedModified) {
            this.load(settings, modified);
        }
    }

    public void reloadNow(CardRevealSettings settings) {
        this.createDefaultFile(settings);
        this.load(settings, this.lastModified());
    }

    public void createDefaultFile(CardRevealSettings settings) {
        try {
            Files.createDirectories(this.path.getParent());
            if (Files.exists(this.path)) {
                return;
            }
            Properties properties = new Properties();
            this.writeDefaults(properties, settings);
            try (OutputStream output = Files.newOutputStream(this.path)) {
                properties.store(output, "AstralCraft card reveal debug settings. Edit while the game is running; changes hot-reload every 0.5s.");
            }
        } catch (IOException ignored) {
        }
    }

    public void writeDefaults(Properties properties, CardRevealSettings settings) {
        properties.setProperty("modelMinSize", Integer.toString(settings.modelMinSize));
        properties.setProperty("modelMaxSize", Integer.toString(settings.modelMaxSize));
        properties.setProperty("modelScreenHeightRatio", Float.toString(settings.modelScreenHeightRatio));
        properties.setProperty("modelScreenWidthRatio", Float.toString(settings.modelScreenWidthRatio));
        properties.setProperty("modelScreenMargin", Integer.toString(settings.modelScreenMargin));
        properties.setProperty("cardModelScale", Float.toString(settings.cardModelScale));
        properties.setProperty("cardTextScale", Float.toString(settings.cardTextScale));
        properties.setProperty("cardCenterYOffsetRatio", Float.toString(settings.cardCenterYOffsetRatio));
        properties.setProperty("cardCenterYOffsetPixels", Integer.toString(settings.cardCenterYOffsetPixels));
        properties.setProperty("cardFrameWidthRatio", Float.toString(settings.cardFrameWidthRatio));
        properties.setProperty("cardFrameHeightRatio", Float.toString(settings.cardFrameHeightRatio));
        properties.setProperty("cardFrameYOffsetRatio", Float.toString(settings.cardFrameYOffsetRatio));
        properties.setProperty("frontArtSizeRatio", Float.toString(settings.frontArtSizeRatio));
        properties.setProperty("frontArtYOffsetRatio", Float.toString(settings.frontArtYOffsetRatio));
        properties.setProperty("backArtWidthRatio", Float.toString(settings.backArtWidthRatio));
        properties.setProperty("backArtHeightRatio", Float.toString(settings.backArtHeightRatio));
        properties.setProperty("backArtYOffsetRatio", Float.toString(settings.backArtYOffsetRatio));
        properties.setProperty("titleTextMaxWidthRatio", Float.toString(settings.titleTextMaxWidthRatio));
        properties.setProperty("bodyTextMaxWidthRatio", Float.toString(settings.bodyTextMaxWidthRatio));
        properties.setProperty("minTitleTextWidth", Integer.toString(settings.minTitleTextWidth));
        properties.setProperty("minBodyTextWidth", Integer.toString(settings.minBodyTextWidth));
        properties.setProperty("maxTitleTextWidth", Integer.toString(settings.maxTitleTextWidth));
        properties.setProperty("maxBodyTextWidth", Integer.toString(settings.maxBodyTextWidth));
        properties.setProperty("titleYOffsetRatio", Float.toString(settings.titleYOffsetRatio));
        properties.setProperty("bodyYOffsetRatio", Float.toString(settings.bodyYOffsetRatio));
        properties.setProperty("titleExtraYOffsetRatio", Float.toString(settings.titleExtraYOffsetRatio));
        properties.setProperty("bodyExtraYOffsetRatio", Float.toString(settings.bodyExtraYOffsetRatio));
        properties.setProperty("titleExtraYOffsetPixels", Integer.toString(settings.titleExtraYOffsetPixels));
        properties.setProperty("bodyExtraYOffsetPixels", Integer.toString(settings.bodyExtraYOffsetPixels));
        properties.setProperty("bodyMaxLines", Integer.toString(settings.bodyMaxLines));
        properties.setProperty("textShadow", Boolean.toString(settings.textShadow));
        properties.setProperty("textBackdropAlpha", Float.toString(settings.textBackdropAlpha));
        properties.setProperty("itemScaleStep", Float.toString(settings.itemScaleStep));
        properties.setProperty("flipIntroHoldTicks", Integer.toString(settings.flipIntroHoldTicks));
        properties.setProperty("flipRotateTicks", Integer.toString(settings.flipRotateTicks));
        properties.setProperty("flipOutroHoldTicks", Integer.toString(settings.flipOutroHoldTicks));
        properties.setProperty("flipFadeTicks", Integer.toString(settings.flipFadeTicks));
        properties.setProperty("approachInTicks", Integer.toString(settings.approachInTicks));
        properties.setProperty("approachHoldTicks", Integer.toString(settings.approachHoldTicks));
        properties.setProperty("approachOutTicks", Integer.toString(settings.approachOutTicks));
    }

    public void load(CardRevealSettings settings, long modified) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(this.path)) {
            properties.load(input);
            settings.modelMinSize = this.intValue(properties, "modelMinSize", settings.modelMinSize);
            settings.modelMaxSize = this.intValue(properties, "modelMaxSize", settings.modelMaxSize);
            settings.modelScreenHeightRatio = this.floatValue(properties, "modelScreenHeightRatio", settings.modelScreenHeightRatio);
            settings.modelScreenWidthRatio = this.floatValue(properties, "modelScreenWidthRatio", settings.modelScreenWidthRatio);
            settings.modelScreenMargin = this.intValue(properties, "modelScreenMargin", settings.modelScreenMargin);
            settings.cardModelScale = this.floatValue(properties, "cardModelScale", settings.cardModelScale);
            settings.cardTextScale = this.floatValue(properties, "cardTextScale", settings.cardTextScale);
            settings.cardCenterYOffsetRatio = this.floatValue(properties, "cardCenterYOffsetRatio", settings.cardCenterYOffsetRatio);
            settings.cardCenterYOffsetPixels = this.intValue(properties, "cardCenterYOffsetPixels", settings.cardCenterYOffsetPixels);
            settings.cardFrameWidthRatio = this.floatValue(properties, "cardFrameWidthRatio", settings.cardFrameWidthRatio);
            settings.cardFrameHeightRatio = this.floatValue(properties, "cardFrameHeightRatio", settings.cardFrameHeightRatio);
            settings.cardFrameYOffsetRatio = this.floatValue(properties, "cardFrameYOffsetRatio", settings.cardFrameYOffsetRatio);
            settings.frontArtSizeRatio = this.floatValue(properties, "frontArtSizeRatio", settings.frontArtSizeRatio);
            settings.frontArtYOffsetRatio = this.floatValue(properties, "frontArtYOffsetRatio", settings.frontArtYOffsetRatio);
            settings.backArtWidthRatio = this.floatValue(properties, "backArtWidthRatio", settings.backArtWidthRatio);
            settings.backArtHeightRatio = this.floatValue(properties, "backArtHeightRatio", settings.backArtHeightRatio);
            settings.backArtYOffsetRatio = this.floatValue(properties, "backArtYOffsetRatio", settings.backArtYOffsetRatio);
            settings.titleTextMaxWidthRatio = this.floatValue(properties, "titleTextMaxWidthRatio", settings.titleTextMaxWidthRatio);
            settings.bodyTextMaxWidthRatio = this.floatValue(properties, "bodyTextMaxWidthRatio", settings.bodyTextMaxWidthRatio);
            settings.textMaxWidthRatio = settings.bodyTextMaxWidthRatio;
            settings.minTitleTextWidth = this.intValue(properties, "minTitleTextWidth", settings.minTitleTextWidth);
            settings.minBodyTextWidth = this.intValue(properties, "minBodyTextWidth", settings.minBodyTextWidth);
            settings.maxTitleTextWidth = this.intValue(properties, "maxTitleTextWidth", settings.maxTitleTextWidth);
            settings.maxBodyTextWidth = this.intValue(properties, "maxBodyTextWidth", settings.maxBodyTextWidth);
            settings.titleYOffsetRatio = this.floatValue(properties, "titleYOffsetRatio", settings.titleYOffsetRatio);
            settings.bodyYOffsetRatio = this.floatValue(properties, "bodyYOffsetRatio", settings.bodyYOffsetRatio);
            settings.titleExtraYOffsetRatio = this.floatValue(properties, "titleExtraYOffsetRatio", settings.titleExtraYOffsetRatio);
            settings.bodyExtraYOffsetRatio = this.floatValue(properties, "bodyExtraYOffsetRatio", settings.bodyExtraYOffsetRatio);
            settings.titleExtraYOffsetPixels = this.intValue(properties, "titleExtraYOffsetPixels", settings.titleExtraYOffsetPixels);
            settings.bodyExtraYOffsetPixels = this.intValue(properties, "bodyExtraYOffsetPixels", settings.bodyExtraYOffsetPixels);
            settings.bodyMaxLines = this.intValue(properties, "bodyMaxLines", settings.bodyMaxLines);
            settings.textShadow = this.booleanValue(properties, "textShadow", settings.textShadow);
            settings.textBackdropAlpha = this.floatValue(properties, "textBackdropAlpha", settings.textBackdropAlpha);
            settings.itemScaleStep = this.floatValue(properties, "itemScaleStep", settings.itemScaleStep);
            settings.flipIntroHoldTicks = this.intValue(properties, "flipIntroHoldTicks", settings.flipIntroHoldTicks);
            settings.flipRotateTicks = this.intValue(properties, "flipRotateTicks", settings.flipRotateTicks);
            settings.flipOutroHoldTicks = this.intValue(properties, "flipOutroHoldTicks", settings.flipOutroHoldTicks);
            settings.flipFadeTicks = this.intValue(properties, "flipFadeTicks", settings.flipFadeTicks);
            settings.approachInTicks = this.intValue(properties, "approachInTicks", settings.approachInTicks);
            settings.approachHoldTicks = this.intValue(properties, "approachHoldTicks", settings.approachHoldTicks);
            settings.approachOutTicks = this.intValue(properties, "approachOutTicks", settings.approachOutTicks);
            this.lastLoadedModified = modified;
        } catch (IOException ignored) {
        }
    }

    public long lastModified() {
        try {
            return Files.exists(this.path) ? Files.getLastModifiedTime(this.path).toMillis() : Long.MIN_VALUE;
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    public int intValue(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public float floatValue(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public boolean booleanValue(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key, Boolean.toString(fallback)).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value) || "1".equals(value) || "yes".equals(value)) {
            return true;
        }
        if ("false".equals(value) || "0".equals(value) || "no".equals(value)) {
            return false;
        }
        return fallback;
    }
}
