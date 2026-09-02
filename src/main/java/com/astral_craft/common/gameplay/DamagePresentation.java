package com.astral_craft.common.gameplay;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Shared damage presentation used by battles, card effects and board state changes. */
public class DamagePresentation {

    public static final int CRITICAL_DAMAGE_THRESHOLD = 7;

    public static void playDamageImpact(ServerLevel level, LivingEntity target) {
        if (level == null || target == null) return;
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55D;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 12,
                target.getBbWidth() * 0.34D, target.getBbHeight() * 0.28D, target.getBbWidth() * 0.34D, 0.12D);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 7,
                target.getBbWidth() * 0.24D, target.getBbHeight() * 0.20D, target.getBbWidth() * 0.24D, 0.08D);
    }

    public static void playCriticalImpact(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55D;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 34, 0.42D, 0.46D, 0.42D, 0.18D);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 12, 0.24D, 0.28D, 0.24D, 0.06D);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.82F);
    }

}