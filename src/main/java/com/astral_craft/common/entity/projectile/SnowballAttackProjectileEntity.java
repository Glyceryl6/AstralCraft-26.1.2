package com.astral_craft.common.entity.projectile;

import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SnowballAttackProjectileEntity extends AbstractCardProjectileEntity {

    public SnowballAttackProjectileEntity(EntityType<? extends SnowballAttackProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SnowballAttackProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, CardProjectileSettings settings) {
        super(AstralEntities.SNOWBALL_ATTACK_PROJECTILE.get(), level, owner, target, damage, settings);
    }

    public SnowballAttackProjectileEntity(Level level, LivingEntity owner, LivingEntity target, int damage, int durationTicks) {
        this(level, owner, target, damage, CardProjectileSettings.of(1.05F, 0.026F, 0.12F, 0.12F, durationTicks));
    }

    @Override
    protected void spawnFlightParticles(ServerLevel level, Vec3 pos) {
        this.particle(level, ParticleTypes.SNOWFLAKE, pos, 2, 0.03D, 0.0D);
    }

    @Override
    protected void onImpact(ServerLevel level, LivingEntity owner, LivingEntity target) {
        this.damageTarget(owner, target);
        AstralCardEffects.update(target, AstralStats.getOrDefault(target).addBuff(AstralBoardBuffs.instance(AstralBoardBuffs.SNOWBALL_SLOW_ID, AstralBoardBuffs.SPEED.get()).duration(1).value(-4).build()));
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 28, 0.28D, 0.28D, 0.28D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 0.75F, 1.45F);
    }

}