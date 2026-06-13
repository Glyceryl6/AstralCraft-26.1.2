package com.astral_craft.common.gameplay.battle;

import com.astral_craft.common.network.OpenBattleScenePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Utility methods for opening the interactive battle scene on both participants. */
public final class BattleSceneService {
    private BattleSceneService() {}

    public static void open(ServerPlayer attacker, ServerPlayer defender, int cost, int decisionTicks, String attackerCards, String defenderCards) {
        PacketDistributor.sendToPlayer(attacker, new OpenBattleScenePayload(attacker.getId(), defender.getId(), true, cost, decisionTicks, attackerCards));
        PacketDistributor.sendToPlayer(defender, new OpenBattleScenePayload(attacker.getId(), defender.getId(), false, cost, decisionTicks, defenderCards));
    }
}
