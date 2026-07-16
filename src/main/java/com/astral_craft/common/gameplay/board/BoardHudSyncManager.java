package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sends compact board snapshots used by the HUD and client-side protection rendering. */
public class BoardHudSyncManager {

    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final double HUD_RANGE_SQR = 512.0D * 512.0D;
    private static int ticker;

    public static void serverTick(MinecraftServer server) {
        if (++ticker % SYNC_INTERVAL_TICKS != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (BoardSession session : BoardSessionManager.sessions(level)) send(level, session);
        }
    }

    public static void send(ServerLevel level, BoardSession session) {
        String encoded = encode(level, session);
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D,
                    center.getZ() + 0.5D) <= HUD_RANGE_SQR) {
                PacketDistributor.sendToPlayer(player, new BoardHudSnapshotPayload(encoded));
            }
        }
    }

    public static String encode(ServerLevel level, BoardSession session) {
        StringBuilder out = new StringBuilder();
        BlockPos center = session.protectedArea().center();
        BoardArea area = session.protectedArea();
        out.append(session.id()).append('|')
                .append(center.getX()).append(',').append(center.getY()).append(',').append(center.getZ()).append('|');
        session.positions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(512)
                .forEach(entry -> {
                    BoardNode node = session.nodes().get(entry.getKey());
                    BlockPos pos = entry.getValue();
                    if (node != null) {
                        out.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append(',')
                                .append(session.startNodes().contains(entry.getKey()) ? '1' : '0').append(';');
                    }
                });
        out.append('|')
                .append(area.min().getX()).append(',').append(area.min().getY()).append(',').append(area.min().getZ()).append(',')
                .append(area.max().getX()).append(',').append(area.max().getY()).append(',').append(area.max().getZ()).append('|')
                .append(session.protectionEnabled()).append('|')
                .append(session.phase().name()).append('|');
        Set<UUID> encodedSlots = new LinkedHashSet<>();
        for (UUID slotId : session.turnOrder()) {
            session.participant(slotId).ifPresent(participant -> {
                appendParticipant(level, session, out, participant);
                encodedSlots.add(participant.slotUuid());
            });
        }

        session.participants().stream().filter(participant -> !encodedSlots.contains(participant.slotUuid()))
                .forEach(participant -> appendParticipant(level, session, out, participant));
        out.append('|').append(session.round() + 1).append('|')
                .append(session.currentParticipant().map(value -> value.slotUuid().toString()).orElse(""));
        return out.toString();
    }

    private static void appendParticipant(ServerLevel level, BoardSession session, StringBuilder out, BoardParticipant participant) {
        BlockPos pos = session.positions().get(participant.currentNodeKey());
        if (pos == null) return;
        out.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append(',')
                .append(participant.characterId()).append(',').append(participant.skinName()).append(',')
                .append(participant.slotUuid()).append(',')
                .append(encodeName(BoardSessionManager.displayName(level, participant))).append(',')
                .append(participant.stats().starCoins()).append(',')
                .append(participant.stats().health()).append(',')
                .append(participant.stats().maxHealth()).append(',')
                .append(participant.stats().stars()).append(',')
                .append(participant.knockedDown() ? '1' : '0').append(',')
                .append(participant.disconnectedHuman() ? '1' : '0').append(';');
    }

    private static String encodeName(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

}