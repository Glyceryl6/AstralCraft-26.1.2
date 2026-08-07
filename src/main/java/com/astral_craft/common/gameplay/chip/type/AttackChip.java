package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class AttackChip extends ChipDefinition {

    private final int amount;

    public AttackChip(Identifier id, ChipRarity rarity, int amount) {
        this(id, rarity, amount, null);
    }

    public AttackChip(Identifier id, ChipRarity rarity, int amount, @Nullable Identifier keywordId) {
        super(id, rarity, ChipPool.ATTACK, keywordId);
        this.amount = amount;
    }

    @Override
    protected AstralPlayerStats applyStats(AstralPlayerStats stats) {
        return stats.addBaseAttack(this.amount);
    }

}