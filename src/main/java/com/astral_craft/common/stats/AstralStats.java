package com.astral_craft.common.stats;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Central stats bridge for normal players and logical board pawns. */
public class AstralStats {

    public static AstralPlayerStats get(Player player) {
        AstralPlayerStats stored = player.getData(AstralAttachments.PLAYER_STATS);
        if (player instanceof ServerPlayer serverPlayer) {
            return BoardSessionManager.statsForController(serverPlayer, stored);
        }
        return stored;
    }

    public static void set(Player player, AstralPlayerStats stats) {
        if (player instanceof ServerPlayer serverPlayer && BoardSessionManager.setStatsForController(serverPlayer, stats)) {
            return;
        }
        player.setData(AstralAttachments.PLAYER_STATS, stats);
    }

    public static AstralPlayerStats getOrDefault(LivingEntity entity) {
        if (entity instanceof Player player) return get(player);
        if (entity instanceof AstralCharacterEntity character) {
            return BoardSessionManager.statsForEntity(character, fallback(entity));
        }
        return fallback(entity);
    }

    public static boolean set(LivingEntity entity, AstralPlayerStats stats) {
        if (entity instanceof Player player) {
            set(player, stats);
            return true;
        }
        if (entity instanceof AstralCharacterEntity character) {
            return BoardSessionManager.setStatsForEntity(character, stats);
        }
        entity.setHealth(Math.clamp(stats.health(), 0, Mth.ceil(entity.getMaxHealth())));
        return true;
    }

    private static AstralPlayerStats fallback(LivingEntity entity) {
        int max = Math.max(1, Mth.ceil(entity.getMaxHealth()));
        int hp = Math.max(0, Mth.ceil(entity.getHealth()));
        return new AstralPlayerStats(1, 0, max, hp, 0, 0, 1, 1, 0,
                java.util.Map.of(), java.util.List.of());
    }

}