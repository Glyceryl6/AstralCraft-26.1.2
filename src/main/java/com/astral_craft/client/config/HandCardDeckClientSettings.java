package com.astral_craft.client.config;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Client-only visual preference for the bottom hand-card deck. */
public final class HandCardDeckClientSettings {

    public enum LayoutMode {
        FAN,
        CLASSIC;

        public static LayoutMode parse(String raw) {
            String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "classic", "line", "row", "old" -> CLASSIC;
                default -> FAN;
            };
        }
    }

    private static final Path PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("astral_craft").resolve("hand_card_deck.properties");
    private static long lastLoadedModified = Long.MIN_VALUE;
    private static long nextCheckNanos;
    private static LayoutMode layoutMode = LayoutMode.FAN;

    private HandCardDeckClientSettings() {}

    public static LayoutMode layoutMode() {
        tick();
        return layoutMode;
    }

    public static boolean fanLayout() {
        return layoutMode() == LayoutMode.FAN;
    }

    public static void tick() {
        long now = System.nanoTime();
        if (now < nextCheckNanos) {
            return;
        }
        nextCheckNanos = now + 500_000_000L;
        createDefaultFile();
        long modified = lastModified();
        if (modified != lastLoadedModified) {
            load(modified);
        }
    }

    private static void createDefaultFile() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                return;
            }
            Properties properties = new Properties();
            properties.setProperty("layout", "fan");
            try (OutputStream output = Files.newOutputStream(PATH)) {
                properties.store(output, "AstralCraft hand card deck client settings.  layout=fan or layout=classic");
            }
        } catch (IOException ignored) {
        }
    }

    private static void load(long modified) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(PATH)) {
            properties.load(input);
            layoutMode = LayoutMode.parse(properties.getProperty("layout", "fan"));
        } catch (IOException ignored) {
            layoutMode = LayoutMode.FAN;
        } finally {
            lastLoadedModified = modified;
        }
    }

    private static long lastModified() {
        try {
            return Files.exists(PATH) ? Files.getLastModifiedTime(PATH).toMillis() : Long.MIN_VALUE;
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }
}
