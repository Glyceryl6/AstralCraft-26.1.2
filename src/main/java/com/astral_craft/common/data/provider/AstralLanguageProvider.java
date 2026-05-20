package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class AstralLanguageProvider extends LanguageProvider {

    private final Map<String, String> enData = new TreeMap<>();
    private final Map<String, String> cnData = new TreeMap<>();
    private final PackOutput output;
    private final String locale;

    public AstralLanguageProvider(PackOutput output, String locale) {
        super(output, AstralCraft.MOD_ID, locale);
        this.output = output;
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.addTranslations();
        if (this.locale.equals("en_us") && !this.enData.isEmpty()) {
            return this.save(cache, this.enData);
        }

        if (this.locale.equals("zh_cn") && !this.cnData.isEmpty()) {
            return this.save(cache, this.cnData);
        }

        return CompletableFuture.allOf();
    }

    private CompletableFuture<?> save(CachedOutput cache, Map<String, String> data) {
        try {
            Path prefix = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(AstralCraft.MOD_ID);
            Path langExtra = prefix.resolve("lang_extra").resolve(String.format("%s.json", this.locale));
            FileReader reader = new FileReader(langExtra.toString().replace("generated", "main"));
            JsonObject fileObject = JsonParser.parseReader(reader).getAsJsonObject();
            fileObject.keySet().forEach(s -> data.put(s, fileObject.get(s).getAsString()));
            Path target = prefix.resolve("lang").resolve(String.format("%s.json", this.locale));
            JsonObject json = new JsonObject();
            data.forEach(json::addProperty);
            return DataProvider.saveStable(cache, json, target);
        } catch (FileNotFoundException e) {
            return CompletableFuture.allOf();
        }
    }

    private String getEnglishName(String path) {
        String[] words = path.split("_");
        for (int i = 0; i < words.length; i++) {
            String firstLetter = words[i].substring(0, 1);
            String remainingLetters = words[i].substring((1));
            words[i] = firstLetter.toUpperCase() + remainingLetters;
        }

        return String.join(" ", words);
    }

    private void addKey(DeferredHolder<?, ?> key, String cn) {
        try {
            Class<?> clazz = key.get().getClass();
            Method method = clazz.getMethod("getDescriptionId");
            if (method.invoke(key.get()) instanceof String id) {
                this.add(id, this.getEnglishName(key.getId().getPath()), cn);
            }
        } catch (Exception ignored) {}
    }

    private void addKey(ResourceKey<?> key, String cn) {
        String type = key.registry().getPath();
        String name = key.identifier().getPath();
        if (type.contains("/")) {
            String[] words = type.split("/");
            type = words[words.length - 1];
        }

        String languageKey = type + "." + key.identifier().toLanguageKey();
        this.add(languageKey, this.getEnglishName(name), cn);
    }

    private void add(String key, String en, String cn) {
        if (this.locale.equals("en_us") && !this.enData.containsKey(key)) {
            this.enData.put(key, en);
        } else if (this.locale.equals("zh_cn") && !this.cnData.containsKey(key)) {
            this.cnData.put(key, cn);
        }
    }

}