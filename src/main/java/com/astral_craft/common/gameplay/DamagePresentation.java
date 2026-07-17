package com.astral_craft.common.gameplay;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Shared high-damage presentation used by battles and card effects. */
public class DamagePresentation {

    public static final int CRITICAL_DAMAGE_THRESHOLD = 7;

    public static void playCriticalImpact(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55D;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 34, 0.42D, 0.46D, 0.42D, 0.18D);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 12, 0.24D, 0.28D, 0.24D, 0.06D);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.82F);
    }

}