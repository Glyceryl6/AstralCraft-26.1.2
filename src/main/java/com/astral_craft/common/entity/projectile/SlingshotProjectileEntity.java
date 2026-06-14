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

/** Small cube projectile for Slingshot. */
public class SlingshotProjectileEntity extends AbstractCardProjectileEntity {
    public SlingshotProjectileEntity(EntityType<? extends SlingshotProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SlingshotProjectileEntity(Level level, ServerPlayer owner, LivingEntity target, int damage, int durationTicks) {
        super(AstralEntities.SLINGSHOT_PROJECTILE.get(), level, owner, target, damage, durationTicks);
    }

    @Override
    protected float defaultSpeed() { return 1.15F; }

    @Override
    protected float defaultGravity() { return 0.018F; }

    @Override
    protected float defaultHoming() { return 0.10F; }

    @Override
    protected float defaultArcBoost() { return 0.08F; }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.CRIT, pos, 2, 0.025D, 0.0D);
    }

    @Override
    protected void onImpact(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        this.damageTarget(owner, target);
        level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 18, 0.18D, 0.18D, 0.18D, 0.04D);
        level.playSound(null, target.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.7F, 1.6F);
    }
}
