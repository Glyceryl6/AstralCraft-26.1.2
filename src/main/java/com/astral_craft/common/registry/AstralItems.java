package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class AstralItems {

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

    public static DeferredHolder<Item, ? extends Item> register(
            String name, Function<Item.Properties, Item> itemFactory) {
        return register(name, itemFactory, Item.Properties::new);
    }

    public static DeferredHolder<Item, ? extends Item> register(
            String name, Function<Item.Properties, Item> itemFactory, Supplier<Item.Properties> properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, AstralCraft.prefix(name));
        return ITEMS.registerItem(name, itemFactory, () -> properties.get().setId(key));
    }

}