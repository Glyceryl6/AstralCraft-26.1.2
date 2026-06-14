package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.network.BoardHudSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;

/** Sends small board-session snapshots to nearby players for the HUD board projection. */
public class BoardHudSyncManager {

    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final double HUD_RANGE_SQR = 96.0D * 96.0D;
    private static int ticker;

    public static void serverTick() {
        if (++ticker % SYNC_INTERVAL_TICKS != 0) return;
        for (BoardSession session : BoardSessionManager.sessions()) {
            ServerLevel level = levelFor(session);
            if (level == null) continue;
            String encoded = encode(session);
            BlockPos center = session.protectedArea().center();
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= HUD_RANGE_SQR) {
                    PacketDistributor.sendToPlayer(player, new BoardHudSnapshotPayload(encoded));
                }
            }
        }
    }

    private static ServerLevel levelFor(BoardSession session) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getLevel(session.dimension());
    }

    private static String encode(BoardSession session) {
        StringBuilder out = new StringBuilder();
        BlockPos center = session.protectedArea().center();
        out.append(session.id()).append('|')
                .append(center.getX()).append(',').append(center.getY()).append(',').append(center.getZ()).append('|');
        session.positions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(256)
                .forEach(entry -> {
                    BoardNode node = session.nodes().get(entry.getKey());
                    BlockPos pos = entry.getValue();
                    if (node != null) {
                        out.append(entry.getKey()).append(',')
                                .append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append(',')
                                .append(node.panelTypeId()).append(';');
                    }
                });
        return out.toString();
    }

}