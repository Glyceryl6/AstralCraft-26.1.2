package com.astral_craft.common.entity;

import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

/** Disposable visual proxy for persistent board traps, coin piles and carried bombs. */
public class BoardWorldObjectEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_BOARD_ID = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OBJECT_ID = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BLOCK_ID = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_INDEX = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AMOUNT = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(BoardWorldObjectEntity.class, EntityDataSerializers.INT);

    private double startX;
    private double startY;
    private double startZ;

    public BoardWorldObjectEntity(EntityType<? extends BoardWorldObjectEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public BoardWorldObjectEntity(Level level) {
        this(AstralEntities.BOARD_WORLD_OBJECT.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BOARD_ID, "");
        builder.define(DATA_OBJECT_ID, "");
        builder.define(DATA_KIND, Kind.ENTRAPMENT.ordinal());
        builder.define(DATA_BLOCK_ID, BuiltInRegistries.BLOCK.getKey(Blocks.LODESTONE).toString());
        builder.define(DATA_INDEX, 0);
        builder.define(DATA_COUNT, 1);
        builder.define(DATA_AMOUNT, 1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LIFETIME, 0);
    }

    public void configure(UUID boardId, UUID objectId, Kind kind, Block block, int index, int count, int amount) {
        this.entityData.set(DATA_BOARD_ID, boardId.toString());
        this.entityData.set(DATA_OBJECT_ID, objectId.toString());
        this.entityData.set(DATA_KIND, kind.ordinal());
        this.entityData.set(DATA_BLOCK_ID, BuiltInRegistries.BLOCK.getKey(block == null ? kind.defaultBlock() : block).toString());
        this.entityData.set(DATA_INDEX, Math.max(0, index));
        this.entityData.set(DATA_COUNT, Math.max(1, count));
        this.entityData.set(DATA_AMOUNT, Math.max(1, amount));
        this.entityData.set(DATA_TARGET, -1);
        this.entityData.set(DATA_LIFETIME, 0);
        this.setGlowingTag(kind != Kind.COIN_PICKUP && kind != Kind.COIN_AWARD);
    }

    public void configurePickup(UUID boardId, UUID objectId, int targetEntityId, int amount, int lifetime) {
        this.configure(boardId, objectId, Kind.COIN_PICKUP, Blocks.AIR, 0, 1, amount);
        this.entityData.set(DATA_TARGET, targetEntityId);
        this.entityData.set(DATA_LIFETIME, Math.max(8, lifetime));
        this.startX = this.getX();
        this.startY = this.getY();
        this.startZ = this.getZ();
    }

    public void configureAward(UUID boardId, UUID objectId, int targetEntityId, int amount, int lifetime) {
        this.configure(boardId, objectId, Kind.COIN_AWARD, Blocks.AIR, 0, 1, amount);
        this.entityData.set(DATA_TARGET, targetEntityId);
        this.entityData.set(DATA_LIFETIME, Math.max(8, lifetime));
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (!this.level().isClientSide() && this.boardId().isEmpty()) {
            this.discard();
            return;
        }

        if (!this.transientVisual()) return;
        Entity target = this.level().getEntity(this.targetEntityId());
        if (target == null) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }

        int lifetime = this.lifetime();
        float progress = Mth.clamp(this.tickCount / (float) lifetime, 0.0F, 1.0F);
        if (this.kind() == Kind.COIN_PICKUP) {
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

    public Optional<UUID> boardId() {
        return parseUuid(this.entityData.get(DATA_BOARD_ID));
    }

    public Optional<UUID> objectId() {
        return parseUuid(this.entityData.get(DATA_OBJECT_ID));
    }

    public Kind kind() {
        int ordinal = this.entityData.get(DATA_KIND);
        return ordinal >= 0 && ordinal < Kind.values().length ? Kind.values()[ordinal] : Kind.ENTRAPMENT;
    }

    public Block block() {
        String raw = this.entityData.get(DATA_BLOCK_ID);
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(raw));
            return block == null ? this.kind().defaultBlock() : block;
        } catch (RuntimeException ignored) {
            return this.kind().defaultBlock();
        }
    }

    public BlockState blockState() {
        return this.block().defaultBlockState();
    }

    public int stackIndex() {
        return this.entityData.get(DATA_INDEX);
    }

    public int stackCount() {
        return this.entityData.get(DATA_COUNT);
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
        return this.kind() == Kind.COIN_PICKUP || this.kind() == Kind.COIN_AWARD;
    }

    public float visualAge(float partialTick) {
        return this.tickCount + partialTick;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public enum Kind {
        ENTRAPMENT,
        DEMOLITION,
        BARRICADE,
        ENHANCED_BARRICADE,
        TIME_BOMB,
        COIN_PILE,
        COIN_PICKUP,
        COIN_AWARD;

        public boolean coin() {
            return this == COIN_PILE || this == COIN_PICKUP || this == COIN_AWARD;
        }

        public Block defaultBlock() {
            return switch (this) {
                case ENTRAPMENT -> Blocks.LODESTONE;
                case DEMOLITION, TIME_BOMB -> Blocks.TNT;
                case BARRICADE -> Blocks.YELLOW_CONCRETE;
                case ENHANCED_BARRICADE -> Blocks.ORANGE_CONCRETE;
                case COIN_PILE, COIN_PICKUP, COIN_AWARD -> Blocks.AIR;
            };
        }
    }

}