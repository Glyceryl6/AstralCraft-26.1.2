package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import net.minecraft.world.level.block.Block;

public class HospitalPlatform extends BasePlatform {

    public HospitalPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }
}
