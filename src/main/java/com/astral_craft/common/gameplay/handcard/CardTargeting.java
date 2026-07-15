package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.CardTargetCandidate;
import com.astral_craft.common.network.s2c.OpenTargetSelectionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class CardTargeting {

    public static List<CardTargetCandidate> candidates(ServerPlayer user, CardDefinition definition) {
        return candidates(user, ItemStack.EMPTY, definition, null);
    }

    public static List<CardTargetCandidate> candidates(ServerPlayer user, ItemStack stack, CardDefinition definition) {
        return candidates(user, stack, definition, null);
    }

    public static List<CardTargetCandidate> candidates(ServerPlayer user, ItemStack stack, CardDefinition definition, BaseHandCard card) {
        AABB box = user.getBoundingBox().inflate(Math.max(0, CardRangeResolver.targetingRange(user, stack, definition)));
        return user.level().getEntitiesOfClass(LivingEntity.class, box, entity -> isValidTarget(user, entity, stack, definition, card)).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(user))).limit(OpenTargetSelectionPayload.MAX_CANDIDATES)
                .map(entity -> new CardTargetCandidate(entity.getId(), entity.getDisplayName(), Mth.ceil(Math.sqrt(entity.distanceToSqr(user))))).toList();
    }

    public static boolean isValidTarget(ServerPlayer user, LivingEntity entity, CardDefinition definition) {
        return isValidTarget(user, entity, ItemStack.EMPTY, definition, null);
    }

    public static boolean isValidTarget(ServerPlayer user, LivingEntity entity, ItemStack stack, CardDefinition definition) {
        return isValidTarget(user, entity, stack, definition, null);
    }

    public static boolean isValidTarget(ServerPlayer user, LivingEntity entity, ItemStack stack, CardDefinition definition, BaseHandCard card) {
        if (!entity.isAlive()) return false;
        if (entity == user && (card == null || !card.allowsSelfTarget())) return false;
        int range = Math.max(0, CardRangeResolver.targetingRange(user, stack, definition));
        if (entity.distanceToSqr(user) > (double) range * range) return false;
        return definition.acceptsTarget(entity);
    }

}