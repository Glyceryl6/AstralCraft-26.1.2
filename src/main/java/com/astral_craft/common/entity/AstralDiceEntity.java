package com.astral_craft.common.entity;

import com.astral_craft.common.registry.AstralEntities;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AstralDiceEntity extends Entity {

    public static final int RESULT_HOLD_TICKS = 12;

    private static final EntityDataAccessor<Integer> DATA_MIN = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESULT = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMBINED_RESULT = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROLL_TICKS = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MERGE_TICKS = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SPIN_SPEED = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MERGE_OFFSET_X = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MERGE_OFFSET_Z = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PRIMARY = SynchedEntityData.defineId(AstralDiceEntity.class, EntityDataSerializers.BOOLEAN);

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
        builder.define(DATA_RESULT, 1);
        builder.define(DATA_COMBINED_RESULT, 1);
        builder.define(DATA_ROLL_TICKS, 28);
        builder.define(DATA_MERGE_TICKS, 0);
        builder.define(DATA_SPIN_SPEED, 34.0F);
        builder.define(DATA_MERGE_OFFSET_X, 0.0F);
        builder.define(DATA_MERGE_OFFSET_Z, 0.0F);
        builder.define(DATA_PRIMARY, true);
    }

    public void startRoll(int min, int max, int rollTicks, RandomSource random) {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        this.startRoll(safeMin, safeMax, rollTicks, 34.0F,
                Mth.nextInt(random, safeMin, safeMax), 0, 0, true, 0.0F, 0.0F);
    }

    public void startRoll(int min, int max, int rollTicks, float spinSpeed, int result,
                          int combinedResult, int mergeTicks, boolean primary,
                          float mergeOffsetX, float mergeOffsetZ) {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        this.entityData.set(DATA_MIN, safeMin);
        this.entityData.set(DATA_MAX, safeMax);
        this.entityData.set(DATA_RESULT, Math.clamp(result, safeMin, safeMax));
        this.entityData.set(DATA_COMBINED_RESULT, Math.max(0, combinedResult));
        this.entityData.set(DATA_ROLL_TICKS, Math.max(1, rollTicks));
        this.entityData.set(DATA_MERGE_TICKS, Math.max(0, mergeTicks));
        this.entityData.set(DATA_SPIN_SPEED, Math.clamp(spinSpeed, 1.0F, 180.0F));
        this.entityData.set(DATA_MERGE_OFFSET_X, mergeOffsetX);
        this.entityData.set(DATA_MERGE_OFFSET_Z, mergeOffsetZ);
        this.entityData.set(DATA_PRIMARY, primary);
        this.tickCount = 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (this.level().isClientSide()) return;
        int mergeEnd = this.rollTicks() + this.mergeTicks();
        if (this.mergeTicks() > 0 && this.tickCount == mergeEnd) {
            if (this.isPrimary()) {
                this.setPos(this.getX() + this.mergeOffsetX(), this.getY(), this.getZ() + this.mergeOffsetZ());
                this.entityData.set(DATA_MERGE_OFFSET_X, 0.0F);
                this.entityData.set(DATA_MERGE_OFFSET_Z, 0.0F);
            } else {
                this.discard();
                return;
            }
        }

        if (this.tickCount > mergeEnd + RESULT_HOLD_TICKS) this.discard();
    }

    public int rollTicks() {
        return this.entityData.get(DATA_ROLL_TICKS);
    }

    public int mergeTicks() {
        return this.entityData.get(DATA_MERGE_TICKS);
    }

    public int result() {
        return this.entityData.get(DATA_RESULT);
    }

    public int combinedResult() {
        return this.entityData.get(DATA_COMBINED_RESULT);
    }

    public float spinSpeed() {
        return this.entityData.get(DATA_SPIN_SPEED);
    }

    public boolean isPrimary() {
        return this.entityData.get(DATA_PRIMARY);
    }

    public float mergeOffsetX() {
        return this.entityData.get(DATA_MERGE_OFFSET_X);
    }

    public float mergeOffsetZ() {
        return this.entityData.get(DATA_MERGE_OFFSET_Z);
    }

    public boolean isRolling(float ageTicks) {
        return ageTicks < this.rollTicks();
    }

    public String faceText(float ageTicks) {
        if (this.isRolling(ageTicks)) return "?";
        if (this.isPrimary() && this.mergeTicks() > 0
                && ageTicks >= this.rollTicks() + this.mergeTicks()) {
            return Integer.toString(this.combinedResult());
        }
        return Integer.toString(this.result());
    }

    public float xSpin(float ageTicks) {
        return this.spin(ageTicks, 1.0F);
    }

    public float ySpin(float ageTicks) {
        return this.spin(ageTicks, 0.82F);
    }

    public float zSpin(float ageTicks) {
        return this.spin(ageTicks, 0.63F);
    }

    public float mergeProgress(float ageTicks) {
        if (this.mergeTicks() <= 0) return 0.0F;
        float progress = (ageTicks - this.rollTicks()) / this.mergeTicks();
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        return progress * progress * (3.0F - 2.0F * progress);
    }

    public float renderScale(float ageTicks) {
        float progress = this.mergeProgress(ageTicks);
        if (this.mergeTicks() <= 0) return 1.0F;
        if (!this.isPrimary()) return 1.0F - progress;
        return 1.0F + Mth.sin(progress * (float) Math.PI) * 0.16F;
    }

    private float spin(float ageTicks, float axisFactor) {
        float progress = Mth.clamp(ageTicks / this.rollTicks(), 0.0F, 1.0F);
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        int rotations = Math.max(1, Math.round(this.spinSpeed() * axisFactor * this.rollTicks() / 360.0F));
        return 360.0F * rotations * eased;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_MIN, input.getIntOr("min", 1));
        this.entityData.set(DATA_MAX, input.getIntOr("max", 6));
        this.entityData.set(DATA_RESULT, input.getIntOr("result", 1));
        this.entityData.set(DATA_COMBINED_RESULT, input.getIntOr("combined_result", this.result()));
        this.entityData.set(DATA_ROLL_TICKS, input.getIntOr("roll_ticks", 28));
        this.entityData.set(DATA_MERGE_TICKS, input.getIntOr("merge_ticks", 0));
        this.entityData.set(DATA_SPIN_SPEED, input.getFloatOr("spin_speed", 34.0F));
        this.entityData.set(DATA_MERGE_OFFSET_X, input.getFloatOr("merge_offset_x", 0.0F));
        this.entityData.set(DATA_MERGE_OFFSET_Z, input.getFloatOr("merge_offset_z", 0.0F));
        this.entityData.set(DATA_PRIMARY, input.getBooleanOr("primary", true));
        this.tickCount = Math.max(0, input.getIntOr("age", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("min", this.entityData.get(DATA_MIN));
        output.putInt("max", this.entityData.get(DATA_MAX));
        output.putInt("result", this.result());
        output.putInt("combined_result", this.combinedResult());
        output.putInt("roll_ticks", this.rollTicks());
        output.putInt("merge_ticks", this.mergeTicks());
        output.putFloat("spin_speed", this.spinSpeed());
        output.putFloat("merge_offset_x", this.mergeOffsetX());
        output.putFloat("merge_offset_z", this.mergeOffsetZ());
        output.putBoolean("primary", this.isPrimary());
        output.putInt("age", this.tickCount);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        this.discard();
        return true;
    }

}