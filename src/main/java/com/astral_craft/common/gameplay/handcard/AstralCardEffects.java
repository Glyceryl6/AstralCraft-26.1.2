package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.projectile.CardProjectileSettings;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.DamagePresentation;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.KnockdownManager;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Shared helpers used by individual card item classes. No card item switch lives here. */
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

    public static void update(LivingEntity entity, AstralPlayerStats stats) {
        AstralStats.set(entity, stats);
    }

    public static void heal(ServerPlayer player, int amount) {
        update(player, AstralStats.get(player).heal(amount));
    }

    public static void damage(ServerPlayer user, List<LivingEntity> targets, int amount) {
        target(targets).ifPresent(target -> damage(user, target, amount));
    }

    /** Targeted effect damage: player targets receive a short counter-card response window. */
    public static void damage(ServerPlayer user, LivingEntity target, int amount) {
        PendingCounterEffectManager.offerDirectDamage(user, target, amount);
    }

    /** Final damage after counter resolution / visual impact. Do not call this at card selection time. */
    public static void damageNow(ServerPlayer user, LivingEntity target, int amount) {
        damageNow((LivingEntity) user, target, amount);
    }

    /** Board pawns may be the actual visual and logical source even when a player controls them. */
    public static void damageNow(LivingEntity source, LivingEntity target, int amount) {
        if (source == null || target == null || !(source.level() instanceof ServerLevel level)) return;
        AstralPlayerStats targetStats = AstralStats.getOrDefault(target);
        int finalDamage = Math.max(0, amount + targetStats.incomingDamageBonus()
                + Math.min(1, targetStats.buff(BuffKinds.MARK)));
        if (finalDamage == 0) return;
        if (finalDamage >= DamagePresentation.CRITICAL_DAMAGE_THRESHOLD) {
            DamagePresentation.playCriticalImpact(level, target);
        }
        if (target instanceof AstralCharacterEntity character && character.isBoardPawn()) {
            character.applyBoardDamage(finalDamage);
            return;
        }
        if (target instanceof ServerPlayer player) {
            AstralPlayerStats next = AstralStats.get(player).damage(finalDamage);
            update(player, next);
            KnockdownManager.checkKnockdown(player, next);
        }

        if (target.isAlive()) {
            DamageSource damageSource = source instanceof ServerPlayer player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(source);
            target.hurtServer(level, damageSource, finalDamage);
        }
    }

    public static void areaDamage(ServerPlayer user, int range, int amount, boolean playersOnly) {
        List<LivingEntity> list = user.level().getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(range), entity -> entity != user && entity.isAlive() && (!playersOnly || entity instanceof Player));
        for (LivingEntity entity : list) {
            damage(user, entity, amount);
        }
    }

    public static void areaDamageAt(ServerPlayer user, LivingEntity center, int range, int amount, boolean playersOnly) {
        List<LivingEntity> list = user.level().getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(range), entity -> entity.isAlive() && (!playersOnly || entity instanceof Player));
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
        PendingCounterEffectManager.offerLaser(user, target, amount, argb, radius);
        return true;
    }

    public static boolean firecrackerProjectile(ServerPlayer user, LivingEntity target, int amount) {
        return firecrackerProjectile(user, target, amount, CardProjectileSettings.firecrackers());
    }

    public static boolean firecrackerProjectile(ServerPlayer user, LivingEntity target, int amount, CardProjectileSettings settings) {
        if (target == null || !target.isAlive()) return false;
        PendingCounterEffectManager.offerFirecracker(user, target, amount, settings);
        return true;
    }

    public static boolean slingshotProjectile(ServerPlayer user, LivingEntity target, int amount) {
        return slingshotProjectile(user, target, amount, CardProjectileSettings.slingshot());
    }

    public static boolean slingshotProjectile(ServerPlayer user, LivingEntity target, int amount, CardProjectileSettings settings) {
        if (target == null || !target.isAlive()) return false;
        PendingCounterEffectManager.offerSlingshot(user, target, amount, settings);
        return true;
    }

    public static boolean snowballAttackProjectile(ServerPlayer user, LivingEntity target, int amount) {
        return snowballAttackProjectile(user, target, amount, CardProjectileSettings.snowballAttack());
    }

    public static boolean snowballAttackProjectile(ServerPlayer user, LivingEntity target, int amount, CardProjectileSettings settings) {
        if (target == null || !target.isAlive()) return false;
        PendingCounterEffectManager.offerSnowballAttack(user, target, amount, settings);
        return true;
    }

    public static boolean fallingBrick(ServerPlayer user, LivingEntity target, int amount) {
        if (target == null || !target.isAlive()) return false;
        PendingCounterEffectManager.offerFallingBrick(user, target, amount);
        return true;
    }

    public static void snatchCoins(ServerPlayer user, LivingEntity target, int amount) {
        AstralPlayerStats victim = AstralStats.getOrDefault(target);
        int taken = Math.min(amount, victim.starCoins());
        update(target, victim.spendCoins(taken));
        update(user, AstralStats.get(user).addCoins(taken));
    }

}