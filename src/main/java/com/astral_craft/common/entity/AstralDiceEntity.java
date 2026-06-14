package com.astral_craft.common.entity;

import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Controller entity for the Astral Party style 3D dice.
 *
 * <p>The entity does not rely on baked dice-face textures. The renderer draws a cube and submits text
 * on each face. During rolling every face displays "?"; once settled all faces display the rolled
 * value, and the renderer eases the cube back to an upright, camera-facing orientation.</p>
 */
public class AstralDiceEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_MIN = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESULT = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROLL_TICKS = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);

    public AstralDiceEntity(EntityType<? extends AstralDiceEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public AstralDiceEntity(Level level, double x, double y, double z) {
        this(AstralEntities.ASTRAL_DICE.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MIN, 1);
        builder.define(DATA_MAX, 6);
        builder.define(DATA_RESULT, 0);
        builder.define(DATA_ROLL_TICKS, 20);
        builder.define(DATA_AGE, 0);
    }

    public void startRoll(int min, int max, int rollTicks, RandomSource random) {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        this.entityData.set(DATA_MIN, safeMin);
        this.entityData.set(DATA_MAX, safeMax);
        this.entityData.set(DATA_ROLL_TICKS, Math.max(1, rollTicks));
        this.entityData.set(DATA_AGE, 0);
        this.entityData.set(DATA_RESULT, Mth.nextInt(random, safeMin, safeMax));
    }

    @Override
    public void tick() {
        super.tick();
        int age = this.entityData.get(DATA_AGE) + 1;
        this.entityData.set(DATA_AGE, age);
        this.setDeltaMovement(0, 0, 0);
        if (!this.level().isClientSide() && age > this.rollTicks() + 80) {
            this.discard();
        }
    }

    public int rollAge() {
        return this.entityData.get(DATA_AGE);
    }

    public int rollTicks() {
        return this.entityData.get(DATA_ROLL_TICKS);
    }

    public int result() {
        return this.entityData.get(DATA_RESULT);
    }

    public boolean isRolling() {
        return this.rollAge() < this.rollTicks();
    }

    public String faceText() {
        return this.isRolling() ? "?" : Integer.toString(this.result());
    }

    public float xSpin(float partialTick) {
        float t = this.rollAge() + partialTick;
        float settle = this.settleProgress(partialTick);
        return Mth.lerp(settle, t * 17.0F, finalX());
    }

    public float ySpin(float partialTick) {
        float t = this.rollAge() + partialTick;
        float settle = this.settleProgress(partialTick);
        return Mth.lerp(settle, t * 23.0F, finalY());
    }

    public float zSpin(float partialTick) {
        float t = this.rollAge() + partialTick;
        float settle = this.settleProgress(partialTick);
        return Mth.lerp(settle, t * 13.0F, finalZ());
    }

    private float settleProgress(float partialTick) {
        float t = (this.rollAge() + partialTick - this.rollTicks()) / 20.0F;
        return Mth.clamp(t, 0.0F, 1.0F);
    }

    private float finalX() {
        return 0.0F;
    }

    private float finalY() {
        return 0.0F;
    }

    private float finalZ() {
        return 0.0F;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_MIN, input.getIntOr("min", 1));
        this.entityData.set(DATA_MAX, input.getIntOr("max", 6));
        this.entityData.set(DATA_RESULT, input.getIntOr("result", 0));
        this.entityData.set(DATA_ROLL_TICKS, input.getIntOr("roll_ticks", 20));
        this.entityData.set(DATA_AGE, input.getIntOr("age", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("min", this.entityData.get(DATA_MIN));
        output.putInt("max", this.entityData.get(DATA_MAX));
        output.putInt("result", this.result());
        output.putInt("roll_ticks", this.rollTicks());
        output.putInt("age", this.rollAge());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        this.discard();
        return true;
    }

}