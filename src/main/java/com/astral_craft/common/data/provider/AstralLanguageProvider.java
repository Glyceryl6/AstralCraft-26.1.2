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

import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
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
        this.addKey(AstralBlocks.PLATFORM_CANDY_GHOST, "Quirky Candy Machine", "怪奇糖果机");
        this.addKey(AstralBlocks.PLATFORM_CARD, "Card Bounce", "卡牌奖励");
        this.addKey(AstralBlocks.PLATFORM_DAMAGE, "Sudden Calamity", "天降横祸");
        this.addKey(AstralBlocks.PLATFORM_DESTINY, "Fortune", "个人命运");
        this.addKey(AstralBlocks.PLATFORM_DIVINE, "Divination", "占卜");
        this.addKey(AstralBlocks.PLATFORM_EVENT, "Event", "全员事件");
        this.addKey(AstralBlocks.PLATFORM_FIRE, "Cannon", "炮台");
        this.addKey(AstralBlocks.PLATFORM_GAMBLE, "Guessing", "彩票");
        this.addKey(AstralBlocks.PLATFORM_GIFT, "Gift", "礼物");
        this.addKey(AstralBlocks.PLATFORM_GIMMICK, "Gimmick", "机关控制台");
        this.addKey(AstralBlocks.PLATFORM_GOLD, "Windfall Hits", "天降横财");
        this.addKey(AstralBlocks.PLATFORM_HEAL, "Recover", "恢复");
        this.addKey(AstralBlocks.PLATFORM_HOSPITAL, "Hospital", "医院");
        this.addKey(AstralBlocks.PLATFORM_JUMP, "Jump", "跳跃");
        this.addKey(AstralBlocks.PLATFORM_LOTTERY, "Lottery", "猜猜乐");
        this.addKey(AstralBlocks.PLATFORM_MONSTER, "Monster", "怪物");
        this.addKey(AstralBlocks.PLATFORM_MOVE_AGAIN, "Haste", "疾行");
        this.addKey(AstralBlocks.PLATFORM_RELIC, "Chip Shop", "筹码商店");
        this.addKey(AstralBlocks.PLATFORM_SHOP, "Shop", "商店");
        this.addKey(AstralBlocks.PLATFORM_START, "Check Point", "起始点");
        this.addKey(AstralBlocks.PLATFORM_TELEPORT, "Portal", "传送门");
        this.addKey(AstralBlocks.PLATFORM_TELEPORT_POINT, "Assault", "突击门");
        
        // Item: Handcard
        this.addKey(AstralItems.HANDCARD_ATTACK_M, "Attack (M)", "攻击（中）");
        this.addKey(AstralItems.HANDCARD_ATTACK_L, "Attack (L)", "攻击（大）");
        this.addKey(AstralItems.HANDCARD_ATTACK_G, "Attack (G)", "攻击（特大）");
        this.addKey(AstralItems.HANDCARD_DEFENSE_M, "Defense (M)", "防御（中）");
        this.addKey(AstralItems.HANDCARD_DEFENSE_L, "Defense (L)", "防御（大）");
        this.addKey(AstralItems.HANDCARD_DEFENSE_G, "Defense (G)", "防御（特大）");
        this.addKey(AstralItems.HANDCARD_CHOCOLATE_CAKE, "Chocolate Cake", "");
        this.addKey(AstralItems.HANDCARD_HAMBURGER, "Hamburger", "巧克力蛋糕");
        this.addKey(AstralItems.HANDCARD_SMART_DICE, "Smart Dice", "遥控骰子");
        this.addKey(AstralItems.HANDCARD_FIGHT_FIRE_WITH_FIRE, "Fight Fire with Fire", "以毒攻毒");
        this.addKey(AstralItems.HANDCARD_BERSERK, "Berserk", "狂暴");
        this.addKey(AstralItems.HANDCARD_BARRICADE, "Barricade", "路障");
        this.addKey(AstralItems.HANDCARD_REDIRECTION, "Redirection", "方向抉择");
        this.addKey(AstralItems.HANDCARD_ENTRAPMENT, "Entrapment", "钓鱼执法");
        this.addKey(AstralItems.HANDCARD_SOUL_LINK, "Soul Link", "灵魂链接");
        this.addKey(AstralItems.HANDCARD_SELF_EXPLOSION, "Self-Explosion", "自爆");
        this.addKey(AstralItems.HANDCARD_SLINGSHOT, "Slingshot", "皮筋弹弓");
        this.addKey(AstralItems.HANDCARD_DEMOLITION, "Demolition", "爆破专家");
        this.addKey(AstralItems.HANDCARD_EXPIRED_BENTO, "Expired Bento", "过期便当");
        this.addKey(AstralItems.HANDCARD_ALL_OR_NOTHING, "All or Nothing", "丧心病狂");
        this.addKey(AstralItems.HANDCARD_TIME_BOMB, "Time Bomb", "传递炸弹");
        this.addKey(AstralItems.HANDCARD_SNATCH, "Snatch", "抢夺");
        this.addKey(AstralItems.HANDCARD_SCAVENGING, "Scavenging", "拾荒");
        this.addKey(AstralItems.HANDCARD_RANDOM_PORTAL, "Random Portal", "随机传送门");
        this.addKey(AstralItems.HANDCARD_DIRECTED_BOOST, "Directed Boost", "定向加速");
        this.addKey(AstralItems.HANDCARD_SNOWBALL_ATTACK, "Snowball Attack", "雪球攻击");
        this.addKey(AstralItems.HANDCARD_BARRIER, "Barrier", "保护屏障");
        this.addKey(AstralItems.HANDCARD_EYE_FOR_AN_EYE, "Eye for an Eye", "以牙还牙");
        this.addKey(AstralItems.HANDCARD_RANDOM_SELECT, "Random Select", "错误的目标");
        this.addKey(AstralItems.HANDCARD_GAWU_CUT, "Legendary Sword: Gawu Cut", "名刀：嘎呜切");
        this.addKey(AstralItems.HANDCARD_SHADOW_ATTACK, "Shadow Attack", "暗影突袭");
        this.addKey(AstralItems.HANDCARD_CHARGE, "Charge", "蓄力");
        this.addKey(AstralItems.HANDCARD_POWERFUL_ATTACK, "Powerful Attack", "全力攻击");
        this.addKey(AstralItems.HANDCARD_POISON_FANG, "Poison Fang", "毒牙");
        this.addKey(AstralItems.HANDCARD_BITE, "Bite", "撕咬");
        this.addKey(AstralItems.HANDCARD_DRAGON_ROAR, "Dragon Roar", "龙之咆哮");
        this.addKey(AstralItems.HANDCARD_FATE_GUIDANCE, "Guidance of Fate", "命运的指引");
        this.addKey(AstralItems.HANDCARD_RAILGUN, "Railgun", "轨道炮");
        this.addKey(AstralItems.HANDCARD_BOTH_HAVE, "What you have, I have.", "你有我有");
        this.addKey(AstralItems.HANDCARD_HURRY, "Hurry Hurry", "加急加快");
        this.addKey(AstralItems.HANDCARD_KING_POWER, "King's Power", "王之力");
        this.addKey(AstralItems.HANDCARD_IMMOVABLE, "Immovable", "岿然不动");
        this.addKey(AstralItems.HANDCARD_LUXURIOUS_FEAST, "Luxurious Feast", "奢华大餐");
        this.addKey(AstralItems.HANDCARD_BLAST, "Blast", "定向爆破");
        this.addKey(AstralItems.HANDCARD_SUPPORT, "Support", "支援");
        this.addKey(AstralItems.HANDCARD_FIRECRACKERS, "Firecrackers", "高升炮");
        this.addKey(AstralItems.HANDCARD_SUPPORT_GUM, "Support Gum", "支援口香糖");
        this.addKey(AstralItems.HANDCARD_ENERGY_BAR, "Energy Bar", "能量补充棒");
        this.addKey(AstralItems.HANDCARD_SMARTIE_GUMMY, "Smartie Gummy", "大聪明软糖");
        this.addKey(AstralItems.HANDCARD_QUIRKY_ENCHANTED, "Quirky Enchanted", "古怪的附灵物");
        this.addKey(AstralItems.HANDCARD_RELEASE_AZURE_SOUL, "Release Azure Soul", "释放青魂");
        this.addKey(AstralItems.HANDCARD_RELEASE_SCARLET_SOUL, "Release Scarlet Soul", "释放赤魂");
        this.addKey(AstralItems.HANDCARD_COLOURFUL_FEATHER, "Colourful Feather", "彩羽");
        this.addKey(AstralItems.HANDCARD_CHEER_UP, "Cheer up", "振作起来");
        this.addKey(AstralItems.HANDCARD_OVERFLOWING_FORTUNE, "Overflowing Fortune", "吉星高照");
        this.addKey(AstralItems.HANDCARD_ASHEN_FEATHER, "Ashen Feather", "灰烬之羽");
        this.addKey(AstralItems.HANDCARD_PROBLEM_STUDENT, "It's His Fault!", "是他干的");
        this.addKey(AstralItems.HANDCARD_LASER, "Laser Beam", "激光");
        this.addKey(AstralItems.HANDCARD_BRICK, "Brick", "板砖");
        this.addKey(AstralItems.HANDCARD_FORTUNE, "Talisman Card - Fortune", "符卡-福");
        this.addKey(AstralItems.HANDCARD_MISFORTUNE, "Talisman Card - Misfortune", "符卡-祸");
        this.addKey(AstralItems.HANDCARD_LIVING_BOOK, "Living Book", "活体书页");
        this.addKey(AstralItems.HANDCARD_ENHANCED_BARRICADE, "Enhanced Barricade", "强化拒止");
        this.addCharacterTranslations();
    }

    private void addCharacterTranslations() {
        this.add("character.astral_craft.default.title", "Astral Character", "星趴角色");
        this.add("gui.astral_craft.character_settings.source.all", "All", "全部");
        this.add("gui.astral_craft.character_settings.addon_header", "%s Additions", "%s追加");
        for (AstralCharacterDataCatalog.SkinRarityEntry rarity : AstralCharacterDataCatalog.SKIN_RARITIES) {
            this.add(rarity.nameKey(), rarity.enName(), rarity.zhName());
        }

        for (AstralCharacterDataCatalog.CharacterEntry entry : AstralCharacterDataCatalog.CHARACTERS) {
            String baseKey = "character.astral_craft." + entry.id;
            this.add(baseKey + ".name", entry.enName, entry.zhName);
            this.add(baseKey + ".title", entry.enTitle, entry.zhTitle);
            this.add(baseKey + ".skin.default", "Default", "默认");
            this.add(baseKey + ".skin.bond", "Bond", "羁绊");
            for (AstralCharacterDataCatalog.SkinEntry skin : entry.skins) {
                this.add(baseKey + ".skin." + skin.id(), skin.enName(), skin.zhName());
            }
            if (entry.hasPotential) {
                this.add(baseKey + ".potential.desc", "This character has an unlockable potential.", "该角色拥有可以激发的潜能。");
                this.add(baseKey + ".potential.effect", "After activation, this character's skill is enhanced.", "激发后，该角色的技能会获得强化。");
            }
        }

        this.add("event.astral_craft.lucky_find.name", "Lucky Find", "幸运发现");
        this.add("event.astral_craft.lucky_find.description", "You found a small treasure nearby.", "你在附近发现了一份小小的宝物。");
        this.add("event.astral_craft.ambush.name", "Sudden Ambush", "突然伏击");
        this.add("event.astral_craft.ambush.description", "Something dangerous has been drawn to you.", "有什么危险的东西被你吸引过来了。");
        this.add("event.astral_craft.astral_blessing.name", "Astral Blessing", "星之祝福");
        this.add("event.astral_craft.astral_blessing.description", "A gentle astral light restores your strength.", "温柔的星光正在恢复你的力量。");
        this.add("event.astral_craft.low_health_aid.name", "Emergency Aid", "应急援助");
        this.add("event.astral_craft.low_health_aid.description", "When you are in danger, a small reserve of astral power answers you.", "当你陷入危险时，一小股星力会回应你。");
        this.add("event.astral_craft.night_ambush.name", "Night Ambush", "夜间伏击");
        this.add("event.astral_craft.night_ambush.description", "The night connects you with wandering hostile presences.", "夜色将你与游荡的敌意存在连接在了一起。");
        this.add("event.astral_craft.cave_cache.name", "Cave Cache", "洞窟藏物");
        this.add("event.astral_craft.cave_cache.description", "Deep underground mining may uncover a small cache.", "在地下深处挖掘时，有机会发现一份小小的藏物。");
        this.add("message.astral_craft.event.triggered", "%s", "%s");
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
        Path prefix = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(AstralCraft.MOD_ID);
        Path langExtra = prefix.resolve("lang_extra").resolve(String.format("%s.json", this.locale));
        this.mergeLangExtra(data, langExtra.toString().replace("generated", "main"));
        Path target = prefix.resolve("lang").resolve(String.format("%s.json", this.locale));
        JsonObject json = new JsonObject();
        data.forEach(json::addProperty);
        return DataProvider.saveStable(cache, json, target);
    }

    private void mergeLangExtra(Map<String, String> data, String path) {
        try (var reader = Files.newBufferedReader(Path.of(path))) {
            JsonObject fileObject = JsonParser.parseReader(reader).getAsJsonObject();
            fileObject.keySet().forEach(key -> data.putIfAbsent(key, fileObject.get(key).getAsString()));
        } catch (IOException ignored) {}
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