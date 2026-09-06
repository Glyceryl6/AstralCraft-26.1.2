package com.astral_craft.common.entity.projectile;

import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RisingCannonProjectileEntity extends AbstractCardProjectileEntity {

    private static final double SPAWN_HEIGHT = 7.0D;
    private static final double FALL_SPEED = 0.34D;

    public RisingCannonProjectileEntity(EntityType<? extends RisingCannonProjectileEntity> type, Level level) {
        super(type, level);
    }

    public RisingCannonProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, int durationTicks) {
        super(AstralEntities.RISING_CANNON_PROJECTILE.get(), level, owner, target, damage,
                CardProjectileSettings.of((float) FALL_SPEED, 0.018F, 0.0F, 0.0F, durationTicks));
        this.setPos(target.getX(), target.getY() + target.getBbHeight() + SPAWN_HEIGHT, target.getZ());
        this.setDeltaMovement(0.0D, -FALL_SPEED, 0.0D);
    }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.SMOKE, pos, 2, 0.07D, 0.01D);
        if (this.age() % 3 == 0) this.particle(level, ParticleTypes.POOF, pos, 1, 0.04D, 0.01D);
    }

    @Override
    protected void onImpact(ServerLevel level, LivingEntity owner, LivingEntity target) {
        double y = target.getY() + target.getBbHeight() * 0.45D;
        BoardWorldObjectService.playExplosion(level, target.getX(), y, target.getZ());
        this.damageTarget(owner, target);
    }
}
