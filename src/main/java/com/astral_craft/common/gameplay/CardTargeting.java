package com.astral_craft.common.gameplay;

import com.astral_craft.common.components.CardDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public final class CardTargeting {

    public static String encodeCandidates(ServerPlayer user, CardDefinition definition) {
        int range = definition.range() > 0 ? definition.range() : 32;
        AABB box = user.getBoundingBox().inflate(range);
        List<LivingEntity> entities = user.level().getEntitiesOfClass(LivingEntity.class, box, entity -> isValidTarget(user, entity, definition));
        entities.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(user)));
        StringBuilder builder = new StringBuilder();
        for (LivingEntity entity : entities) {
            if (!builder.isEmpty()) builder.append(';');
            int distance = (int) Math.ceil(Math.sqrt(entity.distanceToSqr(user)));
            builder.append(entity.getId()).append('|').append(clean(entity.getDisplayName().getString())).append('|').append(distance);
        }

        return builder.toString();
    }

    public static boolean isValidTarget(ServerPlayer user, LivingEntity entity, CardDefinition definition) {
        if (!entity.isAlive() || entity == user) return false;
        int range = definition.range() > 0 ? definition.range() : 32;
        if (entity.distanceToSqr(user) > range * range) return false;
        return switch (definition.targetMode()) {
            case ENEMY_PLAYER, ALLY -> entity instanceof Player;
            case ANY_PLAYER, TWO_PLAYERS -> entity instanceof Player || entity instanceof Mob;
            case MONSTER -> !(entity instanceof Player);
            default -> entity instanceof LivingEntity;
        };
    }

    private static String clean(String text) {
        return text.replace('|', '/').replace(';', ',');
    }

}