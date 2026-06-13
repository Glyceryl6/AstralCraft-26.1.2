package com.astral_craft.common.entity.visual;

import com.astral_craft.common.gameplay.AstralCardEffects;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Server-owned visual controller for top-down laser cards.
 *
 * <p>The renderer grows a tube from a point above the target down to the ground. Damage is applied once the tube
 * reaches the target/ground, then the tube fades by shrinking its radius.</p>
 */
public class LaserStrikeEntity extends Entity {

    public static final int DEFAULT_GROW_TICKS = 14;
    public static final int DEFAULT_HOLD_TICKS = 7;
    public static final int DEFAULT_FADE_TICKS = 16;
    public static final float DEFAULT_HEIGHT = 8.0F;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DAMAGE = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GROW = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOLD = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FADE = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DAMAGED = SynchedEntityData.defineId(LaserStrikeEntity.class, EntityDataSerializers.BOOLEAN);

    public LaserStrikeEntity(EntityType<? extends LaserStrikeEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public LaserStrikeEntity(Level level, ServerPlayer owner, LivingEntity target, int damage, int color, float radius) {
        this(AstralEntities.LASER_STRIKE.get(), level);
        this.setPos(target.getX(), target.getY(), target.getZ());
        this.entityData.set(DATA_OWNER, owner.getId());
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_DAMAGE, damage);
        this.entityData.set(DATA_COLOR, color);
        this.entityData.set(DATA_RADIUS, Math.max(0.03F, radius));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_DAMAGE, 0);
        builder.define(DATA_AGE, 0);
        builder.define(DATA_GROW, DEFAULT_GROW_TICKS);
        builder.define(DATA_HOLD, DEFAULT_HOLD_TICKS);
        builder.define(DATA_FADE, DEFAULT_FADE_TICKS);
        builder.define(DATA_HEIGHT, DEFAULT_HEIGHT);
        builder.define(DATA_RADIUS, 0.12F);
        builder.define(DATA_COLOR, 0xFF66E8FF);
        builder.define(DATA_DAMAGED, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.entityData.set(DATA_AGE, this.age() + 1);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);

        Entity target = this.level().getEntity(this.targetId());
        if (target != null) {
            this.setPos(target.getX(), target.getY(), target.getZ());
        }

        if (!this.level().isClientSide()) {
            if (!(target instanceof LivingEntity living) || !living.isAlive()) {
                this.discard();
                return;
            }
            if (!this.damaged() && this.age() >= this.growTicks()) {
                this.entityData.set(DATA_DAMAGED, true);
                Entity owner = this.level().getEntity(this.ownerId());
                if (owner instanceof ServerPlayer player) {
                    AstralCardEffects.damage(player, living, this.damage());
                }
            }
            if (this.age() > this.totalLifetime()) {
                this.discard();
            }
        }
    }

    public int ownerId() { return this.entityData.get(DATA_OWNER); }
    public int targetId() { return this.entityData.get(DATA_TARGET); }
    public int damage() { return this.entityData.get(DATA_DAMAGE); }
    public int age() { return this.entityData.get(DATA_AGE); }
    public int growTicks() { return this.entityData.get(DATA_GROW); }
    public int holdTicks() { return this.entityData.get(DATA_HOLD); }
    public int fadeTicks() { return this.entityData.get(DATA_FADE); }
    public float beamHeight() { return this.entityData.get(DATA_HEIGHT); }
    public float radius() { return this.entityData.get(DATA_RADIUS); }
    public int color() { return this.entityData.get(DATA_COLOR); }
    public boolean damaged() { return this.entityData.get(DATA_DAMAGED); }
    public int totalLifetime() { return this.growTicks() + this.holdTicks() + this.fadeTicks(); }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_OWNER, input.getIntOr("owner", -1));
        this.entityData.set(DATA_TARGET, input.getIntOr("target", -1));
        this.entityData.set(DATA_DAMAGE, input.getIntOr("damage", 0));
        this.entityData.set(DATA_AGE, input.getIntOr("age", 0));
        this.entityData.set(DATA_GROW, input.getIntOr("grow", DEFAULT_GROW_TICKS));
        this.entityData.set(DATA_HOLD, input.getIntOr("hold", DEFAULT_HOLD_TICKS));
        this.entityData.set(DATA_FADE, input.getIntOr("fade", DEFAULT_FADE_TICKS));
        this.entityData.set(DATA_HEIGHT, input.getFloatOr("height", DEFAULT_HEIGHT));
        this.entityData.set(DATA_RADIUS, input.getFloatOr("radius", 0.12F));
        this.entityData.set(DATA_COLOR, input.getIntOr("color", 0xFF66E8FF));
        this.entityData.set(DATA_DAMAGED, input.getBooleanOr("damaged", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("owner", this.ownerId());
        output.putInt("target", this.targetId());
        output.putInt("damage", this.damage());
        output.putInt("age", this.age());
        output.putInt("grow", this.growTicks());
        output.putInt("hold", this.holdTicks());
        output.putInt("fade", this.fadeTicks());
        output.putFloat("height", this.beamHeight());
        output.putFloat("radius", this.radius());
        output.putInt("color", this.color());
        output.putBoolean("damaged", this.damaged());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

}