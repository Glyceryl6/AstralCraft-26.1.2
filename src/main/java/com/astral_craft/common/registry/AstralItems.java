package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class AstralItems {

    // Platform
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AstralCraft.MOD_ID);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_CANDY_GHOST = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_CANDY_GHOST);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_CARD = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_CARD);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_DAMAGE = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_DAMAGE);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_DESTINY = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_DESTINY);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_DIVINE = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_DIVINE);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_EVENT = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_EVENT);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_FIRE = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_FIRE);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_GAMBLE = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_GAMBLE);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_GIFT = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_GIFT);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_GIMMICK = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_GIMMICK);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_GOLD = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_GOLD);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_HEAL = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_HEAL);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_HOSPITAL = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_HOSPITAL);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_JUMP = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_JUMP);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_LOTTERY = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_LOTTERY);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_MONSTER = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_MONSTER);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_MOVE_AGAIN = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_MOVE_AGAIN);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_RELIC = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_RELIC);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_SHOP = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_SHOP);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_START = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_START);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_TELEPORT = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_TELEPORT);
    public static final DeferredHolder<Item, ? extends Item> PLATFORM_TELEPORT_POINT = ITEMS.registerSimpleBlockItem(AstralBlocks.PLATFORM_TELEPORT_POINT);

    // Handcard
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ATTACK_M = register("handcard_attack_m", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ATTACK_L = register("handcard_attack_l", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ATTACK_G = register("handcard_attack_g", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEFENSE_M = register("handcard_defense_m", BaseHandCard::new, CardType.DEFENSE);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEFENSE_L = register("handcard_defense_l", BaseHandCard::new, CardType.DEFENSE);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEFENSE_G = register("handcard_defense_g", BaseHandCard::new, CardType.DEFENSE);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_CHOCOLATE_CAKE = register("handcard_chocolate_cake", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_HAMBURGER = register("handcard_hamburger", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SMART_DICE = register("handcard_smart_dice", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FIGHT_FIRE_WITH_FIRE = register("handcard_fight_fire_with_fire", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BERSERK = register("handcard_berserk", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BARRICADE = register("handcard_barricade", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_REDIRECTION = register("handcard_redirection", BaseHandCard::new, CardType.EFFECT);

    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ENTRAPMENT = register("handcard_entrapment", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SOUL_LINK = register("handcard_soul_link", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SELF_EXPLOSION = register("handcard_self_explosion", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SLINGSHOT = register("handcard_slingshot", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEMOLITION = register("handcard_demolition", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_EXPIRED_BENTO = register("handcard_expired_bento", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ALL_OR_NOTHING = register("handcard_all_or_nothing", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_TIME_BOMB = register("handcard_time_bomb", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SNATCH = register("handcard_snatch", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SCAVENGING = register("handcard_scavenging", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RANDOM_PORTAL = register("handcard_random_portal", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DIRECTED_BOOST = register("handcard_directed_boost", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SNOWBALL_ATTACK = register("handcard_snowball_attack", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BARRIER = register("handcard_barrier", BaseHandCard::new, CardType.COUNTER);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_EYE_FOR_AN_EYE = register("handcard_eye_for_an_eye", BaseHandCard::new, CardType.COUNTER);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RANDOM_SELECT = register("handcard_random_select", BaseHandCard::new, CardType.COUNTER);

    public static final DeferredHolder<Item, ? extends Item> HANDCARD_GAWU_CUT = register("handcard_gawu_cut", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SHADOW_ATTACK = register("handcard_shadow_attack", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_CHARGE = register("handcard_charge", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_POWERFUL_ATTACK = register("handcard_powerful_attack", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_POISON_FANG = register("handcard_poison_fang", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BITE = register("handcard_bite", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DRAGON_ROAR = register("handcard_dragon_roar", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FATE_GUIDANCE = register("handcard_fate_guidance", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RAILGUN = register("handcard_railgun", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BOTH_HAVE = register("handcard_both_have", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_HURRY = register("handcard_hurry", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_KING_POWER = register("handcard_king_power", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_IMMOVABLE = register("handcard_immovable", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_LUXURIOUS_FEAST = register("handcard_luxurious_feast", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BLAST = register("handcard_blast", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SUPPORT = register("handcard_support", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FIRECRACKERS = register("handcard_firecrackers", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SUPPORT_GUM = register("handcard_support_gum", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ENERGY_BAR = register("handcard_energy_bar", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SMARTIE_GUMMY = register("handcard_smartie_gummy", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_QUIRKY_ENCHANTED = register("handcard_quirky_enchanted", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RELEASE_AZURE_SOUL = register("handcard_release_azure_soul", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RELEASE_SCARLET_SOUL = register("handcard_release_scarlet_soul", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_COLOURFUL_FEATHER = register("handcard_colourful_feather", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_CHEER_UP = register("handcard_cheer_up", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_OVERFLOWING_FORTUNE = register("handcard_overflowing_fortune", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ASHEN_FEATHER = register("handcard_ashen_feather", BaseHandCard::new, CardType.JINX);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_PROBLEM_STUDENT = register("handcard_problem_student", BaseHandCard::new, CardType.JINX);

    public static final DeferredHolder<Item, ? extends Item> HANDCARD_LASER = register("handcard_laser", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BRICK = register("handcard_brick", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FORTUNE = register("handcard_fortune", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_MISFORTUNE = register("handcard_misfortune", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_LIVING_BOOK = register("handcard_living_book", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ENHANCED_BARRICADE = register("handcard_enhanced_barricade", BaseHandCard::new, CardType.EFFECT);

    public static DeferredHolder<Item, ? extends Item> register(String name, Function<Item.Properties, Item> itemFactory) {
        return register(name, itemFactory, Item.Properties::new);
    }

    public static DeferredHolder<Item, ? extends Item> register(String name, Function<Item.Properties, Item> itemFactory, CardType cardType) {
        return register(name, itemFactory, () -> new Item.Properties().component(AstralDataComponents.CARD_TYPE, cardType));
    }

    public static DeferredHolder<Item, ? extends Item> register(String name, Function<Item.Properties, Item> itemFactory, Supplier<Item.Properties> properties) {
        return ITEMS.registerItem(name, itemFactory, () -> properties.get().setId(ResourceKey.create(Registries.ITEM, AstralCraft.prefix(name))));
    }

}