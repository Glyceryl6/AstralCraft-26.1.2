package com.astral_craft.common.gameplay.chip;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.gameplay.StatBundle;
import com.astral_craft.common.registry.AstralChips;

public class AstralBuiltinChips {

    public static final ChipDefinition BOXING_GLOVES_BASIC = chip("boxing_gloves_basic", ChipRarity.BLUE, null, new StatBundle(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition BOXING_GLOVES_FAIR = chip("boxing_gloves_fair", ChipRarity.PURPLE, null, new StatBundle(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition BOXING_GLOVES_EXCELLENT = chip("boxing_gloves_excellent", ChipRarity.GOLD, null, new StatBundle(5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition MOTORCYCLE_HELMET_REGULAR = chip("motorcycle_helmet_regular", ChipRarity.BLUE, null, new StatBundle(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition MOTORCYCLE_HELMET_FAIR = chip("motorcycle_helmet_fair", ChipRarity.PURPLE, null, new StatBundle(0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition MOTORCYCLE_HELMET_EXCELLENT = chip("motorcycle_helmet_excellent", ChipRarity.GOLD, null, new StatBundle(0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition SANDWICH_COOKIE_REGULAR = chip("sandwich_cookie_regular", ChipRarity.BLUE, null, new StatBundle(0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition SANDWICH_COOKIE_TASTY = chip("sandwich_cookie_tasty", ChipRarity.PURPLE, null, new StatBundle(0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition SANDWICH_COOKIE_DELICIOUS = chip("sandwich_cookie_delicious", ChipRarity.GOLD, null, new StatBundle(0, 0, 0, 5, 5, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition SPEED_SKATES_BASIC = chip("speed_skates_basic", ChipRarity.BLUE, null, new StatBundle(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition SPEED_SKATES_FAIR = chip("speed_skates_fair", ChipRarity.PURPLE, null, new StatBundle(0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition SPEED_SKATES_EXCELLENT = chip("speed_skates_excellent", ChipRarity.GOLD, null, new StatBundle(0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0));
    public static final ChipDefinition LARGE_BACKPACK = chip("large_backpack", ChipRarity.PURPLE, null, new StatBundle(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0));
    public static final ChipDefinition EXTRA_BATTERY = chip("extra_battery", ChipRarity.PURPLE, null, new StatBundle(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0));
    public static final ChipDefinition SMARTWATCH = chip("smartwatch", ChipRarity.PURPLE, null, StatBundle.EMPTY);
    public static final ChipDefinition PIGGY_BANK = chip("piggy_bank", ChipRarity.BLUE, null, StatBundle.EMPTY);
    public static final ChipDefinition BANK_CARD_LOW = chip("bank_card_low", ChipRarity.BLUE, AstralBoardBuffs.STARLIGHT.get(), new StatBundle(0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0));
    public static final ChipDefinition BANK_CARD_HIGH = chip("bank_card_high", ChipRarity.PURPLE, AstralBoardBuffs.STARLIGHT.get(), new StatBundle(0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0));
    public static final ChipDefinition MEDICAL_KIT_EMERGENCY = chip("medical_kit_emergency", ChipRarity.BLUE, AstralBoardBuffs.HEAL.get(), new StatBundle(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
    public static final ChipDefinition MEDICAL_KIT_FULL = chip("medical_kit_full", ChipRarity.GOLD, AstralBoardBuffs.HEAL.get(), new StatBundle(0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0));
    public static final ChipDefinition MARKING_SPRAY = chip("marking_spray", ChipRarity.BLUE, AstralBoardBuffs.MARK.get(), new StatBundle(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1));
    public static final ChipDefinition STANDARD_SIGHT = chip("standard_sight", ChipRarity.PURPLE, AstralBoardBuffs.MARK.get(), new StatBundle(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1));

    private static ChipDefinition chip(String id, ChipRarity rarity, BoardBuff keyword, StatBundle stats) {
        return AstralChips.register(new ChipDefinition(id, ChipDefinition.nameKey(id), ChipDefinition.effectKey(id), rarity, keyword, stats));
    }

}