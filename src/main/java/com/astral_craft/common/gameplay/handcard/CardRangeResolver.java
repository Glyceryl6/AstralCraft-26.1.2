package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.components.CardDefinition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CardRangeResolver {

    public static final int DEFAULT_TARGET_RANGE = 32;

    public static int baseRange(CardDefinition definition) {
        if (definition == null) return -1;
        if (definition.needsTarget() && definition.range() <= 0) {
            return DEFAULT_TARGET_RANGE;
        }
        return definition.range();
    }

    public static int effectiveRange(Player player, ItemStack stack, CardDefinition definition) {
        if (definition == null) return -1;
        int baseRange = baseRange(definition);
        if (!definition.needsTarget()) {
            return baseRange;
        }
        return CardRangeAttributeHelper.applyRangeBonus(player, baseRange);
    }

    public static int targetingRange(Player player, ItemStack stack, CardDefinition definition) {
        int range = effectiveRange(player, stack, definition);
        return range > 0 ? range : DEFAULT_TARGET_RANGE;
    }

    public static CardDefinition effectiveDefinition(Player player, ItemStack stack, CardDefinition definition) {
        if (definition == null || !definition.needsTarget()) {
            return definition;
        }
        return definition.withRange(effectiveRange(player, stack, definition));
    }

    public static boolean rangeChanged(Player player, ItemStack stack, CardDefinition definition) {
        return definition != null && definition.needsTarget() && baseRange(definition) != effectiveRange(player, stack, definition);
    }

}