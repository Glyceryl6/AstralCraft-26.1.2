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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Homing/parabolic visual projectile used by Firecrackers and Slingshot style cards. */
public class ArcProjectileEntity extends Entity {

    public static final int MODE_FIRECRACKER = 0;
    public static final int MODE_SLINGSHOT = 1;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DAMAGE = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_START_X = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Y = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Z = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HIT = SynchedEntityData.defineId(ArcProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    public ArcProjectileEntity(EntityType<? extends ArcProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public ArcProjectileEntity(Level level, ServerPlayer owner, LivingEntity target, int damage, int mode, int durationTicks) {
        this(AstralEntities.ARC_PROJECTILE.get(), level);
        Vec3 start = owner.position().add(0.0D, owner.getBbHeight() * 0.65D, 0.0D);
        this.setPos(start.x, start.y, start.z);
        this.entityData.set(DATA_OWNER, owner.getId());
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_DAMAGE, damage);
        this.entityData.set(DATA_MODE, mode);
        this.entityData.set(DATA_DURATION, Math.max(4, durationTicks));
        this.entityData.set(DATA_START_X, (float) start.x);
        this.entityData.set(DATA_START_Y, (float) start.y);
        this.entityData.set(DATA_START_Z, (float) start.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_DAMAGE, 0);
        builder.define(DATA_MODE, MODE_FIRECRACKER);
        builder.define(DATA_AGE, 0);
        builder.define(DATA_DURATION, 24);
        builder.define(DATA_START_X, 0.0F);
        builder.define(DATA_START_Y, 0.0F);
        builder.define(DATA_START_Z, 0.0F);
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

        Vec3 old = this.position();
        Vec3 next = this.point(this.age() / (float) this.durationTicks(), living);
        this.setPos(next.x, next.y, next.z);
        this.setDeltaMovement(next.subtract(old));

        if (!this.level().isClientSide() && this.age() >= this.durationTicks() && !this.hit()) {
            this.entityData.set(DATA_HIT, true);
            Entity owner = this.level().getEntity(this.ownerId());
            if (owner instanceof ServerPlayer player) {
                AstralCardEffects.damage(player, living, this.damage());
            }
        }
        if (!this.level().isClientSide() && this.age() >= this.durationTicks() + 12) {
            this.discard();
        }
    }

    public Vec3 start() {
        return new Vec3(this.entityData.get(DATA_START_X), this.entityData.get(DATA_START_Y), this.entityData.get(DATA_START_Z));
    }

    public Vec3 point(float rawT, LivingEntity target) {
        float t = Mth.clamp(rawT, 0.0F, 1.0F);
        Vec3 start = this.start();
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        double arc = Math.sin(t * Math.PI) * Math.max(1.2D, start.distanceTo(end) * 0.28D);
        return start.lerp(end, t).add(0.0D, arc, 0.0D);
    }

    public Vec3 tangent(float rawT, LivingEntity target) {
        float t0 = Mth.clamp(rawT - 0.025F, 0.0F, 1.0F);
        float t1 = Mth.clamp(rawT + 0.025F, 0.0F, 1.0F);
        Vec3 diff = this.point(t1, target).subtract(this.point(t0, target));
        if (diff.lengthSqr() < 1.0E-7D) return new Vec3(0.0D, 1.0D, 0.0D);
        return diff.normalize();
    }

    public int ownerId() { return this.entityData.get(DATA_OWNER); }
    public int targetId() { return this.entityData.get(DATA_TARGET); }
    public int damage() { return this.entityData.get(DATA_DAMAGE); }
    public int mode() { return this.entityData.get(DATA_MODE); }
    public int age() { return this.entityData.get(DATA_AGE); }
    public int durationTicks() { return this.entityData.get(DATA_DURATION); }
    public boolean hit() { return this.entityData.get(DATA_HIT); }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_OWNER, input.getIntOr("owner", -1));
        this.entityData.set(DATA_TARGET, input.getIntOr("target", -1));
        this.entityData.set(DATA_DAMAGE, input.getIntOr("damage", 0));
        this.entityData.set(DATA_MODE, input.getIntOr("mode", MODE_FIRECRACKER));
        this.entityData.set(DATA_AGE, input.getIntOr("age", 0));
        this.entityData.set(DATA_DURATION, input.getIntOr("duration", 24));
        this.entityData.set(DATA_START_X, input.getFloatOr("start_x", 0.0F));
        this.entityData.set(DATA_START_Y, input.getFloatOr("start_y", 0.0F));
        this.entityData.set(DATA_START_Z, input.getFloatOr("start_z", 0.0F));
        this.entityData.set(DATA_HIT, input.getBooleanOr("hit", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("owner", this.ownerId());
        output.putInt("target", this.targetId());
        output.putInt("damage", this.damage());
        output.putInt("mode", this.mode());
        output.putInt("age", this.age());
        output.putInt("duration", this.durationTicks());
        output.putFloat("start_x", (float) this.start().x);
        output.putFloat("start_y", (float) this.start().y);
        output.putFloat("start_z", (float) this.start().z);
        output.putBoolean("hit", this.hit());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }
}
