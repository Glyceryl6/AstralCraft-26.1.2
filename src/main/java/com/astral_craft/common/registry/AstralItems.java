package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.items.BaseHandCard;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class AstralItems {

    public static final List<ModelledCardItem> MODELLED_CARD_ITEMS = new ArrayList<>();

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
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ATTACK_M = registerCard("handcard_attack_m", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ATTACK_L = registerCard("handcard_attack_l", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ATTACK_G = registerCard("handcard_attack_g", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEFENSE_M = registerCard("handcard_defense_m", BaseHandCard::new, CardType.DEFENSE);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEFENSE_L = registerCard("handcard_defense_l", BaseHandCard::new, CardType.DEFENSE);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEFENSE_G = registerCard("handcard_defense_g", BaseHandCard::new, CardType.DEFENSE);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_CHOCOLATE_CAKE = registerCard("handcard_chocolate_cake", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_HAMBURGER = registerCard("handcard_hamburger", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SMART_DICE = registerCard("handcard_smart_dice", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FIGHT_FIRE_WITH_FIRE = registerCard("handcard_fight_fire_with_fire", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BERSERK = registerCard("handcard_berserk", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BARRICADE = registerCard("handcard_barricade", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_REDIRECTION = registerCard("handcard_redirection", BaseHandCard::new, CardType.EFFECT);

    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ENTRAPMENT = registerCard("handcard_entrapment", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SOUL_LINK = registerCard("handcard_soul_link", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SELF_EXPLOSION = registerCard("handcard_self_explosion", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SLINGSHOT = registerCard("handcard_slingshot", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DEMOLITION = registerCard("handcard_demolition", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_EXPIRED_BENTO = registerCard("handcard_expired_bento", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ALL_OR_NOTHING = registerCard("handcard_all_or_nothing", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_TIME_BOMB = registerCard("handcard_time_bomb", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SNATCH = registerCard("handcard_snatch", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SCAVENGING = registerCard("handcard_scavenging", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RANDOM_PORTAL = registerCard("handcard_random_portal", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DIRECTED_BOOST = registerCard("handcard_directed_boost", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SNOWBALL_ATTACK = registerCard("handcard_snowball_attack", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BARRIER = registerCard("handcard_barrier", BaseHandCard::new, CardType.COUNTER);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_EYE_FOR_AN_EYE = registerCard("handcard_eye_for_an_eye", BaseHandCard::new, CardType.COUNTER);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RANDOM_SELECT = registerCard("handcard_random_select", BaseHandCard::new, CardType.COUNTER);

    public static final DeferredHolder<Item, ? extends Item> HANDCARD_GAWU_CUT = registerCard("handcard_gawu_cut", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SHADOW_ATTACK = registerCard("handcard_shadow_attack", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_CHARGE = registerCard("handcard_charge", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_POWERFUL_ATTACK = registerCard("handcard_powerful_attack", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_POISON_FANG = registerCard("handcard_poison_fang", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BITE = registerCard("handcard_bite", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_DRAGON_ROAR = registerCard("handcard_dragon_roar", BaseHandCard::new, CardType.ATTACK);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FATE_GUIDANCE = registerCard("handcard_fate_guidance", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RAILGUN = registerCard("handcard_railgun", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BOTH_HAVE = registerCard("handcard_both_have", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_HURRY = registerCard("handcard_hurry", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_KING_POWER = registerCard("handcard_king_power", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_IMMOVABLE = registerCard("handcard_immovable", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_LUXURIOUS_FEAST = registerCard("handcard_luxurious_feast", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BLAST = registerCard("handcard_blast", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SUPPORT = registerCard("handcard_support", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FIRECRACKERS = registerCard("handcard_firecrackers", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SUPPORT_GUM = registerCard("handcard_support_gum", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ENERGY_BAR = registerCard("handcard_energy_bar", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_SMARTIE_GUMMY = registerCard("handcard_smartie_gummy", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_QUIRKY_ENCHANTED = registerCard("handcard_quirky_enchanted", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RELEASE_AZURE_SOUL = registerCard("handcard_release_azure_soul", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_RELEASE_SCARLET_SOUL = registerCard("handcard_release_scarlet_soul", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_COLOURFUL_FEATHER = registerCard("handcard_colourful_feather", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_CHEER_UP = registerCard("handcard_cheer_up", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_OVERFLOWING_FORTUNE = registerCard("handcard_overflowing_fortune", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ASHEN_FEATHER = registerCard("handcard_ashen_feather", BaseHandCard::new, CardType.JINX);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_PROBLEM_STUDENT = registerCard("handcard_problem_student", BaseHandCard::new, CardType.JINX);

    public static final DeferredHolder<Item, ? extends Item> HANDCARD_LASER = registerCard("handcard_laser", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_BRICK = registerCard("handcard_brick", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_FORTUNE = registerCard("handcard_fortune", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_MISFORTUNE = registerCard("handcard_misfortune", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_LIVING_BOOK = registerCard("handcard_living_book", BaseHandCard::new, CardType.EFFECT);
    public static final DeferredHolder<Item, ? extends Item> HANDCARD_ENHANCED_BARRICADE = registerCard("handcard_enhanced_barricade", BaseHandCard::new, CardType.EFFECT);

    public static DeferredHolder<Item, ? extends Item> register(String name, Function<Item.Properties, Item> itemFactory) {
        return register(name, itemFactory, Item.Properties::new);
    }

    public static DeferredHolder<Item, ? extends Item> registerCard(String name, Function<Item.Properties, Item> itemFactory, CardType cardType) {
        DeferredHolder<Item, ? extends Item> register = register(name, itemFactory, () ->
                new Item.Properties().component(AstralDataComponents.CARD_TYPE, cardType));
        MODELLED_CARD_ITEMS.add(new ModelledCardItem(register, cardType));
        return register;
    }

    public static DeferredHolder<Item, ? extends Item> register(String name, Function<Item.Properties, Item> itemFactory, Supplier<Item.Properties> properties) {
        return ITEMS.registerItem(name, itemFactory, () -> properties.get().setId(ResourceKey.create(Registries.ITEM, AstralCraft.prefix(name))));
    }

    public record ModelledCardItem(DeferredHolder<Item, ? extends Item> item, CardType cardType) {}

}