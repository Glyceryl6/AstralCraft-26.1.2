package com.astral_craft.common.entity.projectile;

import com.astral_craft.common.gameplay.AstralCardEffects;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for card projectiles.
 *
 * <p>This now extends {@link Projectile}, so callers can tune launch direction, speed, homing,
 * gravity and arc height through projectile-like concepts. The entity still supports target tracking
 * because many Astral Party cards are selected-target effects rather than free-aim weapons.</p>
 */
public abstract class AbstractCardProjectileEntity extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DAMAGE = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_AGE = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GRAVITY = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HOMING = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ARC_BOOST = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HIT = SynchedEntityData.defineId(AbstractCardProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    protected AbstractCardProjectileEntity(EntityType<? extends AbstractCardProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    protected AbstractCardProjectileEntity(EntityType<? extends AbstractCardProjectileEntity> type, Level level, ServerPlayer owner, LivingEntity target, int damage, int durationTicks) {
        this(type, level);
        Vec3 start = this.defaultStart(owner);
        this.setOwner(owner);
        this.setPos(start.x, start.y, start.z);
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_DAMAGE, damage);
        this.entityData.set(DATA_MAX_AGE, Math.max(4, durationTicks));
        this.configureFlight(this.defaultSpeed(), this.defaultGravity(),
                this.defaultHoming(), this.defaultArcBoost());
        this.shootAt(target, this.speed(), 0.0F);
    }

    protected Vec3 defaultStart(ServerPlayer owner) {
        return owner.position().add(0.0D, owner.getBbHeight() * 0.65D, 0.0D);
    }

    protected float defaultSpeed() { return 0.82F; }
    protected float defaultGravity() { return 0.030F; }
    protected float defaultHoming() { return 0.18F; }
    protected float defaultArcBoost() { return 0.22F; }

    public void configureFlight(float speed, float gravity, float homing, float arcBoost) {
        this.entityData.set(DATA_SPEED, Math.max(0.05F, speed));
        this.entityData.set(DATA_GRAVITY, Math.max(0.0F, gravity));
        this.entityData.set(DATA_HOMING, Mth.clamp(homing, 0.0F, 1.0F));
        this.entityData.set(DATA_ARC_BOOST, Math.max(0.0F, arcBoost));
    }

    /** Set the initial velocity using Projectile#shoot so speed/divergence feel like vanilla projectiles. */
    public void shootAt(LivingEntity target, float speed, float divergence) {
        Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 toTarget = targetPos.subtract(this.position());
        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        Vec3 aim = new Vec3(toTarget.x, toTarget.y + horizontal * this.arcBoost(), toTarget.z);
        if (aim.lengthSqr() < 1.0E-7D) aim = new Vec3(0.0D, 0.1D, 0.0D);
        this.shoot(aim.x, aim.y, aim.z, speed, divergence);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_DAMAGE, 0);
        builder.define(DATA_AGE, 0);
        builder.define(DATA_MAX_AGE, 24);
        builder.define(DATA_SPEED, 0.8F);
        builder.define(DATA_GRAVITY, 0.03F);
        builder.define(DATA_HOMING, 0.18F);
        builder.define(DATA_ARC_BOOST, 0.22F);
        builder.define(DATA_HIT, false);
    }

    @Override
    public void tick() {
        super.tick();
        int nextAge = this.age() + 1;
        this.entityData.set(DATA_AGE, nextAge);
        Entity targetEntity = this.level().getEntity(this.targetId());
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }

        if (!this.hit()) {
            this.steerToward(target);
            Vec3 motion = this.getDeltaMovement();
            this.move(MoverType.SELF, motion);
            this.setDeltaMovement(motion.x, motion.y - this.gravity(), motion.z);
            this.updateRotation();
            if (this.level() instanceof ServerLevel serverLevel) {
                this.spawnFlightParticles(serverLevel, this.position());
            }

            if (!this.level().isClientSide() && (this.intersectsTarget(target) || nextAge >= this.maxAge())) {
                this.entityData.set(DATA_HIT, true);
                Entity owner = this.getOwner();
                if (owner instanceof ServerPlayer player) {
                    this.onImpact(player.level(), player, target);
                }

                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private void steerToward(LivingEntity target) {
        float homing = this.homing();
        if (homing <= 0.0F) return;
        Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 wanted = targetPos.subtract(this.position());
        if (wanted.lengthSqr() < 1.0E-7D) return;
        Vec3 current = this.getDeltaMovement();
        double currentSpeed = Math.max(0.05D, current.length());
        Vec3 next = current.normalize().lerp(wanted.normalize(), homing).normalize().scale(currentSpeed);
        this.setDeltaMovement(next);
    }

    private boolean intersectsTarget(LivingEntity target) {
        return this.getBoundingBox().inflate(0.25D).intersects(target.getBoundingBox().inflate(0.20D))
                || this.position().distanceToSqr(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)) < 0.55D;
    }

    protected void damageTarget(ServerPlayer owner, LivingEntity target) {
        AstralCardEffects.damageNow(owner, target, this.damage());
        this.discard();
    }

    protected void particle(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    protected abstract void spawnFlightParticles(ServerLevel level, Vec3 pos);

    protected abstract void onImpact(ServerLevel level, ServerPlayer owner, LivingEntity target);

    public Vec3 tangent() {
        Vec3 velocity = this.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-7D) return new Vec3(0.0D, 1.0D, 0.0D);
        return velocity.normalize();
    }

    public int targetId() { return this.entityData.get(DATA_TARGET); }
    public int damage() { return this.entityData.get(DATA_DAMAGE); }
    public int age() { return this.entityData.get(DATA_AGE); }
    public int maxAge() { return this.entityData.get(DATA_MAX_AGE); }
    public int durationTicks() { return this.maxAge(); }
    public float speed() { return this.entityData.get(DATA_SPEED); }
    public float gravity() { return this.entityData.get(DATA_GRAVITY); }
    public float homing() { return this.entityData.get(DATA_HOMING); }
    public float arcBoost() { return this.entityData.get(DATA_ARC_BOOST); }
    public boolean hit() { return this.entityData.get(DATA_HIT); }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_TARGET, input.getIntOr("target", -1));
        this.entityData.set(DATA_DAMAGE, input.getIntOr("damage", 0));
        this.entityData.set(DATA_AGE, input.getIntOr("age", 0));
        this.entityData.set(DATA_MAX_AGE, input.getIntOr("max_age", 24));
        this.entityData.set(DATA_SPEED, input.getFloatOr("speed", 0.8F));
        this.entityData.set(DATA_GRAVITY, input.getFloatOr("gravity", 0.03F));
        this.entityData.set(DATA_HOMING, input.getFloatOr("homing", 0.18F));
        this.entityData.set(DATA_ARC_BOOST, input.getFloatOr("arc_boost", 0.22F));
        this.entityData.set(DATA_HIT, input.getBooleanOr("hit", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("target", this.targetId());
        output.putInt("damage", this.damage());
        output.putInt("age", this.age());
        output.putInt("max_age", this.maxAge());
        output.putFloat("speed", this.speed());
        output.putFloat("gravity", this.gravity());
        output.putFloat("homing", this.homing());
        output.putFloat("arc_boost", this.arcBoost());
        output.putBoolean("hit", this.hit());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

}