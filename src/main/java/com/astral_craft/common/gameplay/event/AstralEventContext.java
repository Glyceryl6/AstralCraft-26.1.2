package com.astral_craft.common.gameplay.event;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record AstralEventContext(ServerPlayer triggerPlayer, Entity target, ServerLevel level, AstralEventDefinition definition, String trigger, BlockPos origin) {

    public static AstralEventContext player(ServerPlayer player, AstralEventDefinition definition) {
        return new AstralEventContext(player, player, player.level(), definition, "manual", player.blockPosition());
    }

    public static AstralEventContext of(ServerPlayer triggerPlayer, Entity target, AstralEventDefinition definition, String trigger) {
        Entity safeTarget = target == null ? triggerPlayer : target;
        ServerLevel level = safeTarget.level() instanceof ServerLevel serverLevel ? serverLevel : triggerPlayer.level();
        BlockPos origin = safeTarget.blockPosition();
        return new AstralEventContext(triggerPlayer, safeTarget, level, definition, trigger == null ? "manual" : trigger, origin);
    }

    public ServerPlayer targetPlayer() {
        return this.target instanceof ServerPlayer player ? player : null;
    }

    public LivingEntity targetLiving() {
        return this.target instanceof LivingEntity living ? living : null;
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