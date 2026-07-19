package com.astral_craft.client.gui.phrase;

import com.astral_craft.AstralCraft;
import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stores player-owned quick phrases locally on the physical client. */
public class PlayerQuickPhraseConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<String> phrases;

    public static List<String> phrases() {
        ensureLoaded();
        return phrases;
    }

    public static void add(String phrase) {
        ensureLoaded();
        String normalized = phrase == null ? "" : phrase.trim();
        if (normalized.isBlank()) return;
        phrases.add(normalized);
        save();
    }

    public static void delete(int index) {
        ensureLoaded();
        if (index < 0 || index >= phrases.size()) return;
        phrases.remove(index);
        save();
    }

    public static void ensureLoaded() {
        if (phrases != null) return;
        phrases = new ArrayList<>();
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("phrases")) return;
            JsonArray array = root.getAsJsonArray("phrases");
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    String text = element.getAsString().trim();
                    if (!text.isBlank()) phrases.add(text);
                }
            }
        } catch (Exception ignored) {
            phrases.clear();
        }
    }

    public static void save() {
        if (phrases == null) return;
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (String phrase : phrases) {
                array.add(phrase);
            }

            root.add("phrases", array);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {}
    }

    public static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(AstralCraft.MOD_ID)
                .resolve("quick_phrases.json");
    }

}