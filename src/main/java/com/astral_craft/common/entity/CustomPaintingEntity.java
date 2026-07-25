package com.astral_craft.common.entity;

import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.gameplay.CustomPaintingPlacement;
import com.astral_craft.common.network.s2c.OpenCustomPaintingConfigPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class CustomPaintingEntity extends Painting {

    private static final EntityDataAccessor<String> DATA_RESOURCE = SynchedEntityData.defineId(CustomPaintingEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_WIDTH = SynchedEntityData.defineId(CustomPaintingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEIGHT = SynchedEntityData.defineId(CustomPaintingEntity.class, EntityDataSerializers.INT);

    public CustomPaintingEntity(EntityType<? extends CustomPaintingEntity> type, Level level) {
        super(type, level);
    }

    public CustomPaintingEntity(Level level, BlockPos supportPos, Direction direction, CustomPaintingData data) {
        this(AstralEntities.CUSTOM_PAINTING.get(), level);
        this.pos = supportPos.immutable();
        this.setData(data);
        this.setDirection(direction.getAxis().isHorizontal() ? direction : Direction.NORTH);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RESOURCE, "");
        builder.define(DATA_WIDTH, 1);
        builder.define(DATA_HEIGHT, 1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_RESOURCE.equals(accessor) || DATA_WIDTH.equals(accessor) || DATA_HEIGHT.equals(accessor)) this.recalculateBoundingBox();
    }

    public boolean applyConfiguration(CustomPaintingData data) {
        if (data == null || !data.configured()) return false;
        CustomPaintingData previous = this.data();
        this.setData(data);
        this.recalculateBoundingBox();
        if (this.survives()) return true;
        this.setData(previous);
        this.recalculateBoundingBox();
        return false;
    }

    public CustomPaintingData data() {
        return new CustomPaintingData(this.entityData.get(DATA_RESOURCE), this.entityData.get(DATA_WIDTH), this.entityData.get(DATA_HEIGHT));
    }

    public Direction facing() {
        return this.getDirection();
    }

    public BlockPos supportPos() {
        return this.getPos();
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        return CustomPaintingPlacement.boundingBox(pos, direction, this.data());
    }

    @Override
    public boolean survives() {
        return CustomPaintingPlacement.canPlace(this.level(), this.supportPos(), this.facing(), this.data(), this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenCustomPaintingConfigPayload(this.getId(), this.data()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void dropItem(ServerLevel level, Entity breaker) {
        level.playSound(null, this.blockPosition(), SoundEvents.PAINTING_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!(breaker instanceof Player player) || !player.getAbilities().instabuild) this.spawnAtLocation(level, this.createDropStack(), 0.0F);
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public ItemStack getPickResult() {
        return this.createDropStack();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_RESOURCE, input.getStringOr("resource", ""));
        this.entityData.set(DATA_WIDTH, Math.clamp(input.getIntOr("width", 1), 1, CustomPaintingData.MAX_SIZE));
        this.entityData.set(DATA_HEIGHT, Math.clamp(input.getIntOr("height", 1), 1, CustomPaintingData.MAX_SIZE));
        this.recalculateBoundingBox();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("resource", this.data().resource());
        output.putInt("width", this.data().width());
        output.putInt("height", this.data().height());
    }

    private void setData(CustomPaintingData data) {
        CustomPaintingData safeData = data == null ? CustomPaintingData.EMPTY : data;
        this.entityData.set(DATA_RESOURCE, safeData.resource());
        this.entityData.set(DATA_WIDTH, safeData.width());
        this.entityData.set(DATA_HEIGHT, safeData.height());
    }

    private ItemStack createDropStack() {
        ItemStack stack = new ItemStack(AstralItems.CUSTOM_PAINTING.get());
        stack.set(AstralDataComponents.CUSTOM_PAINTING.get(), this.data());
        return stack;
    }
}
