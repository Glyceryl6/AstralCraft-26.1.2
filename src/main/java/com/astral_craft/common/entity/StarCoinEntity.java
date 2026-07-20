package com.astral_craft.common.entity;

import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** Lightweight board coin entity with only gravity, collision and board-owned visual movement. */
public class StarCoinEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_BOARD_ID = SynchedEntityData.defineId(StarCoinEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OBJECT_ID = SynchedEntityData.defineId(StarCoinEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(StarCoinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AMOUNT = SynchedEntityData.defineId(StarCoinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(StarCoinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(StarCoinEntity.class, EntityDataSerializers.INT);
    private double startX;
    private double startY;
    private double startZ;
    private boolean capturedStart;

    public StarCoinEntity(EntityType<? extends StarCoinEntity> type, Level level) {
        super(type, level);
    }

    public StarCoinEntity(Level level) {
        this(AstralEntities.STAR_COIN.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BOARD_ID, "");
        builder.define(DATA_OBJECT_ID, "");
        builder.define(DATA_KIND, Kind.PILE.ordinal());
        builder.define(DATA_AMOUNT, 1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LIFETIME, 0);
    }

    public void configurePile(UUID boardId, UUID objectId, int amount) {
        this.configure(boardId, objectId, Kind.PILE, amount, -1, 0);
        this.setNoGravity(false);
        this.noPhysics = false;
        this.setGlowingTag(true);
    }

    public void configurePickup(UUID boardId, UUID objectId, int targetEntityId, int amount, int lifetime) {
        this.configure(boardId, objectId, Kind.PICKUP, amount, targetEntityId, lifetime);
        this.captureStart();
    }

    public void configureAward(UUID boardId, UUID objectId, int targetEntityId, int amount, int lifetime) {
        this.configure(boardId, objectId, Kind.AWARD, amount, targetEntityId, lifetime);
    }

    private void configure(UUID boardId, UUID objectId, Kind kind, int amount, int targetEntityId, int lifetime) {
        this.entityData.set(DATA_BOARD_ID, boardId.toString());
        this.entityData.set(DATA_OBJECT_ID, objectId.toString());
        this.entityData.set(DATA_KIND, kind.ordinal());
        this.entityData.set(DATA_AMOUNT, Math.max(1, amount));
        this.entityData.set(DATA_TARGET, targetEntityId);
        this.entityData.set(DATA_LIFETIME, Math.max(0, lifetime));
        if (kind != Kind.PILE) {
            this.setNoGravity(true);
            this.noPhysics = true;
            this.setGlowingTag(false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel level
                && this.boardId().filter(id -> BoardSessionManager.session(level, id).isPresent()).isEmpty()) {
            this.discard();
            return;
        }
        if (this.transientVisual()) {
            this.tickVisualMovement();
        } else {
            this.tickPilePhysics();
        }
    }

    private void tickPilePhysics() {
        Vec3 motion = this.getDeltaMovement();
        if (!this.isNoGravity()) motion = motion.add(0.0D, -0.04D, 0.0D);
        this.move(MoverType.SELF, motion);
        if (this.onGround()) {
            double bounce = motion.y < -0.08D ? -motion.y * 0.28D : 0.0D;
            motion = new Vec3(motion.x * 0.72D, bounce, motion.z * 0.72D);
            if (motion.horizontalDistanceSqr() < 1.0E-5D && bounce == 0.0D) motion = Vec3.ZERO;
        } else {
            motion = motion.scale(0.98D);
        }
        this.setDeltaMovement(motion);
    }

    private void tickVisualMovement() {
        this.setDeltaMovement(Vec3.ZERO);
        Entity target = this.level().getEntity(this.targetEntityId());
        if (target == null) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }
        int lifetime = Math.max(1, this.lifetime());
        float progress = Mth.clamp(this.tickCount / (float) lifetime, 0.0F, 1.0F);
        if (this.kind() == Kind.PICKUP) {
            if (!this.capturedStart) this.captureStart();
            double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
            this.setPos(Mth.lerp(eased, this.startX, target.getX()),
                    Mth.lerp(eased, this.startY, target.getY() + target.getBbHeight() * 0.55D),
                    Mth.lerp(eased, this.startZ, target.getZ()));
        } else {
            double top = target.getY() + target.getBbHeight() + 1.25D;
            double bottom = target.getY() + target.getBbHeight() + 0.18D;
            this.setPos(target.getX(), Mth.lerp(progress, top, bottom), target.getZ());
        }
        if (!this.level().isClientSide() && this.tickCount >= lifetime) this.discard();
    }

    private void captureStart() {
        this.startX = this.getX();
        this.startY = this.getY();
        this.startZ = this.getZ();
        this.capturedStart = true;
    }

    public Optional<UUID> boardId() {
        return parseUuid(this.entityData.get(DATA_BOARD_ID));
    }

    public Optional<UUID> objectId() {
        return parseUuid(this.entityData.get(DATA_OBJECT_ID));
    }

    public Kind kind() {
        int ordinal = this.entityData.get(DATA_KIND);
        return ordinal >= 0 && ordinal < Kind.values().length ? Kind.values()[ordinal] : Kind.PILE;
    }

    public int amount() {
        return this.entityData.get(DATA_AMOUNT);
    }

    public int targetEntityId() {
        return this.entityData.get(DATA_TARGET);
    }

    public int lifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public boolean transientVisual() {
        return this.kind() != Kind.PILE;
    }

    public float visualAge(float partialTick) {
        return this.tickCount + partialTick;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_BOARD_ID, input.getStringOr("board_id", ""));
        this.entityData.set(DATA_OBJECT_ID, input.getStringOr("object_id", ""));
        this.entityData.set(DATA_KIND, input.getIntOr("kind", Kind.PILE.ordinal()));
        this.entityData.set(DATA_AMOUNT, Math.max(1, input.getIntOr("amount", 1)));
        this.entityData.set(DATA_TARGET, input.getIntOr("target", -1));
        this.entityData.set(DATA_LIFETIME, Math.max(0, input.getIntOr("lifetime", 0)));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("board_id", this.entityData.get(DATA_BOARD_ID));
        output.putString("object_id", this.entityData.get(DATA_OBJECT_ID));
        output.putInt("kind", this.entityData.get(DATA_KIND));
        output.putInt("amount", this.amount());
        output.putInt("target", this.targetEntityId());
        output.putInt("lifetime", this.lifetime());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public enum Kind { PILE, PICKUP, AWARD }
}
