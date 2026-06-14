package com.astral_craft.common.entity.projectile;

import com.astral_craft.common.gameplay.AstralCardEffects;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Snowball Attack projectile: damage plus temporary movement penalty when target is a player. */
public class SnowballAttackProjectileEntity extends AbstractCardProjectileEntity {
    public SnowballAttackProjectileEntity(EntityType<? extends SnowballAttackProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SnowballAttackProjectileEntity(Level level, ServerPlayer owner, LivingEntity target, int damage, int durationTicks) {
        super(AstralEntities.SNOWBALL_ATTACK_PROJECTILE.get(), level, owner, target, damage, durationTicks);
    }

    @Override
    protected float defaultSpeed() { return 1.05F; }

    @Override
    protected float defaultGravity() { return 0.026F; }

    @Override
    protected float defaultHoming() { return 0.12F; }

    @Override
    protected float defaultArcBoost() { return 0.12F; }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.SNOWFLAKE, pos, 2, 0.03D, 0.0D);
    }

    @Override
    protected void onImpact(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        this.damageTarget(owner, target);
        if (target instanceof ServerPlayer player) {
            AstralCardEffects.update(player, AstralStats.get(player).addTemporary("speed", -4, 1));
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 28, 0.28D, 0.28D, 0.28D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 0.75F, 1.45F);
    }
}
