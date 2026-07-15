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

public class SlingshotProjectileEntity extends AbstractCardProjectileEntity {

    public SlingshotProjectileEntity(EntityType<? extends SlingshotProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SlingshotProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, CardProjectileSettings settings) {
        super(AstralEntities.SLINGSHOT_PROJECTILE.get(), level, owner, target, damage, settings);
    }

    public SlingshotProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, int durationTicks) {
        this(level, owner, target, damage, CardProjectileSettings.of(1.15F, 0.018F, 0.10F, 0.08F, durationTicks));
    }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.CRIT, pos, 2, 0.025D, 0.0D);
    }

    @Override
    protected void onImpact(ServerLevel level, LivingEntity owner, LivingEntity target) {
        this.damageTarget(owner, target);
        level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 18, 0.18D, 0.18D, 0.18D, 0.04D);
        level.playSound(null, target.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

}