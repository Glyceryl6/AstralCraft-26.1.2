package com.astral_craft.common.gameplay.battle;

import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.stats.AstralStats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Snapshot of stats when a combat roll is resolved. */
public record BattleParticipantSnapshot(int entityId, String displayName, int attack, int defense) {
    public static BattleParticipantSnapshot of(LivingEntity entity) {
        if (entity instanceof Player player) {
            AstralPlayerStats stats = AstralStats.get(player);
            return new BattleParticipantSnapshot(entity.getId(), entity.getDisplayName().getString(), stats.attack(), stats.defense());
        }
        return new BattleParticipantSnapshot(entity.getId(), entity.getDisplayName().getString(), 0, 0);
    }
}
