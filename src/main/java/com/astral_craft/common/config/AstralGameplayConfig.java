package com.astral_craft.common.config;

import com.astral_craft.common.gameplay.character.skill.CharacterSkillCutinAudience;
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
    public static final int DEFAULT_SKILL_CUTIN_DURATION_TICKS = 64;
    public static final CharacterSkillCutinAudience DEFAULT_SKILL_CUTIN_AUDIENCE = CharacterSkillCutinAudience.OWNER_ONLY;
    public static final int DEFAULT_SKILL_COOLDOWN_SECONDS_PER_ROUND = 45;
    public static final int DEFAULT_SKILL_MINIMUM_COOLDOWN_SECONDS = 3;
    public static final int DEFAULT_SKILL_MAXIMUM_COOLDOWN_SECONDS = 600;

    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("astral_craft").resolve("gameplay.properties");
    private static long lastModified = Long.MIN_VALUE;
    private static int cardRevealLockTicks = DEFAULT_CARD_REVEAL_LOCK_TICKS;
    private static int eventRevealLockTicks = DEFAULT_EVENT_REVEAL_LOCK_TICKS;
    private static String defaultCardMode = DEFAULT_CARD_MODE;
    private static int skillCutinDurationTicks = DEFAULT_SKILL_CUTIN_DURATION_TICKS;
    private static CharacterSkillCutinAudience skillCutinAudience = DEFAULT_SKILL_CUTIN_AUDIENCE;
    private static int skillCooldownSecondsPerRound = DEFAULT_SKILL_COOLDOWN_SECONDS_PER_ROUND;
    private static int skillMinimumCooldownSeconds = DEFAULT_SKILL_MINIMUM_COOLDOWN_SECONDS;
    private static int skillMaximumCooldownSeconds = DEFAULT_SKILL_MAXIMUM_COOLDOWN_SECONDS;


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

    public static int skillCutinDurationTicks() {
        reloadIfChanged();
        return skillCutinDurationTicks;
    }

    public static CharacterSkillCutinAudience skillCutinAudience() {
        reloadIfChanged();
        return skillCutinAudience;
    }

    public static int skillCooldownSecondsPerRound() {
        reloadIfChanged();
        return skillCooldownSecondsPerRound;
    }

    public static int skillMinimumCooldownSeconds() {
        reloadIfChanged();
        return skillMinimumCooldownSeconds;
    }

    public static int skillMaximumCooldownSeconds() {
        reloadIfChanged();
        return skillMaximumCooldownSeconds;
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
            properties.setProperty("skillCutinDurationTicks", Integer.toString(DEFAULT_SKILL_CUTIN_DURATION_TICKS));
            properties.setProperty("skillCutinAudience", DEFAULT_SKILL_CUTIN_AUDIENCE.serializedName());
            properties.setProperty("skillCooldownSecondsPerRound", Integer.toString(DEFAULT_SKILL_COOLDOWN_SECONDS_PER_ROUND));
            properties.setProperty("skillMinimumCooldownSeconds", Integer.toString(DEFAULT_SKILL_MINIMUM_COOLDOWN_SECONDS));
            properties.setProperty("skillMaximumCooldownSeconds", Integer.toString(DEFAULT_SKILL_MAXIMUM_COOLDOWN_SECONDS));
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
            skillCutinDurationTicks = intValue(properties, "skillCutinDurationTicks", DEFAULT_SKILL_CUTIN_DURATION_TICKS, 0, 20 * 30);
            skillCutinAudience = normalizeAudience(properties.getProperty("skillCutinAudience", DEFAULT_SKILL_CUTIN_AUDIENCE.serializedName()));
            skillCooldownSecondsPerRound = intValue(properties, "skillCooldownSecondsPerRound", DEFAULT_SKILL_COOLDOWN_SECONDS_PER_ROUND, 1, 60 * 60);
            skillMinimumCooldownSeconds = intValue(properties, "skillMinimumCooldownSeconds", DEFAULT_SKILL_MINIMUM_COOLDOWN_SECONDS, 0, 60 * 60);
            skillMaximumCooldownSeconds = intValue(properties, "skillMaximumCooldownSeconds", DEFAULT_SKILL_MAXIMUM_COOLDOWN_SECONDS, 1, 60 * 60);
            if (skillMaximumCooldownSeconds < skillMinimumCooldownSeconds) {
                skillMaximumCooldownSeconds = skillMinimumCooldownSeconds;
            }
        } catch (IOException ignored) {
            cardRevealLockTicks = DEFAULT_CARD_REVEAL_LOCK_TICKS;
            eventRevealLockTicks = DEFAULT_EVENT_REVEAL_LOCK_TICKS;
            defaultCardMode = DEFAULT_CARD_MODE;
            skillCutinDurationTicks = DEFAULT_SKILL_CUTIN_DURATION_TICKS;
            skillCutinAudience = DEFAULT_SKILL_CUTIN_AUDIENCE;
            skillCooldownSecondsPerRound = DEFAULT_SKILL_COOLDOWN_SECONDS_PER_ROUND;
            skillMinimumCooldownSeconds = DEFAULT_SKILL_MINIMUM_COOLDOWN_SECONDS;
            skillMaximumCooldownSeconds = DEFAULT_SKILL_MAXIMUM_COOLDOWN_SECONDS;
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

    private static CharacterSkillCutinAudience normalizeAudience(String raw) {
        return CharacterSkillCutinAudience.byName(raw);
    }

    private static long lastModified() {
        try {
            return Files.exists(PATH) ? Files.getLastModifiedTime(PATH).toMillis() : Long.MIN_VALUE;
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }
}
