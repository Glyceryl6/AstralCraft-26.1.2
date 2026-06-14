package com.astral_craft.common.entity.visual;

import com.astral_craft.common.gameplay.AstralCardEffects;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Falling brick/stone card visual. It follows the target horizontally while dropping from above. */
public class FallingBrickEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(FallingBrickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(FallingBrickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DAMAGE = SynchedEntityData.defineId(FallingBrickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(FallingBrickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FALL_TICKS = SynchedEntityData.defineId(FallingBrickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HIT = SynchedEntityData.defineId(FallingBrickEntity.class, EntityDataSerializers.BOOLEAN);

    public FallingBrickEntity(EntityType<? extends FallingBrickEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public FallingBrickEntity(Level level, ServerPlayer owner, LivingEntity target, int damage, int fallTicks) {
        this(AstralEntities.FALLING_BRICK.get(), level);
        this.entityData.set(DATA_OWNER, owner.getId());
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_DAMAGE, damage);
        this.entityData.set(DATA_FALL_TICKS, Math.max(4, fallTicks));
        this.setPos(target.getX(), target.getY() + target.getBbHeight() + 3.5D, target.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_DAMAGE, 0);
        builder.define(DATA_AGE, 0);
        builder.define(DATA_FALL_TICKS, 18);
        builder.define(DATA_HIT, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.entityData.set(DATA_AGE, this.age() + 1);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        Entity target = this.level().getEntity(this.targetId());
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }

        float t = Mth.clamp(this.age() / (float) this.fallTicks(), 0.0F, 1.0F);
        double impactY = living.getY() + living.getBbHeight() + 0.05D;
        double startY = impactY + 3.5D;
        this.setPos(living.getX(), Mth.lerp(t * t, startY, impactY), living.getZ());
        if (!this.level().isClientSide() && this.age() >= this.fallTicks() && !this.hit()) {
            this.entityData.set(DATA_HIT, true);
            Entity owner = this.level().getEntity(this.ownerId());
            if (owner instanceof ServerPlayer player) {
                AstralCardEffects.damageNow(player, living, this.damage());
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, living.getX(), living.getY() + living.getBbHeight(), living.getZ(), 28, 0.35D, 0.20D, 0.35D, 0.05D);
                    serverLevel.playSound(null, living.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.95F, 0.75F);
                }
            }
        }

        if (!this.level().isClientSide() && this.age() > this.fallTicks() + 16) {
            this.discard();
        }
    }

    public int ownerId() { return this.entityData.get(DATA_OWNER); }
    public int targetId() { return this.entityData.get(DATA_TARGET); }
    public int damage() { return this.entityData.get(DATA_DAMAGE); }
    public int age() { return this.entityData.get(DATA_AGE); }
    public int fallTicks() { return this.entityData.get(DATA_FALL_TICKS); }
    public boolean hit() { return this.entityData.get(DATA_HIT); }
    public float progress(float partialTick) { return Mth.clamp((this.age() + partialTick) / (float) this.fallTicks(), 0.0F, 1.0F); }
    public float breakProgress(float partialTick) { return Mth.clamp((this.age() + partialTick - this.fallTicks()) / 14.0F, 0.0F, 1.0F); }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_OWNER, input.getIntOr("owner", -1));
        this.entityData.set(DATA_TARGET, input.getIntOr("target", -1));
        this.entityData.set(DATA_DAMAGE, input.getIntOr("damage", 0));
        this.entityData.set(DATA_AGE, input.getIntOr("age", 0));
        this.entityData.set(DATA_FALL_TICKS, input.getIntOr("fall_ticks", 18));
        this.entityData.set(DATA_HIT, input.getBooleanOr("hit", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("owner", this.ownerId());
        output.putInt("target", this.targetId());
        output.putInt("damage", this.damage());
        output.putInt("age", this.age());
        output.putInt("fall_ticks", this.fallTicks());
        output.putBoolean("hit", this.hit());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

}