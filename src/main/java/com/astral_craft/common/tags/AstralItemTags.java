package com.astral_craft.common.tags;

import com.astral_craft.AstralCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class AstralItemTags {

    public static final TagKey<Item> BOARD_TOOLS = create("board_tools");

    private static TagKey<Item> create(String path) {
        return TagKey.create(Registries.ITEM, AstralCraft.prefix(path));
    }

}