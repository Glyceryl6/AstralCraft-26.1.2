package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralBlocks;
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
        this.addKey(AstralBlocks.PLATFORM_CANDY_GHOST, "Platform: Quirky Candy Machine", "地块：怪奇糖果机");
        this.addKey(AstralBlocks.PLATFORM_CARD, "Platform: Card Bounce", "地块：卡牌奖励");
        this.addKey(AstralBlocks.PLATFORM_DAMAGE, "Platform: Sudden Calamity", "地块：天降横祸");
        this.addKey(AstralBlocks.PLATFORM_DESTINY, "Platform: Fortune", "地块：个人命运");
        this.addKey(AstralBlocks.PLATFORM_DIVINE, "Platform: Divination", "地块：占卜");
        this.addKey(AstralBlocks.PLATFORM_EVENT, "Platform: Event", "地块：全员事件");
        this.addKey(AstralBlocks.PLATFORM_FIRE, "Platform: Cannon", "地块：炮台");
        this.addKey(AstralBlocks.PLATFORM_GAMBLE, "Platform: Guessing", "地块：彩票");
        this.addKey(AstralBlocks.PLATFORM_GIFT, "Platform: Gift", "地块：礼物");
        this.addKey(AstralBlocks.PLATFORM_GIMMICK, "Platform: Gimmick", "地块：机关控制台");
        this.addKey(AstralBlocks.PLATFORM_GOLD, "Platform: Windfall Hits", "地块：天降横财");
        this.addKey(AstralBlocks.PLATFORM_HEAL, "Platform: Recover", "地块：恢复");
        this.addKey(AstralBlocks.PLATFORM_HOSPITAL, "Platform: Hospital", "地块：医院");
        this.addKey(AstralBlocks.PLATFORM_JUMP, "Platform: Jump", "地块：跳跃");
        this.addKey(AstralBlocks.PLATFORM_LOTTERY, "Platform: Lottery", "地块：猜猜乐");
        this.addKey(AstralBlocks.PLATFORM_MONSTER, "Platform: Monster", "地块：怪物");
        this.addKey(AstralBlocks.PLATFORM_MOVE_AGAIN, "Platform: Haste", "地块：疾行");
        this.addKey(AstralBlocks.PLATFORM_RELIC, "Platform: Chip Shop", "地块：筹码商店");
        this.addKey(AstralBlocks.PLATFORM_SHOP, "Platform: Shop", "地块：商店");
        this.addKey(AstralBlocks.PLATFORM_START, "Platform: Check Point", "地块：起始点");
        this.addKey(AstralBlocks.PLATFORM_TELEPORT, "Platform: Portal", "地块：传送门");
        this.addKey(AstralBlocks.PLATFORM_TELEPORT_POINT, "Platform: Assault", "地块：突击门");
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

    private void addKey(DeferredHolder<?, ?> key, String en, String cn) {
        try {
            Class<?> clazz = key.get().getClass();
            Method method = clazz.getMethod("getDescriptionId");
            if (method.invoke(key.get()) instanceof String id) {
                this.add(id, en, cn);
            }
        } catch (Exception ignored) {}
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