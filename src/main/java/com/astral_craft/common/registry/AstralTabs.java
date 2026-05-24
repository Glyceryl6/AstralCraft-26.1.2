package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Stream;

public class AstralTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralCraft.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NORMAL_TAB = TABS.register("normal_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + AstralCraft.MOD_ID)).displayItems((_, output) -> {
                Stream<DeferredHolder<Item, ? extends Item>> stream = AstralItems.ITEMS.getEntries().stream();
                stream.map(Holder::value).map(ItemLike::asItem).forEach(output::accept);
            }).build());

}