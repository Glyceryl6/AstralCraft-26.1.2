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
            character("parunan").setEnName("Parunan").setZhName("帕露南").setEnTitle("Lord of Commerce").setZhTitle("商业之主").hasPotential()
                    .setSkins(skin("fashion_icon", "Fashion Icon", "时尚达人", "sapphire"),
                            skin("fortune_guardian", "Fortune Guardian", "善财天宫", "amethyst")),
            character("fanny").setEnName("Fanny").setZhName("芬妮").setEnTitle("Quirky Detective").setZhTitle("古怪神探").hasPotential()
                    .setSkins(skin("mystic_insight", "Mystic Insight", "超然解析", "sapphire"),
                            skin("breezy_summer", "Breezy Summer", "清凉盛夏", "amethyst"), 
                            skin("mad_scientist", "Mad Scientist", "科学怪探", "amethyst"),
                            skin("leisurely_holiday", "Leisurely Holiday", "悠闲假日", "sapphire")),
            character("alana").setEnName("Alana").setZhName("阿兰娜").setEnTitle("Shy Nun").setZhTitle("社恐修女").hasPotential()
                    .setSkins(skin("clumsy_girl", "Clumsy Girl", "冒失少女", "sapphire"),
                            skin("throbbing_beach", "Throbbing Beach", "悸动海滩", "amethyst"), 
                            skin("witch_weapon", "Witch's Weapon", "魔女兵器", "amethyst")),
            character("komachi").setEnName("Komachi").setZhName("小町").setEnTitle("Shadow Ninja").setZhTitle("暗影忍者").hasPotential()
                    .setSkins(skin("why_the_ninja", "Why the Ninja?", "忍者为何？", "sapphire"),
                            skin("shadow_spirit", "Shadow Spirit", "暗影狼灵", "amethyst")),
            character("padman").setEnName("Padman").setZhName("派德曼").setEnTitle("Uncle Employee").setZhTitle("社员叔叔").hasPotential(),
            character("papara").setEnName("Papara").setZhName("帕帕拉").setEnTitle("Red Hot").setZhTitle("猩红辣妹").hasPotential()
                    .setSkins(skin("guard_captain", "Guard Captain", "护卫队长", "sapphire")),
            character("ren").setEnName("Ren").setZhName("恋").setEnTitle("Pro Gamer").setZhTitle("游戏大师").hasPotential()
                    .setSkins(skin("cheerleader", "Cheerleader", "啦啦队长", "sapphire"),
                            skin("snowy_spirit", "Snowy Spirit", "雪夜精灵", "sapphire"), 
                            skin("pool_party", "Pool Party", "泳池派对", "amethyst"), 
                            skin("cute_captain", "Cute Captain", "可爱船长", "amethyst"), 
                            skin("illusory_dealer", "Illusory Dealer", "卡牌魔术", "platinum")),
            character("mimi").setEnName("Mimi").setZhName("米米").setEnTitle("Kanban Musume").setZhTitle("看板娘").hasPotential()
                    .setSkins(skin("slushie_express", "Slushie Express", "冰沙快递", "sapphire"),
                            skin("transport_magic", "Transport Magic", "运输魔法", "amethyst"), 
                            skin("race_queen", "Race Queen", "赛车女郎", "emerald")),
            character("z3000").setEnName("Z3000").setZhName("Z3000").setEnTitle("Waste-Collecting Robot").setZhTitle("垃圾箱").hasPotential()
                    .setSkins(skin("sky_explorer_assistant", "Sky Explorer Assistant", "探天助手", "amethyst")),
            character("pandaman").setEnName("Pandaman").setZhName("潘大猛").setEnTitle("Human Chariot").setZhTitle("肉弹战车").hasPotential(),
            character("lulu").setEnName("Lulu").setZhName("璐璐").setEnTitle("Slime Girl").setZhTitle("史莱姆").hasPotential()
                    .setSkins(skin("mimic", "Mimic", "宝箱怪", "sapphire"),
                            skin("sweet_trap", "Sweet Trap", "甜蜜陷阱", "amethyst")),
            character("fen").setEnName("Fen").setZhName("枫").setEnTitle("Cheongsam Girl").setZhTitle("旗袍娘").hasPotential()
                    .setSkins(skin("maid_warrior", "Maid Warrior", "战斗女仆", "sapphire"),
                            skin("dragon_ascent", "Dragon's Ascent", "锦跃游龙", "amethyst"), 
                            skin("trendy_princess", "Trendy Princess", "潮流公主", "sapphire")),
            character("hai_qing").setEnName("Hai Qing").setZhName("蓝海晴").setEnTitle("Destiny Girl").setZhTitle("命运少女").hasPotential()
                    .setSkins(skin("stylish_grace", "Stylish Grace", "优雅身姿", "sapphire"),
                            skin("costume_party", "Costume Party", "化装舞会", "amethyst"), 
                            skin("spring_picnic", "Spring Picnic", "春日野餐", "amethyst"), 
                            skin("train_conductor", "Train Conductor", "列车长", "emerald")),
            character("misaki").setEnName("Misaki").setZhName("美咲").setEnTitle("Katana Master").setZhTitle("太刀使").hasPotential()
                    .setSkins(skin("maid_warrior", "Maid Warrior", "战斗女仆", "sapphire"),
                            skin("ink_wash_waltz", "Ink-Wash Waltz", "墨韵圆舞", "amethyst"), 
                            skin("witch_weapon", "Witch's Weapon", "魔女兵器", "amethyst"), 
                            skin("dragon_honor", "Dragon's Honor", "真龙御礼", "ultimate")),
            character("nardis").setEnName("Nardis").setZhName("娜蒂斯").setEnTitle("Oasis Queen").setZhTitle("绿洲女王")
                    .setSkins(skin("twilight_witch", "Twilight Witch", "暮色魔女", "sapphire"),
                            skin("festival_dress", "Festival Dress", "祭典礼装", "amethyst")),
            character("jasmine").setEnName("Jasmine").setZhName("茉莉").setEnTitle("Robot Maid").setZhTitle("家政机器人").hasPotential()
                    .setSkins(skin("coastal_surfer", "Coastal Surfer", "冲浪海岸", "sapphire"),
                            skin("festival_dress", "Festival Dress", "节日盛装", "amethyst"), 
                            skin("kitchen_master", "Kitchen Master", "后厨能手", "amethyst")),
            character("al").setEnName("A.L.").setZhName("阿尔").setEnTitle("Syndicate Prince").setZhTitle("暗区少主")
                    .setSkins(skin("ketchup_magic", "Ketchup Magic", "番茄酱魔法", "sapphire")),
            character("luka").setEnName("Luka").setZhName("星魅琉华").setEnTitle("Midnight Flash").setZhTitle("午夜闪光")
                    .setSkins(skin("special_gift", "Special Gift", "特别礼物", "sapphire"),
                            skin("fated_crossroads", "Fated Crossroads", "转角之期", "amethyst")),
            character("nancy_lu").setEnName("Nancy Lu").setZhName("南希露").setEnTitle("Cyber Phantom").setZhTitle("网络魅影").hasPotential()
                    .setSkins(skin("mealtime_magic", "Mealtime Magic", "餐前魔法", "sapphire"),
                            skin("heatwave_shores", "Heatwave Shores", "热浪沙滩", "amethyst"), 
                            skin("splendor_royale", "Splendor Royale", "雍容盛景", "ultimate")),
            character("rin").setEnName("Rin").setZhName("凛").setEnTitle("Rookie Investigator").setZhTitle("新人调查员")
                    .setSkins(skin("harbour_holiday", "Harbour Holiday", "海湾假日", "sapphire"),
                            skin("winter_glow", "Winter Glow", "冬日暖意", "amethyst")),
            character("megas").setEnName("Megas").setZhName("梅加斯").setEnTitle("Mech Hero").setZhTitle("机械超人").hasPotential()
                    .setSkins(skin("seaside_rescue", "Seaside Rescue", "海滨救援", "sapphire"),
                            skin("arcane_gunslinger", "Arcane Gunslinger", "魔枪表演", "platinum")),
            character("zhao").setEnName("Zhao").setZhName("姬梦朝").setEnTitle("Fengshui Master").setZhTitle("风水师")
                    .setSkins(skin("shoreline_memories", "Shoreline Memories", "滨边时光", "amethyst"),
                            skin("auspicious_blessing", "Auspicious Blessing", "祥瑞天章", "ultimate")),
            character("teru").setEnName("Teru").setZhName("照").setEnTitle("Hierarch of Mikami").setZhTitle("三神御主")
                    .setSkins(skin("little_nurse", "Little Nurse", "小护士", "sapphire"),
                            skin("spellbinding_singer", "Spellbinding Singer", "魔旅歌姬", "platinum")),
            character("moses").setEnName("Moses").setZhName("摩西").setEnTitle("Gunsmith").setZhTitle("枪匠")
                    .setSkins(skin("special_assignment", "Special Assignment", "特殊委派", "amethyst")),
            character("mamushi").setEnName("Mamushi").setZhName("真梦梓").setEnTitle("Jiao of the Mire").setZhTitle("沼之蛟龙")
                    .setSkins(skin("frost_dancer", "Frost Dancer", "冰上舞者", "amethyst")),
            character("ink_shadow").setEnName("Ink Shadow").setZhName("墨影").setEnTitle("Novice Hunter").setZhTitle("小猎手"),
            character("bonnie").setEnName("Bonnie").setZhName("邦妮").setEnTitle("Poisoned Apple").setZhTitle("毒苹果")
                    .setSkins(skin("urgent_invitation", "Urgent Invitation", "紧急邀约", "sapphire")),
            character("ling_ling").setEnName("Ling Ling").setZhName("铃铃").setEnTitle("Supernatural Forces").setZhTitle("怪力乱神"),
            character("sykes").setEnName("Sykes").setZhName("赛克斯").setEnTitle("Abyssal Larva").setZhTitle("魔渊幼体"),
            character("k_angel").setEnName("KAngel").setZhName("超天酱").setEnTitle("OMGKawaiiAngel").setZhTitle("超绝最可爱天使酱").hasPotential()
                    .setSkins(skin("internet_angel", "Internet Angel", "网络主播", "platinum")),
            character("ame").setEnName("Ame").setZhName("糖糖").setEnTitle("Needy Girl").setZhTitle("主播女孩").hasPotential()
                    .setSkins(skin("casual_daily", "Casual Daily", "休闲日常", "platinum")),
            character("jill").setEnName("Jill").setZhName("吉尔").setEnTitle("Jill Stingray").setZhTitle("吉尔·斯汀雷"),
            character("dorothy").setEnName("Dorothy").setZhName("多萝西").setEnTitle("Dorothy Haze").setZhTitle("多萝西·海兹"));

    private static CharacterEntry character(String id) {
        return new CharacterEntry().setId(id);
    }

    private static SkinRarityEntry skinRarity(String id, String enName, String zhName, int borderColor, int badgeColor, int textColor) {
        return new SkinRarityEntry(id, enName, zhName, "skin_rarity.astral_craft." + id, borderColor, badgeColor, textColor);
    }

    private static SkinEntry skin(String id, String enName, String zhName, String rarity) {
        return new SkinEntry(id, enName, zhName, rarity);
    }

    public static class CharacterEntry {
        
        public String id = "";
        public String enName = "";
        public String zhName = "";
        public String enTitle = "";
        public String zhTitle = "";
        public boolean hasPotential;
        public List<SkinEntry> skins = List.of();

        public CharacterEntry setId(String id) {
            this.id = id;
            return this;
        }

        public CharacterEntry setEnName(String enName) {
            this.enName = enName;
            return this;
        }

        public CharacterEntry setZhName(String zhName) {
            this.zhName = zhName;
            return this;
        }

        public CharacterEntry setEnTitle(String enTitle) {
            this.enTitle = enTitle;
            return this;
        }

        public CharacterEntry setZhTitle(String zhTitle) {
            this.zhTitle = zhTitle;
            return this;
        }

        public CharacterEntry hasPotential() {
            this.hasPotential = true;
            return this;
        }

        public CharacterEntry setSkins(List<SkinEntry> skins) {
            this.skins = List.copyOf(skins);
            return this;
        }

        public CharacterEntry setSkins(SkinEntry... skins) {
            return this.setSkins(List.of(skins));
        }

    }
    
    public record SkinEntry(String id, String enName, String zhName, String rarity) {}

    public record SkinRarityEntry(String id, String enName, String zhName, String nameKey, int borderColor, int badgeColor, int textColor) {}

}