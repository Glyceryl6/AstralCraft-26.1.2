package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import net.minecraft.world.level.block.Block;

public class JumpPlatform extends BasePlatform {

    public JumpPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }
}
