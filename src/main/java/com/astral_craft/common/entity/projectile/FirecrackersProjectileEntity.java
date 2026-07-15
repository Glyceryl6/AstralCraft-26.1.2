package com.astral_craft.common.entity.projectile;

import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FirecrackersProjectileEntity extends AbstractCardProjectileEntity {

    public FirecrackersProjectileEntity(EntityType<? extends FirecrackersProjectileEntity> type, Level level) {
        super(type, level);
    }

    public FirecrackersProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, CardProjectileSettings settings) {
        super(AstralEntities.FIRECRACKERS_PROJECTILE.get(), level, owner, target, damage, settings);
    }

    public FirecrackersProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, int durationTicks) {
        this(level, owner, target, damage, CardProjectileSettings.of(0.92F, 0.025F, 0.15F, 0.42F, durationTicks));
    }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.FIREWORK, pos, 2, 0.04D, 0.01D);
        if (this.age() % 3 == 0) {
            this.particle(level, ParticleTypes.SMOKE, pos, 1, 0.02D, 0.0D);
        }
    }

    @Override
    protected void onImpact(ServerLevel level, LivingEntity owner, LivingEntity target) {
        this.damageTarget(owner, target);
        level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 42, 0.40D, 0.40D, 0.40D, 0.12D);
        level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.90F, 1.35F);
    }

}