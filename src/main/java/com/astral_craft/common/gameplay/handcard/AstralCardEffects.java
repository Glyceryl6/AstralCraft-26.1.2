package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.projectile.CardProjectileSettings;
import com.astral_craft.common.entity.projectile.FirecrackersProjectileEntity;
import com.astral_craft.common.entity.projectile.SlingshotProjectileEntity;
import com.astral_craft.common.entity.projectile.SnowballAttackProjectileEntity;
import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import com.astral_craft.common.gameplay.DamagePresentation;
import com.astral_craft.common.gameplay.KnockdownManager;
import com.astral_craft.common.gameplay.board.BoardEntityService;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
        if (player == null || stats == null) return;
        AstralPlayerStats current = AstralStats.get(player);
        AstralStats.set(player, stats);
        if (stats.health() > current.health()) {
            playHealingEffect(player instanceof ServerPlayer serverPlayer
                    ? BoardEntityService.effectSourceEntity(serverPlayer) : player);
        }
    }

    public static void update(LivingEntity entity, AstralPlayerStats stats) {
        if (entity == null || stats == null) return;
        AstralPlayerStats current = AstralStats.getOrDefault(entity);
        AstralStats.set(entity, stats);
        if (stats.health() > current.health()) playHealingEffect(entity);
    }

    public static void heal(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return;
        update(player, AstralStats.get(player).heal(amount));
    }

    public static void playHealingEffect(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                18, entity.getBbWidth() * 0.45D, entity.getBbHeight() * 0.35D, entity.getBbWidth() * 0.45D, 0.08D);
        level.playSound(null, entity.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    public static void damage(ServerPlayer user, List<LivingEntity> targets, int amount) {
        target(targets).ifPresent(target -> damage(user, target, amount));
    }

    /** Targeted effect damage: player targets receive a short counter-card response window. */
    public static void damage(ServerPlayer user, LivingEntity target, int amount) {
        LivingEntity visualSource = BoardEntityService.effectSourceEntity(user);
        PendingCounterEffectManager.offer(user, target, resolvedTarget -> damageNow(effectSource(user, visualSource), resolvedTarget, amount));
    }

    /** Final damage after counter resolution / visual impact. Do not call this at card selection time. */
    public static void damageNow(ServerPlayer user, LivingEntity target, int amount) {
        damageNow((LivingEntity) user, target, amount);
    }

    /** Board pawns may be the actual visual and logical source even when a player controls them. */
    public static void damageNow(LivingEntity source, LivingEntity target, int amount) {
        if (source == null || target == null || !(source.level() instanceof ServerLevel level)) return;
        if (target instanceof AstralCharacterEntity character && character.isBoardPawn()) {
            int boardDamage = Math.max(0, amount);
            if (boardDamage == 0) return;
            character.applyBoardDamage(boardDamage);
            PendingCardActionManager.notifyBoardDamage(source);
            return;
        }

        AstralPlayerStats targetStats = AstralStats.getOrDefault(target);
        int finalDamage = targetStats.resolveIncomingDamage(Math.max(0, amount + targetStats.incomingDamageBonus()));
        if (finalDamage == 0) return;
        if (finalDamage >= DamagePresentation.CRITICAL_DAMAGE_THRESHOLD) {
            DamagePresentation.playCriticalImpact(level, target);
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
        List<LivingEntity> list = user.level().getEntitiesOfClass(
                LivingEntity.class, user.getBoundingBox().inflate(range),
                entity -> entity != user && entity.isAlive()
                        && (!playersOnly || entity instanceof Player));
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
        LivingEntity visualSource = BoardEntityService.effectSourceEntity(user);
        PendingCounterEffectManager.offer(user, target, resolvedTarget -> {
            LivingEntity source = effectSource(user, visualSource);
            playSourceAttack(source);
            source.level().playSound(null, resolvedTarget.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
            source.level().addFreshEntity(new LaserStrikeEntity(source.level(), source, resolvedTarget, amount, argb, radius));
        });

        return true;
    }

    public static boolean firecrackerProjectile(ServerPlayer user, LivingEntity target, int amount) {
        return firecrackerProjectile(user, target, amount, CardProjectileSettings.firecrackers());
    }

    public static boolean firecrackerProjectile(ServerPlayer user, LivingEntity target, int amount, CardProjectileSettings settings) {
        if (target == null || !target.isAlive()) return false;
        LivingEntity visualSource = BoardEntityService.effectSourceEntity(user);
        PendingCounterEffectManager.offer(user, target, resolvedTarget -> {
            LivingEntity source = effectSource(user, visualSource);
            playSourceAttack(source);
            source.level().playSound(null, source.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9F, 1.15F);
            source.level().addFreshEntity(new FirecrackersProjectileEntity(source.level(), source, resolvedTarget, amount, settings));
        });

        return true;
    }

    public static boolean slingshotProjectile(ServerPlayer user, LivingEntity target, int amount) {
        return slingshotProjectile(user, target, amount, CardProjectileSettings.slingshot());
    }

    public static boolean slingshotProjectile(ServerPlayer user, LivingEntity target, int amount, CardProjectileSettings settings) {
        if (target == null || !target.isAlive()) return false;
        LivingEntity visualSource = BoardEntityService.effectSourceEntity(user);
        PendingCounterEffectManager.offer(user, target, resolvedTarget -> {
            LivingEntity source = effectSource(user, visualSource);
            playSourceAttack(source);
            source.level().playSound(null, source.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.8F);
            source.level().addFreshEntity(new SlingshotProjectileEntity(source.level(), source, resolvedTarget, amount, settings));
        });

        return true;
    }

    public static boolean snowballAttackProjectile(ServerPlayer user, LivingEntity target, int amount) {
        return snowballAttackProjectile(user, target, amount, CardProjectileSettings.snowballAttack());
    }

    public static boolean snowballAttackProjectile(ServerPlayer user, LivingEntity target, int amount, CardProjectileSettings settings) {
        if (target == null || !target.isAlive()) return false;
        LivingEntity visualSource = BoardEntityService.effectSourceEntity(user);
        PendingCounterEffectManager.offer(user, target, resolvedTarget -> {
            LivingEntity source = effectSource(user, visualSource);
            playSourceAttack(source);
            source.level().playSound(null, source.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 1.2F);
            source.level().addFreshEntity(new SnowballAttackProjectileEntity(source.level(), source, resolvedTarget, amount, settings));
        });

        return true;
    }

    public static boolean fallingBrick(ServerPlayer user, LivingEntity target, int amount) {
        if (target == null || !target.isAlive()) return false;
        LivingEntity visualSource = BoardEntityService.effectSourceEntity(user);
        PendingCounterEffectManager.offer(user, target, resolvedTarget -> {
            LivingEntity source = effectSource(user, visualSource);
            playSourceAttack(source);
            source.level().playSound(null, resolvedTarget.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.55F);
            source.level().addFreshEntity(new FallingBrickEntity(source.level(), source, resolvedTarget, amount, 10));
        });

        return true;
    }

    private static LivingEntity effectSource(ServerPlayer user, LivingEntity captured) {
        return captured != null && captured.isAlive() ? captured : user;
    }

    private static void playSourceAttack(LivingEntity source) {
        if (source instanceof AstralCharacterEntity character) character.playBoardAttackAnimation(12);
    }

    public static void snatchCoins(ServerPlayer user, LivingEntity target, int amount) {
        AstralPlayerStats victim = AstralStats.getOrDefault(target);
        int taken = Math.min(amount, victim.starCoins());
        update(target, victim.spendCoins(taken));
        update(user, AstralStats.get(user).addCoins(taken));
    }

}