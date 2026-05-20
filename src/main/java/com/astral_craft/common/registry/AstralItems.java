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