package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.resources.Identifier;

public class BuffStackChip extends ChipDefinition {

    private final int amount;

    public BuffStackChip(Identifier id, ChipRarity rarity, ChipPool pool, Identifier keywordId, int amount) {
        super(id, rarity, pool, keywordId);
        this.amount = amount;
    }

    @Override
    protected AstralPlayerStats applyStats(AstralPlayerStats stats) {
        BoardBuff buff = this.keywordId().map(AstralBoardBuffs.REGISTRY::getValue).orElse(null);
        return buff == null ? stats : stats.addPermanentBuff(buff, this.amount);
    }
}
