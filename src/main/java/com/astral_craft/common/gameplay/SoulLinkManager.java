package com.astral_craft.common.gameplay;

import com.astral_craft.common.entity.SoulLinkEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SoulLinkManager {
    private static final Map<UUID, Link> LINKS = new HashMap<>();
    private static boolean mirroringDamage;

    private SoulLinkManager() {}

    public static void link(LivingEntity first, LivingEntity second, long untilGameTime) {
        link(first, second, untilGameTime, SoulLinkStyle.DEFAULT);
    }

    public static void link(LivingEntity first, LivingEntity second, long untilGameTime, SoulLinkStyle style) {
        if (!(first.level() instanceof ServerLevel level) || first.level() != second.level()) return;
        remove(first.getUUID());
        remove(second.getUUID());
        int lifetime = Math.max(1, (int) (untilGameTime - level.getGameTime()));
        SoulLinkEntity visual = new SoulLinkEntity(level, first, second, lifetime, style);
        level.addFreshEntity(visual);
        Link link = new Link(first.getUUID(), second.getUUID(), visual.getUUID(), untilGameTime);
        LINKS.put(first.getUUID(), link);
        LINKS.put(second.getUUID(), link);
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

    public static void remove(UUID entityId) {
        Link link = LINKS.get(entityId);
        if (link != null) {
            remove(link);
        }
    }

    private static void remove(Link link) {
        LINKS.remove(link.first());
        LINKS.remove(link.second());
    }

    private record Link(UUID first, UUID second, UUID visual, long untilGameTime) {
        private UUID other(UUID source) {
            return source.equals(first) ? second : first;
        }
    }

}
