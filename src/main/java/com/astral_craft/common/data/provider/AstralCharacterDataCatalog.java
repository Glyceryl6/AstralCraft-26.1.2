package com.astral_craft.common.data.provider;

import java.util.List;

public class AstralCharacterDataCatalog {

    public static final List<SkinRarityEntry> SKIN_RARITIES = List.of(
            skinRarity("none", "None", "无品质", 0xFFFFFFFF, 0x00FFFFFF, 0xFFFFFFFF),
            skinRarity("sapphire", "Sapphire", "蓝宝石", 0xFF25C7FF, 0xFF111111, 0xFF25C7FF),
            skinRarity("amethyst", "Amethyst", "紫水晶", 0xFFD551FF, 0xFF111111, 0xFFD551FF),
            skinRarity("emerald", "Emerald", "绿宝石", 0xFF8DFF64, 0xFF111111, 0xFF8DFF64),
            skinRarity("platinum", "Platinum", "铂金", 0xFFFFCFB8, 0xFF111111, 0xFFFFCFB8),
            skinRarity("ultimate", "Ultimate", "至臻", 0xFFE5FF75, 0xFF111111, 0xFFE5FF75));

    public static final List<CharacterEntry> CHARACTERS = List.of(
            character("parunan", "Parunan", "帕露南", "Lord of Commerce", "商业之主", 1, 2, 10, true, 10, 2, List.of(skin("fashion_icon", "Fashion Icon", "时尚达人", "sapphire"), skin("fortune_guardian", "Fortune Guardian", "善财天宫", "amethyst"))),
            character("fanny", "Fanny", "芬妮", "Quirky Detective", "古怪神探", 1, 2, 10, true, 11, 3, List.of(skin("mystic_insight", "Mystic Insight", "超然解析", "sapphire"), skin("breezy_summer", "Breezy Summer", "清凉盛夏", "amethyst"), skin("mad_scientist", "Mad Scientist", "科学怪探", "amethyst"), skin("leisurely_holiday", "Leisurely Holiday", "悠闲假日", "sapphire"))),
            character("alana", "Alana", "阿兰娜", "Shy Nun", "社恐修女", 1, 1, 9, true, 12, 3, List.of(skin("clumsy_girl", "Clumsy Girl", "冒失少女", "sapphire"), skin("throbbing_beach", "Throbbing Beach", "悸动海滩", "amethyst"), skin("witchs_weapon", "Witch's Weapon", "魔女兵器", "amethyst"))),
            character("komachi", "Komachi", "小町", "Shadow Ninja", "暗影忍者", 1, 1, 9, true, 13, 3, List.of(skin("why_the_ninja", "Why the Ninja?", "忍者为何？", "sapphire"), skin("shadow_spirit", "Shadow Spirit", "暗影狼灵", "amethyst"))),
            character("padman", "Padman", "派德曼", "Uncle Employee", "社员叔叔", 2, 2, 9, false, 14, 3, List.of()),
            character("papara", "Papara", "帕帕拉", "Red Hot", "猩红辣妹", 2, 1, 10, false, 15, 3, List.of(skin("guard_captain", "Guard Captain", "护卫队长", "sapphire"))),
            character("ren", "Ren", "恋", "Pro Gamer", "游戏大师", 2, 1, 8, false, 16, 3, List.of(skin("cheerleader", "Cheerleader", "啦啦队长", "sapphire"), skin("snowy_spirit", "Snowy Spirit", "雪夜精灵", "sapphire"), skin("pool_party", "Pool Party", "泳池派对", "amethyst"), skin("cute_captain", "Cute Captain", "可爱船长", "amethyst"), skin("illusory_dealer", "Illusory Dealer", "卡牌魔术", "platinum"))),
            character("mimi", "Mimi", "米米", "Kanban Musume", "看板娘", 1, 1, 9, true, 17, 3, List.of(skin("slushie_express", "Slushie Express", "冰沙快递", "sapphire"), skin("transport_magic", "Transport Magic", "运输魔法", "amethyst"), skin("race_queen", "Race Queen", "赛车女郎", "emerald"))),
            character("z3000", "Z3000", "Z3000", "Waste-Collecting Robot", "垃圾箱", 1, 2, 10, false, 18, 4, List.of(skin("sky_explorer_assistant", "Sky Explorer Assistant", "探天助手", "amethyst"))),
            character("pandaman", "Pandaman", "潘大猛", "Human Chariot", "肉弹战车", 1, 0, 14, false, 19, 3, List.of()),
            character("lulu", "Lulu", "璐璐", "Slime Girl", "史莱姆", 2, 2, 9, false, 20, 3, List.of(skin("mimic", "Mimic", "宝箱怪", "sapphire"), skin("sweet_trap", "Sweet Trap", "甜蜜陷阱", "amethyst"))),
            character("fen", "Fen", "枫", "Cheongsam Girl", "旗袍娘", 1, 0, 10, false, 21, 3, List.of(skin("maid_warrior", "Maid Warrior", "战斗女仆", "sapphire"), skin("dragon_ascent", "Dragon's Ascent", "锦跃游龙", "amethyst"), skin("trendy_princess", "Trendy Princess", "潮流公主", "sapphire"))),
            character("hai_qing", "Hai Qing", "蓝海晴", "Destiny Girl", "命运少女", 1, 1, 10, false, 22, 3, List.of(skin("stylish_grace", "Stylish Grace", "优雅身姿", "sapphire"), skin("costume_party", "Costume Party", "化装舞会", "amethyst"), skin("spring_picnic", "Spring Picnic", "春日野餐", "amethyst"), skin("train_conductor", "Train Conductor", "列车长", "emerald"))),
            character("misaki", "Misaki", "美咲", "Katana Master", "太刀使", 0, 2, 9, false, 23, 3, List.of(skin("maid_warrior", "Maid Warrior", "战斗女仆", "sapphire"), skin("ink_wash_waltz", "Ink-Wash Waltz", "墨韵圆舞", "amethyst"), skin("witch_weapon", "Witch's Weapon", "魔女兵器", "amethyst"), skin("dragon_honor", "Dragon's Honor", "真龙御礼", "ultimate"))),
            character("nardis", "Nardis", "娜蒂斯", "Oasis Queen", "绿洲女王", 1, 1, 9, false, 24, 3, List.of(skin("twilight_witch", "Twilight Witch", "暮色魔女", "sapphire"), skin("festival_dress", "Festival Dress", "祭典礼装", "amethyst"))),
            character("jasmine", "Jasmine", "茉莉", "Robot Maid", "家政机器人", 1, 0, 9, false, 25, 4, List.of(skin("coastal_surfer", "Coastal Surfer", "冲浪海岸", "sapphire"), skin("festival_dress", "Festival Dress", "节日盛装", "amethyst"), skin("kitchen_master", "Kitchen Master", "后厨能手", "amethyst"))),
            character("al", "A.L.", "阿尔", "Syndicate Prince", "暗区少主", 1, 1, 9, false, 26, 3, List.of(skin("ketchup_magic", "Ketchup Magic", "番茄酱魔法", "sapphire"))),
            character("luka", "Luka", "星魅琉华", "Midnight Flash", "午夜闪光", 1, 2, 9, false, 27, 3, List.of(skin("special_gift", "Special Gift", "特别礼物", "sapphire"), skin("fated_crossroads", "Fated Crossroads", "转角之期", "amethyst"))),
            character("nancy_lu", "Nancy Lu", "南希露", "Cyber Phantom", "网络魅影", 1, 1, 9, false, 28, 3, List.of(skin("mealtime_magic", "Mealtime Magic", "餐前魔法", "sapphire"), skin("heatwave_shores", "Heatwave Shores", "热浪沙滩", "amethyst"), skin("splendor_royale", "Splendor Royale", "雍容盛景", "ultimate"))),
            character("rin", "Rin", "凛", "Rookie Investigator", "新人调查员", 1, 1, 9, false, 29, 3, List.of(skin("harbour_holiday", "Harbour Holiday", "海湾假日", "sapphire"), skin("winter_glow", "Winter Glow", "冬日暖意", "amethyst"))),
            character("megas", "Megas", "梅加斯", "Mech Hero", "机械超人", 0, 2, 9, false, 30, 3, List.of(skin("seaside_rescue", "Seaside Rescue", "海滨救援", "sapphire"), skin("arcane_gunslinger", "Arcane Gunslinger", "魔枪表演", "platinum"))),
            character("zhao", "Zhao", "姬梦朝", "Fengshui Master", "风水师", 1, 1, 10, false, 31, 3, List.of(skin("shoreline_memories", "Shoreline Memories", "滨边时光", "amethyst"), skin("auspicious_blessing", "Auspicious Blessing", "祥瑞天章", "ultimate"))),
            character("teru", "Teru", "照", "Hierarch of Mikami", "三神御主", 2, 1, 9, false, 32, 3, List.of(skin("little_nurse", "Little Nurse", "小护士", "sapphire"), skin("spellbinding_singer", "Spellbinding Singer", "魔旅歌姬", "platinum"))),
            character("moses", "Moses", "摩西", "Gunsmith", "枪匠", 1, 1, 11, false, 33, 2, List.of(skin("special_assignment", "Special Assignment", "特殊委派", "amethyst"))),
            character("mamushi", "Mamushi", "真梦梓", "Jiao of the Mire", "沼之蛟龙", 2, 1, 9, false, 34, 3, List.of(skin("frost_dancer", "Frost Dancer", "冰上舞者", "amethyst"))),
            character("ink_shadow", "Ink Shadow", "墨影", "Novice Hunter", "小猎手", 2, 1, 10, false, 35, 3, List.of()),
            character("bonnie", "Bonnie", "邦妮", "Poisoned Apple", "毒苹果", 2, 1, 9, false, 36, 3, List.of(skin("urgent_invitation", "Urgent Invitation", "紧急邀约", "sapphire"))),
            character("ling_ling", "Ling Ling", "铃铃", "Supernatural Forces", "怪力乱神", 2, 2, 10, false, 37, 3, List.of()),
            character("k_angel", "KAngel", "超天酱", "OMGKawaiiAngel", "超绝最可爱天使酱", 0, 1, 9, false, false, 38, 3, List.of()),
            character("ame", "Ame", "糖糖", "Needy Girl", "主播女孩", 1, 4, 9, false, false, 39, 3, List.of()),
            character("jill", "Jill", "吉尔", "Jill Stingray", "吉尔·斯汀雷", 1, 1, 10, false, false, 40, 3, List.of()),
            character("dorothy", "Dorothy", "多萝西", "Dorothy Haze", "多萝西·海兹", 1, 0, 8, false, false, 41, 2, List.of()));

    private static CharacterEntry character(String id, String enName, String zhName, String enTitle, String zhTitle, int attack, int defense, int health, boolean unlockedByDefault, int sortOrder, int activeCooldown, List<SkinEntry> skins) {
        return new CharacterEntry(id, enName, zhName, enTitle, zhTitle, attack, defense, health, unlockedByDefault, true, sortOrder, activeCooldown, skins);
    }

    private static CharacterEntry character(String id, String enName, String zhName, String enTitle, String zhTitle, int attack, int defense, int health, boolean unlockedByDefault, boolean implicitBondSkin, int sortOrder, int activeCooldown, List<SkinEntry> skins) {
        return new CharacterEntry(id, enName, zhName, enTitle, zhTitle, attack, defense, health, unlockedByDefault, implicitBondSkin, sortOrder, activeCooldown, skins);
    }

    private static SkinRarityEntry skinRarity(String id, String enName, String zhName, int borderColor, int badgeColor, int textColor) {
        return new SkinRarityEntry(id, enName, zhName, "skin_rarity.astral_craft." + id, borderColor, badgeColor, textColor);
    }

    private static SkinEntry skin(String id, String enName, String zhName, String rarity) {
        return new SkinEntry(id, enName, zhName, rarity);
    }

    public record CharacterEntry(String id, String enName, String zhName, String enTitle, String zhTitle, int attack, int defense, int health, boolean unlockedByDefault, boolean implicitBondSkin, int sortOrder, int activeCooldown, List<SkinEntry> skins) { }

    public record SkinEntry(String id, String enName, String zhName, String rarity) {}

    public record SkinRarityEntry(String id, String enName, String zhName, String nameKey, int borderColor, int badgeColor, int textColor) {}

}