package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import net.minecraft.world.level.block.Block;

public class DestinyPlatform extends BasePlatform {

    public DestinyPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }
}
