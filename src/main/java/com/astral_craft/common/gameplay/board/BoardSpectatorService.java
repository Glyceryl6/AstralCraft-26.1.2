package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.network.s2c.CloseBoardPresentationPayload;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Keeps the temporary one-board presentation subscription created by the spectator tool. */
public class BoardSpectatorService {

    private static final Map<UUID, UUID> WATCHED_BOARDS = new LinkedHashMap<>();

    public static boolean toggle(ServerPlayer player, BoardSession session) {
        if (player == null || session == null || session.phase() != BoardPhase.PLAYING) return false;
        UUID previous = WATCHED_BOARDS.get(player.getUUID());
        if (session.id().equals(previous)) {
            removeBinding(player, previous);
            return false;
        }

        if (previous != null) closePresentation(player, previous);
        WATCHED_BOARDS.put(player.getUUID(), session.id());
        return true;
    }

    public static void stopWatching(ServerPlayer player) {
        if (player == null) return;
        UUID previous = WATCHED_BOARDS.get(player.getUUID());
        if (previous != null) removeBinding(player, previous);
    }

    public static Optional<UUID> watchedBoard(ServerPlayer player) {
        return player == null ? Optional.empty() : Optional.ofNullable(WATCHED_BOARDS.get(player.getUUID()));
    }

    public static List<ServerPlayer> presentationViewers(ServerLevel level, BoardSession session) {
        Set<ServerPlayer> viewers = new LinkedHashSet<>(BoardSessionManager.humanPlayers(level, session));
        viewers.addAll(spectators(level, session));
        return List.copyOf(viewers);
    }

    public static List<ServerPlayer> spectators(ServerLevel level, BoardSession session) {
        List<ServerPlayer> result = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : WATCHED_BOARDS.entrySet()) {
            if (!session.id().equals(entry.getValue())) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.level() == level && hasSpectatorTool(player)) result.add(player);
        }

        return List.copyOf(result);
    }

    public static void clearBoard(ServerLevel level, UUID boardId) {
        if (level == null || boardId == null) return;
        Iterator<Map.Entry<UUID, UUID>> iterator = WATCHED_BOARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            if (!boardId.equals(entry.getValue())) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) closePresentation(player, boardId);
            iterator.remove();
        }
    }

    public static void serverTick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, UUID>> iterator = WATCHED_BOARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            boolean remove = player == null || !hasSpectatorTool(player);
            if (!remove) {
                remove = true;
                for (ServerLevel level : server.getAllLevels()) {
                    BoardSession session = BoardSessionManager.session(level, entry.getValue()).orElse(null);
                    if (session != null) {
                        remove = session.phase() != BoardPhase.PLAYING;
                        break;
                    }
                }
            }

            if (!remove) continue;
            if (player != null) closePresentation(player, entry.getValue());
            iterator.remove();
        }
    }

    private static void removeBinding(ServerPlayer player, UUID boardId) {
        WATCHED_BOARDS.remove(player.getUUID());
        closePresentation(player, boardId);
    }

    private static void closePresentation(ServerPlayer player, UUID boardId) {
        boolean participant = false;
        for (ServerLevel level : player.server.getAllLevels()) {
            BoardSession session = BoardSessionManager.session(level, boardId).orElse(null);
            if (session != null) {
                participant = session.participantByController(player.getUUID()).isPresent();
                break;
            }
        }

        if (!participant) PacketDistributor.sendToPlayer(player, new CloseBoardPresentationPayload(boardId));
    }

    private static boolean hasSpectatorTool(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(AstralItems.BOARD_SPECTATOR.get())) return true;
        }
        return false;
    }

}