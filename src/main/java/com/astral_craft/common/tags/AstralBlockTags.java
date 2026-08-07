package com.astral_craft.common.tags;

import com.astral_craft.AstralCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class AstralBlockTags {

    public static final TagKey<Block> PVP_BOARD_PANELS = create("pvp_board_panels");
    public static final TagKey<Block> PVE_BOARD_PANELS = create("pve_board_panels");

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, AstralCraft.prefix(path));
    }
}
