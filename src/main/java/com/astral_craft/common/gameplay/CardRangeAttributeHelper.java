package com.astral_craft.common.gameplay;

import com.astral_craft.common.registry.AstralAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class CardRangeAttributeHelper {

    public static int rangeBonus(Player player) {
        if (player == null) return 0;
        AttributeInstance instance = player.getAttribute(AstralAttributes.HAND_CARD_RANGE);
        if (instance == null) return 0;
        return (int) Math.round(instance.getValue());
    }

    public static int applyRangeBonus(Player player, int baseRange) {
        return Math.max(0, baseRange + rangeBonus(player));
    }

    public static void addOrUpdateTransientModifier(LivingEntity entity, Identifier id, double amount) {
        AttributeInstance instance = instance(entity);
        if (instance == null || id == null) return;
        instance.addOrUpdateTransientModifier(modifier(id, amount));
    }

    public static void addOrReplacePermanentModifier(LivingEntity entity, Identifier id, double amount) {
        AttributeInstance instance = instance(entity);
        if (instance == null || id == null) return;
        instance.addOrReplacePermanentModifier(modifier(id, amount));
    }

    public static void removeModifier(LivingEntity entity, Identifier id) {
        AttributeInstance instance = instance(entity);
        if (instance == null || id == null) return;
        instance.removeModifier(id);
    }

    public static AttributeModifier modifier(Identifier id, double amount) {
        return new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    protected static AttributeInstance instance(LivingEntity entity) {
        if (entity == null) return null;
        return entity.getAttribute(AstralAttributes.HAND_CARD_RANGE);
    }

}