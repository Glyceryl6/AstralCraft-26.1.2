package com.astral_craft.common.entity;

import com.astral_craft.common.gameplay.SoulLinkStyle;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SoulLinkEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_FIRST = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SECOND = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ARC_HEIGHT = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_THICKNESS = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RAINBOW = SynchedEntityData.defineId(SoulLinkEntity.class, EntityDataSerializers.BOOLEAN);

    public SoulLinkEntity(EntityType<? extends SoulLinkEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public SoulLinkEntity(Level level, LivingEntity first, LivingEntity second, int lifetimeTicks, SoulLinkStyle style) {
        this(AstralEntities.SOUL_LINK.get(), level);
        this.setPos((first.getX() + second.getX()) * 0.5D,
                (first.getY() + second.getY()) * 0.5D,
                (first.getZ() + second.getZ()) * 0.5D);
        this.setEndpoints(first.getId(), second.getId());
        this.setVisualStyle(style);
        this.entityData.set(DATA_LIFETIME, Math.max(1, lifetimeTicks));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FIRST, -1);
        builder.define(DATA_SECOND, -1);
        builder.define(DATA_AGE, 0);
        builder.define(DATA_LIFETIME, 20 * 60);
        builder.define(DATA_ARC_HEIGHT, SoulLinkStyle.DEFAULT.arcHeight());
        builder.define(DATA_THICKNESS, SoulLinkStyle.DEFAULT.thickness());
        builder.define(DATA_COLOR, SoulLinkStyle.DEFAULT.color());
        builder.define(DATA_RAINBOW, SoulLinkStyle.DEFAULT.rainbow());
    }

    public void setEndpoints(int firstId, int secondId) {
        this.entityData.set(DATA_FIRST, firstId);
        this.entityData.set(DATA_SECOND, secondId);
    }

    public void setVisualStyle(SoulLinkStyle style) {
        this.entityData.set(DATA_ARC_HEIGHT, style.arcHeight());
        this.entityData.set(DATA_THICKNESS, style.thickness());
        this.entityData.set(DATA_COLOR, style.color());
        this.entityData.set(DATA_RAINBOW, style.rainbow());
    }

    public int firstId() {
        return this.entityData.get(DATA_FIRST);
    }

    public int secondId() {
        return this.entityData.get(DATA_SECOND);
    }

    public int linkAge() {
        return this.entityData.get(DATA_AGE);
    }

    public float arcHeight() {
        return this.entityData.get(DATA_ARC_HEIGHT);
    }

    public float thickness() {
        return this.entityData.get(DATA_THICKNESS);
    }

    public int color() {
        return this.entityData.get(DATA_COLOR);
    }

    public boolean rainbow() {
        return this.entityData.get(DATA_RAINBOW);
    }

    @Override
    public void tick() {
        super.tick();
        this.entityData.set(DATA_AGE, this.linkAge() + 1);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (!this.level().isClientSide()) {
            Entity first = this.level().getEntity(this.firstId());
            Entity second = this.level().getEntity(this.secondId());
            if (!(first instanceof LivingEntity livingFirst) || !(second instanceof LivingEntity livingSecond) || !livingFirst.isAlive() || !livingSecond.isAlive() || this.linkAge() > this.entityData.get(DATA_LIFETIME)) {
                this.discard();
                return;
            }
            this.setPos((first.getX() + second.getX()) * 0.5D, (first.getY() + second.getY()) * 0.5D, (first.getZ() + second.getZ()) * 0.5D);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_FIRST, input.getIntOr("first", -1));
        this.entityData.set(DATA_SECOND, input.getIntOr("second", -1));
        this.entityData.set(DATA_AGE, input.getIntOr("age", 0));
        this.entityData.set(DATA_LIFETIME, input.getIntOr("lifetime", 20 * 60));
        this.entityData.set(DATA_ARC_HEIGHT, input.getFloatOr("arc_height", SoulLinkStyle.DEFAULT.arcHeight()));
        this.entityData.set(DATA_THICKNESS, input.getFloatOr("thickness", SoulLinkStyle.DEFAULT.thickness()));
        this.entityData.set(DATA_COLOR, input.getIntOr("color", SoulLinkStyle.DEFAULT.color()));
        this.entityData.set(DATA_RAINBOW, input.getBooleanOr("rainbow", SoulLinkStyle.DEFAULT.rainbow()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("first", this.firstId());
        output.putInt("second", this.secondId());
        output.putInt("age", this.linkAge());
        output.putInt("lifetime", this.entityData.get(DATA_LIFETIME));
        output.putFloat("arc_height", this.arcHeight());
        output.putFloat("thickness", this.thickness());
        output.putInt("color", this.color());
        output.putBoolean("rainbow", this.rainbow());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

}