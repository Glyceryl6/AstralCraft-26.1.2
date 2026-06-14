package com.astral_craft.common.gameplay;

import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freedom-friendly knockdown: the Minecraft body is not hard-frozen, but Astral actions are disabled
 * for a short recovery window. BoardSession can later replace RECOVERY_TICKS with "skip exactly one
 * board turn" while reusing isRecovering().
 */
public class KnockdownManager {

    private static final int RECOVERY_TICKS = 20 * 12;
    private static final Map<UUID, Recovery> RECOVERING = new ConcurrentHashMap<>();

    public static void checkKnockdown(ServerPlayer player, AstralPlayerStats stats) {
        if (stats.health() > 0 || RECOVERING.containsKey(player.getUUID())) return;
        int lost = Math.max(0, (stats.starCoins() + 1) / 2);
        AstralPlayerStats next = stats.spendCoins(lost).withHealth(0);
        AstralStats.set(player, next);
        RECOVERING.put(player.getUUID(), new Recovery(player, RECOVERY_TICKS));
        player.sendSystemMessage(Component.translatable("message.astral_craft.knockdown", lost).withStyle(ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.9F, 0.65F);
    }

    public static boolean isRecovering(ServerPlayer player) {
        return RECOVERING.containsKey(player.getUUID());
    }

    public static void serverTick() {
        Iterator<Map.Entry<UUID, Recovery>> iterator = RECOVERING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Recovery> entry = iterator.next();
            Recovery recovery = entry.getValue();
            if (--recovery.ticksLeft > 0) continue;
            ServerPlayer player = recovery.player;
            if (player.isAlive()) {
                AstralPlayerStats stats = AstralStats.get(player);
                AstralStats.set(player, stats.withHealth(stats.maxHealth()));
                player.sendSystemMessage(Component.translatable("message.astral_craft.recovered").withStyle(ChatFormatting.GREEN), true);
            }

            iterator.remove();
        }
    }

    private static final class Recovery {

        private final ServerPlayer player;
        private int ticksLeft;

        private Recovery(ServerPlayer player, int ticksLeft) {
            this.player = player;
            this.ticksLeft = ticksLeft;
        }

    }

}