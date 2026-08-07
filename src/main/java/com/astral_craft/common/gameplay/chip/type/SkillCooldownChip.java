package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import net.minecraft.resources.Identifier;

public class SkillCooldownChip extends ChipDefinition {

    private final int amount;

    public SkillCooldownChip(Identifier id, ChipRarity rarity, int amount) {
        super(id, rarity, ChipPool.CARDS);
        this.amount = amount;
    }

    @Override
    public int skillCooldownReduction() {
        return Math.max(0, this.amount);
    }
}
