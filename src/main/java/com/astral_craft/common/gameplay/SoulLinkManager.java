package com.astral_craft.common.gameplay;

import com.astral_craft.common.entity.SoulLinkEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulLinkManager {

    private static final Map<UUID, Link> LINKS = new HashMap<>();
    private static boolean mirroringDamage;

    public static boolean isLinked(LivingEntity entity) {
        Link link = LINKS.get(entity.getUUID());
        if (link == null) {
            return false;
        }

        if (entity.level() instanceof ServerLevel level && level.getGameTime() > link.untilGameTime()) {
            remove(link);
            return false;
        }

        return true;
    }

    public static boolean link(LivingEntity first, LivingEntity second, long untilGameTime) {
        return link(first, second, untilGameTime, SoulLinkStyle.DEFAULT);
    }

    public static boolean link(LivingEntity first, LivingEntity second, long untilGameTime, SoulLinkStyle style) {
        if (!(first.level() instanceof ServerLevel level) || first.level() != second.level()) return false;
        if (isLinked(first) || isLinked(second)) return false;
        int lifetime = Math.max(1, (int) (untilGameTime - level.getGameTime()));
        SoulLinkEntity visual = new SoulLinkEntity(level, first, second, lifetime, style);
        level.addFreshEntity(visual);
        Link link = new Link(first.getUUID(), second.getUUID(), visual.getUUID(), untilGameTime);
        LINKS.put(first.getUUID(), link);
        LINKS.put(second.getUUID(), link);
        return true;
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (mirroringDamage || event.getNewDamage() <= 0.0F || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        Link link = LINKS.get(event.getEntity().getUUID());
        if (link == null) {
            return;
        }

        if (level.getGameTime() > link.untilGameTime()) {
            remove(link);
            return;
        }

        UUID otherId = link.other(event.getEntity().getUUID());
        Entity other = level.getEntity(otherId);
        if (!(other instanceof LivingEntity living) || !living.isAlive()) {
            remove(link);
            return;
        }

        mirroringDamage = true;
        try {
            living.hurtServer(level, event.getSource(), event.getNewDamage());
        } finally {
            mirroringDamage = false;
        }
    }

    public static void ensureVisual(ServerLevel level, LivingEntity first, LivingEntity second, SoulLinkStyle style) {
        if (level == null || first == null || second == null || first.level() != level || second.level() != level) return;
        AABB bounds = visualBounds(first, second);
        boolean present = level.getEntitiesOfClass(SoulLinkEntity.class, bounds, visual ->
                visual.firstId() == first.getId() && visual.secondId() == second.getId()
                        || visual.firstId() == second.getId() && visual.secondId() == first.getId()).stream().findAny().isPresent();
        if (present) return;
        SoulLinkEntity visual = new SoulLinkEntity(level, first, second, Integer.MAX_VALUE,
                style == null ? SoulLinkStyle.DEFAULT : style);
        level.addFreshEntity(visual);
    }

    public static void removeVisual(ServerLevel level, LivingEntity first, LivingEntity second) {
        if (level == null || first == null || second == null) return;
        for (SoulLinkEntity visual : level.getEntitiesOfClass(SoulLinkEntity.class, visualBounds(first, second), candidate ->
                candidate.firstId() == first.getId() && candidate.secondId() == second.getId()
                        || candidate.firstId() == second.getId() && candidate.secondId() == first.getId())) {
            visual.discard();
        }
    }

    private static AABB visualBounds(LivingEntity first, LivingEntity second) {
        double minX = Math.min(first.getX(), second.getX());
        double minY = Math.min(first.getY(), second.getY());
        double minZ = Math.min(first.getZ(), second.getZ());
        double maxX = Math.max(first.getX(), second.getX());
        double maxY = Math.max(first.getY() + first.getBbHeight(), second.getY() + second.getBbHeight());
        double maxZ = Math.max(first.getZ(), second.getZ());
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(8.0D);
    }

    public static void mirrorLogicalDamage(ServerLevel level, LivingEntity damaged, int amount) {
        if (mirroringDamage || level == null || damaged == null || amount <= 0) return;
        Link link = LINKS.get(damaged.getUUID());
        if (link == null) return;
        if (level.getGameTime() > link.untilGameTime()) {
            remove(link);
            return;
        }
        Entity other = level.getEntity(link.other(damaged.getUUID()));
        if (!(other instanceof LivingEntity living) || !living.isAlive()) {
            remove(link);
            return;
        }
        mirroringDamage = true;
        try {
            living.hurtServer(level, level.damageSources().generic(), amount);
        } finally {
            mirroringDamage = false;
        }
    }

    public static void remove(UUID entityId) {
        Link link = LINKS.get(entityId);
        if (link != null) {
            remove(link);
        }
    }

    public static void remove(Link link) {
        LINKS.remove(link.first());
        LINKS.remove(link.second());
    }

    public record Link(UUID first, UUID second, UUID visual, long untilGameTime) {
        public UUID other(UUID source) {
            return source.equals(this.first) ? this.second : this.first;
        }
    }

}