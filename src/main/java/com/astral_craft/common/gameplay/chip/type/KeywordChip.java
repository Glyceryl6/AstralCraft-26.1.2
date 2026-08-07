package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import net.minecraft.resources.Identifier;

public class KeywordChip extends ChipDefinition {

    public KeywordChip(Identifier id, ChipRarity rarity, ChipPool pool, Identifier keywordId) {
        super(id, rarity, pool, keywordId);
    }
}
