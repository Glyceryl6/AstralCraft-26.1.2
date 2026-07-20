package com.astral_craft.common.entity;

import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Item-physics based visual for logical board star coins. Logical pickup remains board-controlled. */
public class StarCoinEntity extends ItemEntity {

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
        this.setItem(new ItemStack(AstralItems.STAR_COIN.get()));
        this.setNeverPickUp();
        this.setUnlimitedLifetime();
        this.setInvulnerable(true);
    }

    public StarCoinEntity(Level level) {
        this(AstralEntities.STAR_COIN.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
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
        this.startX = this.getX();
        this.startY = this.getY();
        this.startZ = this.getZ();
        this.capturedStart = true;
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
        this.setTarget(objectId);
        ItemStack stack = new ItemStack(AstralItems.STAR_COIN.get(), Math.clamp(amount, 1, 99));
        this.setItem(stack);
        this.setNeverPickUp();
        this.setUnlimitedLifetime();
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

        if (!this.transientVisual()) return;
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        Entity target = this.level().getEntity(this.targetEntityId());
        if (target == null) {
            if (!this.level().isClientSide()) this.discard();
            return;
        }

        int lifetime = Math.max(1, this.lifetime());
        float progress = Mth.clamp(this.tickCount / (float) lifetime, 0.0F, 1.0F);
        if (this.kind() == Kind.PICKUP) {
            if (!this.capturedStart) {
                this.startX = this.getX();
                this.startY = this.getY();
                this.startZ = this.getZ();
                this.capturedStart = true;
            }

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

    @Override
    public void playerTouch(Player player) {}

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

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public enum Kind { PILE, PICKUP, AWARD }

}