package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralBlocks;
import com.astral_craft.common.registry.AstralItems;
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
        // Block: Platform
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
        
        // Item: Handcard
        this.addKey(AstralItems.HANDCARD_ATTACK_M, "Handcard: Attack (M)", "手牌：攻击（中）");
        this.addKey(AstralItems.HANDCARD_ATTACK_L, "Handcard: Attack (L)", "手牌：攻击（大）");
        this.addKey(AstralItems.HANDCARD_ATTACK_G, "Handcard: Attack (G)", "手牌：攻击（特大）");
        this.addKey(AstralItems.HANDCARD_DEFENSE_M, "Handcard: Defense (M)", "手牌：防御（中）");
        this.addKey(AstralItems.HANDCARD_DEFENSE_L, "Handcard: Defense (L)", "手牌：防御（大）");
        this.addKey(AstralItems.HANDCARD_DEFENSE_G, "Handcard: Defense (G)", "手牌：防御（特大）");
        this.addKey(AstralItems.HANDCARD_CHOCOLATE_CAKE, "Handcard: Chocolate Cake", "手牌：");
        this.addKey(AstralItems.HANDCARD_HAMBURGER, "Handcard: Hamburger", "手牌：巧克力蛋糕");
        this.addKey(AstralItems.HANDCARD_SMART_DICE, "Handcard: Smart Dice", "手牌：遥控骰子");
        this.addKey(AstralItems.HANDCARD_FIGHT_FIRE_WITH_FIRE, "Handcard: Fight Fire with Fire", "手牌：以毒攻毒");
        this.addKey(AstralItems.HANDCARD_BERSERK, "Handcard: Berserk", "手牌：狂暴");
        this.addKey(AstralItems.HANDCARD_BARRICADE, "Handcard: Barricade", "手牌：路障");
        this.addKey(AstralItems.HANDCARD_REDIRECTION, "Handcard: Redirection", "手牌：方向抉择");
        this.addKey(AstralItems.HANDCARD_ENTRAPMENT, "Handcard: Entrapment", "手牌：钓鱼执法");
        this.addKey(AstralItems.HANDCARD_SOUL_LINK, "Handcard: Soul Link", "手牌：灵魂链接");
        this.addKey(AstralItems.HANDCARD_SELF_EXPLOSION, "Handcard: Self-Explosion", "手牌：自爆");
        this.addKey(AstralItems.HANDCARD_SLINGSHOT, "Handcard: Slingshot", "手牌：皮筋弹弓");
        this.addKey(AstralItems.HANDCARD_DEMOLITION, "Handcard: Demolition", "手牌：爆破专家");
        this.addKey(AstralItems.HANDCARD_EXPIRED_BENTO, "Handcard: Expired Bento", "手牌：过期便当");
        this.addKey(AstralItems.HANDCARD_ALL_OR_NOTHING, "Handcard: All or Nothing", "手牌：丧心病狂");
        this.addKey(AstralItems.HANDCARD_TIME_BOMB, "Handcard: Time Bomb", "手牌：传递炸弹");
        this.addKey(AstralItems.HANDCARD_SNATCH, "Handcard: Snatch", "手牌：抢夺");
        this.addKey(AstralItems.HANDCARD_SCAVENGING, "Handcard: Scavenging", "手牌：拾荒");
        this.addKey(AstralItems.HANDCARD_RANDOM_PORTAL, "Handcard: Random Portal", "手牌：随机传送门");
        this.addKey(AstralItems.HANDCARD_DIRECTED_BOOST, "Handcard: Directed Boost", "手牌：定向加速");
        this.addKey(AstralItems.HANDCARD_SNOWBALL_ATTACK, "Handcard: Snowball Attack", "手牌：");
        this.addKey(AstralItems.HANDCARD_BARRIER, "Handcard: Barrier", "手牌：保护屏障");
        this.addKey(AstralItems.HANDCARD_EYE_FOR_AN_EYE, "Handcard: Eye for an Eye", "手牌：以牙还牙");
        this.addKey(AstralItems.HANDCARD_RANDOM_SELECT, "Handcard: Random Select", "手牌：错误的目标");
        this.addKey(AstralItems.HANDCARD_GAWU_CUT, "Handcard: Legendary Sword: Gawu Cut", "手牌：名刀：嘎呜切");
        this.addKey(AstralItems.HANDCARD_SHADOW_ATTACK, "Handcard: Shadow Attack", "手牌：暗影突袭");
        this.addKey(AstralItems.HANDCARD_CHARGE, "Handcard: Charge", "手牌：蓄力");
        this.addKey(AstralItems.HANDCARD_POWERFUL_ATTACK, "Handcard: Powerful Attack", "手牌：全力攻击");
        this.addKey(AstralItems.HANDCARD_POISON_FANG, "Handcard: Poison Fang", "手牌：毒牙");
        this.addKey(AstralItems.HANDCARD_BITE, "Handcard: Bite", "手牌：撕咬");
        this.addKey(AstralItems.HANDCARD_DRAGON_ROAR, "Handcard: Dragon Roar", "手牌：龙之咆哮");
        this.addKey(AstralItems.HANDCARD_FATE_GUIDANCE, "Handcard: Guidance of Fate", "手牌：命运的指引");
        this.addKey(AstralItems.HANDCARD_RAILGUN, "Handcard: Railgun", "手牌：轨道炮");
        this.addKey(AstralItems.HANDCARD_BOTH_HAVE, "Handcard: What you have, I have.", "手牌：你有我有");
        this.addKey(AstralItems.HANDCARD_HURRY, "Handcard: Hurry Hurry", "手牌：加急加快");
        this.addKey(AstralItems.HANDCARD_KING_POWER, "Handcard: King's Power", "手牌：王之力");
        this.addKey(AstralItems.HANDCARD_IMMOVABLE, "Handcard: Immovable", "手牌：岿然不动");
        this.addKey(AstralItems.HANDCARD_LUXURIOUS_FEAST, "Handcard: Luxurious Feast", "手牌：奢华大餐");
        this.addKey(AstralItems.HANDCARD_BLAST, "Handcard: Blast", "手牌：定向爆破");
        this.addKey(AstralItems.HANDCARD_SUPPORT, "Handcard: Support", "手牌：支援");
        this.addKey(AstralItems.HANDCARD_FIRECRACKERS, "Handcard: Firecrackers", "手牌：高升炮");
        this.addKey(AstralItems.HANDCARD_SUPPORT_GUM, "Handcard: Support Gum", "手牌：支援口香糖");
        this.addKey(AstralItems.HANDCARD_ENERGY_BAR, "Handcard: Energy Bar", "手牌：能量补充棒");
        this.addKey(AstralItems.HANDCARD_SMARTIE_GUMMY, "Handcard: Smartie Gummy", "手牌：大聪明软糖");
        this.addKey(AstralItems.HANDCARD_QUIRKY_ENCHANTED, "Handcard: Quirky Enchanted", "手牌：古怪的附灵物");
        this.addKey(AstralItems.HANDCARD_RELEASE_AZURE_SOUL, "Handcard: Release Azure Soul", "手牌：释放青魂");
        this.addKey(AstralItems.HANDCARD_RELEASE_SCARLET_SOUL, "Handcard: Release Scarlet Soul", "手牌：释放赤魂");
        this.addKey(AstralItems.HANDCARD_COLOURFUL_FEATHER, "Handcard: Colourful Feather", "手牌：彩羽");
        this.addKey(AstralItems.HANDCARD_CHEER_UP, "Handcard: Cheer up", "手牌：振作起来");
        this.addKey(AstralItems.HANDCARD_OVERFLOWING_FORTUNE, "Handcard: Overflowing Fortune", "手牌：吉星高照");
        this.addKey(AstralItems.HANDCARD_ASHEN_FEATHER, "Handcard: Ashen Feather", "手牌：灰烬之羽");
        this.addKey(AstralItems.HANDCARD_PROBLEM_STUDENT, "Handcard: It's His Fault!", "手牌：是他干的");
        this.addKey(AstralItems.HANDCARD_LASER, "Handcard: Laser Beam", "手牌：激光");
        this.addKey(AstralItems.HANDCARD_BRICK, "Handcard: Brick", "手牌：板砖");
        this.addKey(AstralItems.HANDCARD_FORTUNE, "Handcard: Talisman Card - Fortune", "手牌：符卡-福");
        this.addKey(AstralItems.HANDCARD_MISFORTUNE, "Handcard: Talisman Card - Misfortune", "手牌：符卡-祸");
        this.addKey(AstralItems.HANDCARD_LIVING_BOOK, "Handcard: Living Book", "手牌：活体书页");
        this.addKey(AstralItems.HANDCARD_ENHANCED_BARRICADE, "Handcard: Enhanced Barricade", "手牌：强化拒止");
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