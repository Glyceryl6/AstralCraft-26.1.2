package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.resources.Identifier;

public class MaxHealthChip extends ChipDefinition {

    private final int amount;

    public MaxHealthChip(Identifier id, ChipRarity rarity, int amount) {
        super(id, rarity, ChipPool.SUSTAIN);
        this.amount = amount;
    }

    @Override
    protected AstralPlayerStats applyStats(AstralPlayerStats stats) {
        return stats.addMaxHealth(this.amount);
    }
}
