package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import net.minecraft.world.level.block.Block;

public class EventPlatform extends BasePlatform {

    public EventPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }
}
