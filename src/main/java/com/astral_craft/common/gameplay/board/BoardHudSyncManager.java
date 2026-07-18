package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload.PawnView;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Sends typed board snapshots used by the HUD and client-side protection rendering. */
public class BoardHudSyncManager {

    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final double HUD_RANGE_SQR = 512.0D * 512.0D;
    private static final UUID EMPTY_SLOT_ID = new UUID(0L, 0L);
    private static int ticker;

    public static void serverTick(MinecraftServer server) {
        if (++ticker % SYNC_INTERVAL_TICKS != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (BoardSession session : BoardSessionManager.sessions(level)) send(level, session);
        }
    }

    public static void send(ServerLevel level, BoardSession session) {
        BoardHudSnapshotPayload snapshot = createSnapshot(level, session);
        BlockPos center = session.protectedArea().center();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D,
                    center.getZ() + 0.5D) <= HUD_RANGE_SQR) {
                PacketDistributor.sendToPlayer(player, snapshot);
            }
        }
    }

    public static BoardHudSnapshotPayload createSnapshot(ServerLevel level, BoardSession session) {
        List<PawnView> pawns = new ArrayList<>();
        Set<UUID> includedSlots = new LinkedHashSet<>();
        for (UUID slotId : session.turnOrder()) {
            session.participant(slotId).ifPresent(participant -> {
                addParticipant(level, session, pawns, participant);
                includedSlots.add(participant.slotUuid());
            });
        }

        session.participants().stream()
                .filter(participant -> !includedSlots.contains(participant.slotUuid()))
                .forEach(participant -> addParticipant(level, session, pawns, participant));

        BoardArea area = session.protectedArea();
        UUID currentSlotId = session.currentParticipant().map(BoardParticipant::slotUuid).orElse(EMPTY_SLOT_ID);
        return new BoardHudSnapshotPayload(session.id(), area.center(), area.min(), area.max(),
                session.protectionEnabled(), session.phase() == BoardPhase.PLAYING,
                pawns, session.round() + 1, currentSlotId);
    }

    private static void addParticipant(ServerLevel level, BoardSession session, List<PawnView> pawns, BoardParticipant participant) {
        if (!session.positions().containsKey(participant.currentNodeKey())) return;
        pawns.add(new PawnView(participant.characterId(), participant.skinId(), participant.slotUuid(),
                BoardSessionManager.displayName(level, participant), participant.stats().starCoins(),
                participant.stats().health(), participant.stats().maxHealth(), participant.stats().stars(),
                participant.knockedDown(), participant.disconnectedHuman(), participant.hand().size()));
    }

}