package com.astral_craft.common.gameplay.event;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public record AstralEventContext(
        ServerPlayer triggerPlayer,
        Entity target,
        ServerLevel level,
        AstralEventDefinition definition,
        BlockPos origin,
        BlockPos blockPos,
        BlockState blockState,
        DamageSource damageSource,
        float damageAmount,
        boolean deathContext) {

    public static AstralEventContext player(ServerPlayer player, AstralEventDefinition definition) {
        return new AstralEventContext(player, player, player.level(), definition, player.blockPosition(), null, null, null, 0.0F, false);
    }

    public static AstralEventContext of(ServerPlayer triggerPlayer, Entity target, AstralEventDefinition definition) {
        Entity safeTarget = target == null ? triggerPlayer : target;
        ServerLevel level = safeTarget.level() instanceof ServerLevel serverLevel ? serverLevel : triggerPlayer.level();
        return new AstralEventContext(triggerPlayer, safeTarget, level, definition, safeTarget.blockPosition(), null, null, null, 0.0F, false);
    }

    public static AstralEventContext blockBreak(ServerPlayer player, BlockPos blockPos, BlockState blockState) {
        BlockPos origin = blockPos == null ? player.blockPosition() : blockPos;
        return new AstralEventContext(player, player, player.level(), null, origin, blockPos, blockState, null, 0.0F, false);
    }

    public static AstralEventContext damage(ServerPlayer triggerPlayer, Entity target, DamageSource damageSource, float damageAmount) {
        Entity safeTarget = target == null ? triggerPlayer : target;
        ServerLevel level = safeTarget.level() instanceof ServerLevel serverLevel ? serverLevel : triggerPlayer.level();
        return new AstralEventContext(triggerPlayer, safeTarget, level, null, safeTarget.blockPosition(), null, null, damageSource, damageAmount, false);
    }

    public static AstralEventContext death(ServerPlayer triggerPlayer, Entity target, DamageSource damageSource) {
        Entity safeTarget = target == null ? triggerPlayer : target;
        ServerLevel level = safeTarget.level() instanceof ServerLevel serverLevel ? serverLevel : triggerPlayer.level();
        return new AstralEventContext(triggerPlayer, safeTarget, level, null, safeTarget.blockPosition(), null, null, damageSource, 0.0F, true);
    }

    public AstralEventContext withDefinition(AstralEventDefinition definition) {
        return new AstralEventContext(this.triggerPlayer, this.target, this.level, definition, this.origin, this.blockPos, this.blockState, this.damageSource, this.damageAmount, this.deathContext);
    }

    public ServerPlayer targetPlayer() {
        return this.target instanceof ServerPlayer player ? player : null;
    }

    public LivingEntity targetLiving() {
        return this.target instanceof LivingEntity living ? living : null;
    }

    public boolean hasBlockBreak() {
        return this.blockPos != null && this.blockState != null;
    }

    public boolean hasDamage() {
        return this.damageSource != null && !this.deathContext;
    }

    public boolean hasDeath() {
        return this.damageSource != null && this.deathContext;
    }

    public Entity damageSourceEntity() {
        return this.damageSource == null ? null : this.damageSource.getEntity();
    }


    public boolean isPlayerHurt() {
        return this.hasDamage() && this.targetPlayer() != null;
    }

    public boolean isEntityHurtPlayer() {
        return this.isPlayerHurt() && this.damageSource.getEntity() instanceof LivingEntity;
    }

    public boolean isPlayerHurtEntity() {
        return this.hasDamage() && this.triggerPlayer != null && this.target != null && !this.triggerPlayer.getUUID().equals(this.target.getUUID());
    }

    public boolean isPlayerKilled() {
        return this.hasDeath() && this.targetPlayer() != null;
    }

    public boolean isPlayerKilledEntity() {
        return this.hasDeath() && this.triggerPlayer != null && this.target != null && !this.triggerPlayer.getUUID().equals(this.target.getUUID());
    }

    public RandomSource random() {
        if (this.triggerPlayer != null) {
            return this.triggerPlayer.getRandom();
        }
        return RandomSource.create();
    }

    public CommandSourceStack commandSource() {
        ServerPlayer sourcePlayer = this.targetPlayer() != null ? this.targetPlayer() : this.triggerPlayer;
        return sourcePlayer.createCommandSourceStack().withSuppressedOutput().withPermission(LevelBasedPermissionSet.GAMEMASTER);
    }

    public String triggerPlayerName() {
        return this.triggerPlayer == null ? "" : this.triggerPlayer.getGameProfile().name();
    }

    public String targetSelector() {
        if (this.target instanceof ServerPlayer player) {
            return player.getGameProfile().name();
        }

        if (this.target != null) {
            return this.target.getStringUUID();
        }

        return this.triggerPlayerName();
    }

}
