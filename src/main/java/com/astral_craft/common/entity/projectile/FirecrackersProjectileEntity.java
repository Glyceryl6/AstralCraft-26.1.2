package com.astral_craft.common.entity.projectile;

import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Firecrackers / Gao-Sheng Cannon: a single lit firework-like projectile that flies upward then explodes. */
public class FirecrackersProjectileEntity extends AbstractCardProjectileEntity {

    public FirecrackersProjectileEntity(EntityType<? extends FirecrackersProjectileEntity> type, Level level) {
        super(type, level);
    }

    public FirecrackersProjectileEntity(Level level, ServerPlayer owner, LivingEntity target, int damage, int durationTicks) {
        super(AstralEntities.FIRECRACKERS_PROJECTILE.get(), level, owner, target, damage, durationTicks);
    }

    @Override
    protected float defaultSpeed() { return 0.92F; }

    @Override
    protected float defaultGravity() { return 0.025F; }

    @Override
    protected float defaultHoming() { return 0.15F; }

    @Override
    protected float defaultArcBoost() { return 0.42F; }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.FIREWORK, pos, 2, 0.04D, 0.01D);
        if (this.age() % 3 == 0) {
            this.particle(level, ParticleTypes.SMOKE, pos, 1, 0.02D, 0.0D);
        }
    }

    @Override
    protected void onImpact(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        this.damageTarget(owner, target);
        level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 42, 0.40D, 0.40D, 0.40D, 0.12D);
        level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.90F, 1.35F);
    }

}