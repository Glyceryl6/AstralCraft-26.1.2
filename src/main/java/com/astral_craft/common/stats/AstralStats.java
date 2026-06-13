package com.astral_craft.common.stats;

import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class AstralStats {

    public static AstralPlayerStats get(Player player) {
        return player.getData(AstralAttachments.PLAYER_STATS);
    }

    public static void set(Player player, AstralPlayerStats stats) {
        player.setData(AstralAttachments.PLAYER_STATS, stats);
    }

    public static AstralPlayerStats getOrDefault(LivingEntity entity) {
        if (entity instanceof Player player) return get(player);
        int max = Math.max(1, Mth.ceil(entity.getMaxHealth()));
        int hp = Math.max(0, Mth.ceil(entity.getHealth()));
        return new AstralPlayerStats(1, 0, 0, max, hp, 0, 0, 1, 1, 0, 0, 0, java.util.Map.of(), java.util.List.of());
    }

}