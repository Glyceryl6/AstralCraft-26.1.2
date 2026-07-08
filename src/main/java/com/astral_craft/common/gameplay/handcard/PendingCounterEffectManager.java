package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.entity.projectile.CardProjectileSettings;
import com.astral_craft.common.entity.projectile.FirecrackersProjectileEntity;
import com.astral_craft.common.entity.projectile.SlingshotProjectileEntity;
import com.astral_craft.common.entity.projectile.SnowballAttackProjectileEntity;
import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counter-card window for targeted effect damage.
 *
 * <p>Cards call one of the offer* methods after the reveal animation. If the target is a player,
 * the actual damage/visual is held for a short window. During that window the target can right-click
 * Barrier, Random Select, or Eye for an Eye. Non-player targets are resolved immediately.</p>
 */
public class PendingCounterEffectManager {

    public static final int DEFAULT_RESPONSE_TICKS = 60;

    private static final Map<UUID, PendingEffect> BY_TARGET = new ConcurrentHashMap<>();

    public static void offerDirectDamage(ServerPlayer source, LivingEntity target, int damage) {
        offer(PendingEffect.direct(source, target, damage));
    }

    public static void offerLaser(ServerPlayer source, LivingEntity target, int damage, int argb, float radius) {
        offer(PendingEffect.laser(source, target, damage, argb, radius));
    }

    public static void offerRailgun(ServerPlayer source, LivingEntity target, int damage, int argb, float radius) {
        offer(PendingEffect.laser(source, target, damage, argb, radius));
    }

    public static void offerFirecracker(ServerPlayer source, LivingEntity target, int damage) {
        offerFirecracker(source, target, damage, CardProjectileSettings.firecrackers());
    }

    public static void offerFirecracker(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        offer(PendingEffect.projectile(source, target, damage, VisualKind.FIRECRACKERS, settings));
    }

    public static void offerSlingshot(ServerPlayer source, LivingEntity target, int damage) {
        offerSlingshot(source, target, damage, CardProjectileSettings.slingshot());
    }

    public static void offerSlingshot(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        offer(PendingEffect.projectile(source, target, damage, VisualKind.SLINGSHOT, settings));
    }

    public static void offerSnowballAttack(ServerPlayer source, LivingEntity target, int damage) {
        offerSnowballAttack(source, target, damage, CardProjectileSettings.snowballAttack());
    }

    public static void offerSnowballAttack(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        offer(PendingEffect.projectile(source, target, damage, VisualKind.SNOWBALL_ATTACK, settings));
    }

    public static void offerFallingBrick(ServerPlayer source, LivingEntity target, int damage) {
        offer(PendingEffect.projectile(source, target, damage, VisualKind.FALLING_BRICK, CardProjectileSettings.slingshot()));
    }

    private static void offer(PendingEffect effect) {
        if (!(effect.target() instanceof ServerPlayer player)) {
            resolve(effect, effect.target(), false);
            return;
        }

        BY_TARGET.put(player.getUUID(), effect.withTicksLeft(DEFAULT_RESPONSE_TICKS));
        player.sendSystemMessage(Component.translatable("message.astral_craft.counter.prompt", effect.source().getDisplayName()).withStyle(ChatFormatting.AQUA), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.35F);
    }

    public static boolean respond(ServerPlayer target, CounterAction action) {
        PendingEffect effect = BY_TARGET.remove(target.getUUID());
        if (effect == null) {
            target.sendSystemMessage(Component.translatable("message.astral_craft.counter.none").withStyle(ChatFormatting.GRAY), true);
            return false;
        }

        switch (action) {
            case BARRIER -> {
                target.sendSystemMessage(Component.translatable("message.astral_craft.counter.barrier").withStyle(ChatFormatting.GREEN), true);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.95F, 1.2F);
                return true;
            }
            
            case EYE_FOR_AN_EYE -> {
                target.sendSystemMessage(Component.translatable("message.astral_craft.counter.reflect").withStyle(ChatFormatting.GOLD), true);
                target.level().playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.55F);
                resolve(effect, effect.source(), true);
                return true;
            }
            
            case RANDOM_SELECT -> {
                LivingEntity redirected = randomOtherPlayer(effect, target.level(), target);
                if (redirected == null) {
                    target.sendSystemMessage(Component.translatable("message.astral_craft.counter.random_failed").withStyle(ChatFormatting.YELLOW), true);
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 0.8F);
                    return true;
                }
                
                target.sendSystemMessage(Component.translatable("message.astral_craft.counter.random", redirected.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                target.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.85F, 1.3F);
                resolve(effect, redirected, true);
                return true;
            }
        }

        return false;
    }

    public static void serverTick(MinecraftServer server) {
        List<UUID> keys = new ArrayList<>(BY_TARGET.keySet());
        for (UUID key : keys) {
            PendingEffect current = BY_TARGET.get(key);
            if (current == null) continue;
            PendingEffect next = current.tickDown();
            if (next.ticksLeft() > 0) {
                BY_TARGET.put(key, next);
                continue;
            }
            
            BY_TARGET.remove(key, current);
            Entity target = next.source().level().getEntity(key);
            if (target instanceof LivingEntity living && living.isAlive()) {
                resolve(next, living, false);
            }
        }
    }

    private static LivingEntity randomOtherPlayer(PendingEffect effect, ServerLevel level, ServerPlayer originalTarget) {
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && player != originalTarget && player != effect.source()) {
                candidates.add(player);
            }
        }
        
        if (candidates.isEmpty()) {
            for (ServerPlayer player : level.players()) {
                if (player.isAlive() && player != originalTarget) {
                    candidates.add(player);
                }
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        return candidates.get(level.getRandom().nextInt(candidates.size()));
    }

    private static void resolve(PendingEffect effect, LivingEntity target, boolean countered) {
        if (!target.isAlive() || !effect.source().isAlive()) return;
        switch (effect.kind()) {
            case DIRECT -> AstralCardEffects.damageNow(effect.source(), target, effect.damage());
            case LASER -> spawnLaser(effect.source(), target, effect.damage(), effect.argb(), effect.radius());
            case FIRECRACKERS -> spawnFirecrackers(effect.source(), target, effect.damage(), effect.projectileSettings());
            case SLINGSHOT -> spawnSlingshot(effect.source(), target, effect.damage(), effect.projectileSettings());
            case SNOWBALL_ATTACK -> spawnSnowball(effect.source(), target, effect.damage(), effect.projectileSettings());
            case FALLING_BRICK -> spawnBrick(effect.source(), target, effect.damage());
        }
    }

    private static void spawnLaser(ServerPlayer source, LivingEntity target, int damage, int argb, float radius) {
        LaserStrikeEntity entity = new LaserStrikeEntity(source.level(), source, target, damage, argb, radius);
        source.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnFirecrackers(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        FirecrackersProjectileEntity entity = new FirecrackersProjectileEntity(source.level(), source, target, damage, settings);
        source.level().playSound(null, source.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9F, 1.15F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnSlingshot(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        SlingshotProjectileEntity entity = new SlingshotProjectileEntity(source.level(), source, target, damage, settings);
        source.level().playSound(null, source.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.8F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnSnowball(ServerPlayer source, LivingEntity target, int damage, CardProjectileSettings settings) {
        SnowballAttackProjectileEntity entity = new SnowballAttackProjectileEntity(source.level(), source, target, damage, settings);
        source.level().playSound(null, source.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 1.2F);
        source.level().addFreshEntity(entity);
    }

    private static void spawnBrick(ServerPlayer source, LivingEntity target, int damage) {
        FallingBrickEntity entity = new FallingBrickEntity(source.level(), source, target, damage, 10);
        source.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.55F);
        source.level().addFreshEntity(entity);
    }

    public enum CounterAction {
        BARRIER,
        RANDOM_SELECT,
        EYE_FOR_AN_EYE
    }

    private enum VisualKind {
        DIRECT,
        LASER,
        FIRECRACKERS,
        SLINGSHOT,
        SNOWBALL_ATTACK,
        FALLING_BRICK
    }

    private record PendingEffect(ServerPlayer source, LivingEntity target, int damage, VisualKind kind, int argb, float radius, CardProjectileSettings projectileSettings, int ticksLeft) {

        static PendingEffect direct(ServerPlayer source, LivingEntity target, int damage) {
            return new PendingEffect(source, target, damage, VisualKind.DIRECT, 0xFFFFFFFF, 0.08F, CardProjectileSettings.slingshot(), DEFAULT_RESPONSE_TICKS);
        }

        static PendingEffect laser(ServerPlayer source, LivingEntity target, int damage, int argb, float radius) {
            return new PendingEffect(source, target, damage, VisualKind.LASER, argb, radius, CardProjectileSettings.slingshot(), DEFAULT_RESPONSE_TICKS);
        }

        static PendingEffect projectile(ServerPlayer source, LivingEntity target, int damage, VisualKind kind, CardProjectileSettings settings) {
            return new PendingEffect(source, target, damage, kind, 0xFFFFFFFF, 0.08F, settings, DEFAULT_RESPONSE_TICKS);
        }

        PendingEffect withTicksLeft(int ticks) {
            return new PendingEffect(this.source, this.target, this.damage, this.kind, this.argb, this.radius, this.projectileSettings, ticks);
        }

        PendingEffect tickDown() {
            return withTicksLeft(this.ticksLeft - 1);
        }

    }

}