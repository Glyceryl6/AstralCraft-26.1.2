package com.astral_craft.common.gameplay;

import com.astral_craft.common.entity.visual.ArcProjectileEntity;
import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Shared helpers used by individual card item classes. No card id switch lives here. */
public class AstralCardEffects {

    public static void applyChip(Player player, ChipDefinition chip) {
        AstralStats.set(player, AstralStats.get(player).applyChip(chip.stats()));
    }

    public static Optional<LivingEntity> target(List<LivingEntity> targets) {
        return targets.isEmpty() ? Optional.empty() : Optional.of(targets.getFirst());
    }

    public static Optional<ServerPlayer> targetPlayer(List<LivingEntity> targets) {
        return target(targets).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast);
    }

    public static Optional<ServerPlayer> targetPlayerOrSelf(ServerPlayer user, List<LivingEntity> targets) {
        return targets.isEmpty() ? Optional.of(user) : targetPlayer(targets);
    }

    public static void update(Player player, AstralPlayerStats stats) {
        AstralStats.set(player, stats);
    }

    public static void heal(ServerPlayer player, int amount) {
        update(player, AstralStats.get(player).heal(amount));
    }

    public static void damage(ServerPlayer user, List<LivingEntity> targets, int amount) {
        target(targets).ifPresent(target -> damage(user, target, amount));
    }

    public static void damage(ServerPlayer user, LivingEntity target, int amount) {
        int finalDamage = amount + AstralStats.getOrDefault(target).incomingDamageBonus() +
                Math.min(1, AstralStats.getOrDefault(target).buff(BuffKinds.MARK));
        if (target instanceof ServerPlayer player) {
            update(player, AstralStats.get(player).damage(finalDamage));
        }

        if (target.isAlive()) {
            target.hurtServer(user.level(), user.damageSources().playerAttack(user), finalDamage);
        }
    }

    public static void areaDamage(ServerPlayer user, int range, int amount, boolean playersOnly) {
        List<LivingEntity> list = user.level().getEntitiesOfClass(
                LivingEntity.class, user.getBoundingBox().inflate(range),
                entity -> entity != user && entity.isAlive() && (!playersOnly || entity instanceof Player));
        for (LivingEntity entity : list) {
            damage(user, entity, amount);
        }
    }

    public static void areaDamageAt(ServerPlayer user, LivingEntity center, int range, int amount, boolean playersOnly) {
        List<LivingEntity> list = user.level().getEntitiesOfClass(
                LivingEntity.class, center.getBoundingBox().inflate(range),
                entity -> entity.isAlive() && (!playersOnly || entity instanceof Player));
        for (LivingEntity entity : list) {
            damage(user, entity, amount);
        }
    }

    public static void healPlayersAround(ServerLevel level, LivingEntity center, int range, int amount) {
        List<ServerPlayer> list = level.getEntitiesOfClass(ServerPlayer.class, center.getBoundingBox().inflate(range), Player::isAlive);
        for (ServerPlayer player : list) {
            update(player, AstralStats.get(player).heal(amount));
        }
    }

    public static void giveRandomCard(ServerPlayer player) {
        player.addItem(new ItemStack(AstralItems.HANDCARD_ATTACK_M.get()));
    }

    public static boolean laserStrike(ServerPlayer user, LivingEntity target, int amount, int argb, float radius) {
        if (target == null || !target.isAlive()) return false;
        LaserStrikeEntity entity = new LaserStrikeEntity(user.level(), user, target, amount, argb, radius);
        user.level().addFreshEntity(entity);
        return true;
    }

    public static boolean firecrackerProjectile(ServerPlayer user, LivingEntity target, int amount) {
        if (target == null || !target.isAlive()) return false;
        ArcProjectileEntity entity = new ArcProjectileEntity(
                user.level(), user, target, amount,
                ArcProjectileEntity.MODE_FIRECRACKER, 30);
        user.level().addFreshEntity(entity);
        return true;
    }

    public static boolean slingshotProjectile(ServerPlayer user, LivingEntity target, int amount) {
        if (target == null || !target.isAlive()) return false;
        ArcProjectileEntity entity = new ArcProjectileEntity(
                user.level(), user, target, amount,
                ArcProjectileEntity.MODE_SLINGSHOT, 18);
        user.level().addFreshEntity(entity);
        return true;
    }

    public static boolean fallingBrick(ServerPlayer user, LivingEntity target, int amount) {
        if (target == null || !target.isAlive()) return false;
        FallingBrickEntity entity = new FallingBrickEntity(user.level(), user, target, amount, 18);
        user.level().addFreshEntity(entity);
        return true;
    }

    public static void snatchCoins(ServerPlayer user, ServerPlayer target, int amount) {
        AstralPlayerStats victim = AstralStats.get(target);
        int taken = Math.min(amount, victim.starCoins());
        update(target, victim.spendCoins(taken));
        update(user, AstralStats.get(user).addCoins(taken));
    }

}