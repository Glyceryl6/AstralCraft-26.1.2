package com.astral_craft.common.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Server-authoritative gameplay knobs.  These values deliberately live on the
 * logical server so client-side animation/debug files cannot shorten reveal
 * locks or bypass card restrictions on multiplayer servers.
 */
public class AstralGameplayConfig {

    public static final int DEFAULT_CARD_REVEAL_LOCK_TICKS = 60;
    public static final int DEFAULT_EVENT_REVEAL_LOCK_TICKS = 60;
    public static final String DEFAULT_CARD_MODE = "pvp";

    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("astral_craft").resolve("gameplay.properties");
    private static long lastModified = Long.MIN_VALUE;
    private static int cardRevealLockTicks = DEFAULT_CARD_REVEAL_LOCK_TICKS;
    private static int eventRevealLockTicks = DEFAULT_EVENT_REVEAL_LOCK_TICKS;
    private static String defaultCardMode = DEFAULT_CARD_MODE;

    public static int cardRevealLockTicks() {
        reloadIfChanged();
        return cardRevealLockTicks;
    }

    public static int eventRevealLockTicks() {
        reloadIfChanged();
        return eventRevealLockTicks;
    }

    public static String defaultCardMode() {
        reloadIfChanged();
        return defaultCardMode;
    }

    public static void reloadIfChanged() {
        createDefaultFile();
        long modified = lastModified();
        if (modified != lastModified) {
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
            properties.setProperty("cardRevealLockTicks", Integer.toString(DEFAULT_CARD_REVEAL_LOCK_TICKS));
            properties.setProperty("eventRevealLockTicks", Integer.toString(DEFAULT_EVENT_REVEAL_LOCK_TICKS));
            properties.setProperty("defaultCardMode", DEFAULT_CARD_MODE);
            try (OutputStream output = Files.newOutputStream(PATH)) {
                properties.store(output, "AstralCraft server gameplay settings.  Values are read on the logical server and hot-reload when this file changes.");
            }
        } catch (IOException ignored) {
        }
    }

    private static void load(long modified) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(PATH)) {
            properties.load(input);
            cardRevealLockTicks = intValue(properties, "cardRevealLockTicks", DEFAULT_CARD_REVEAL_LOCK_TICKS, 0, 20 * 60);
            eventRevealLockTicks = intValue(properties, "eventRevealLockTicks", DEFAULT_EVENT_REVEAL_LOCK_TICKS, 0, 20 * 60);
            defaultCardMode = normalizeMode(properties.getProperty("defaultCardMode", DEFAULT_CARD_MODE));
        } catch (IOException ignored) {
            cardRevealLockTicks = DEFAULT_CARD_REVEAL_LOCK_TICKS;
            eventRevealLockTicks = DEFAULT_EVENT_REVEAL_LOCK_TICKS;
            defaultCardMode = DEFAULT_CARD_MODE;
        } finally {
            lastModified = modified;
        }
    }

    private static int intValue(Properties properties, String key, int fallback, int min, int max) {
        try {
            int parsed = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
            return Math.clamp(parsed, min, max);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalizeMode(String raw) {
        String value = raw == null ? DEFAULT_CARD_MODE : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "pve", "coop", "cooperation" -> "pve";
            case "pvp", "versus", "standard" -> "pvp";
            default -> DEFAULT_CARD_MODE;
        };
    }

    private static long lastModified() {
        try {
            return Files.exists(PATH) ? Files.getLastModifiedTime(PATH).toMillis() : Long.MIN_VALUE;
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }
}
