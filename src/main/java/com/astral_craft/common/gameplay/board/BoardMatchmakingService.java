package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload.PlayerEntry;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload.State;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Runtime-only matchmaking state kept separate from the persistent board session. */
public class BoardMatchmakingService {

    public static final int SINGLE_START_DELAY_TICKS = 20 * 3;
    public static final int MATCH_FOUND_DELAY_TICKS = 20 * 5;
    private static final Map<UUID, MatchState> MATCHES = new HashMap<>();

    public static void openModeSelection(ServerPlayer player, BoardSession session) {
        MatchState ownMatch = findPlayerMatch(player.getUUID());
        if (ownMatch != null) {
            if (!ownMatch.boardId.equals(session.id())) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.already_matching"), true);
                return;
            }
            if (!ownMatch.selectionStarted && ownMatch.mode == BoardMatchmakingMode.MULTIPLAYER) {
                sendMatchmakingState(player.level(), ownMatch, player);
                return;
            }
        }

        sendIdle(player, session.id());
    }

    public static void selectMode(ServerPlayer player, UUID boardId, BoardMatchmakingMode mode, boolean tutorial) {
        if (mode == null || !holdsLobbyItem(player) || tutorial && mode != BoardMatchmakingMode.SINGLE_PLAYER) return;
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.READY) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            if (mode == BoardMatchmakingMode.MULTIPLAYER) sendIdle(player, boardId);
            return;
        }

        MatchState ownMatch = findPlayerMatch(player.getUUID());
        if (ownMatch != null && !ownMatch.boardId.equals(boardId)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.already_matching"), true);
            if (mode == BoardMatchmakingMode.MULTIPLAYER) sendIdle(player, boardId);
            return;
        }

        if (mode == BoardMatchmakingMode.SINGLE_PLAYER) {
            startSinglePlayer(player, session, tutorial);
        } else {
            joinMultiplayer(player, session);
        }
    }

    public static void cancelMatchmaking(ServerPlayer player, UUID boardId, boolean returnToSelection) {
        MatchState state = MATCHES.get(boardId);
        if (state == null) {
            BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
            if (returnToSelection && session != null && session.phase() == BoardPhase.READY) sendIdle(player, boardId);
            return;
        }
        if (state.mode != BoardMatchmakingMode.MULTIPLAYER || state.selectionStarted
                || !state.playerIds.remove(player.getUUID())) return;

        state.joinTicks.remove(player.getUUID());
        state.matchFoundTick = -1L;
        if (returnToSelection) sendIdle(player, boardId);
        if (state.playerIds.isEmpty()) {
            MATCHES.remove(boardId);
            return;
        }

        ServerLevel level = player.server.getLevel(state.dimension);
        if (level != null) notifyWaiting(level, state);
    }

    public static void tick(ServerLevel level, BoardSession session) {
        MatchState state = MATCHES.get(session.id());
        if (state == null || state.mode != BoardMatchmakingMode.MULTIPLAYER || state.selectionStarted
                || session.phase() != BoardPhase.READY) return;
        if (pruneWaitingPlayers(level, state)) {
            state.matchFoundTick = -1L;
            if (state.playerIds.isEmpty()) {
                MATCHES.remove(state.boardId);
                return;
            }
            notifyWaiting(level, state);
        }

        if (state.playerIds.size() < BoardSessionManager.REQUIRED_PLAYERS) return;
        long now = AstralServerTickClock.now(level);
        if (state.matchFoundTick < 0L) {
            state.matchFoundTick = now;
            notifyMatched(level, state);
            return;
        }

        int remaining = (int) Math.max(0L, state.matchFoundTick + MATCH_FOUND_DELAY_TICKS - now);
        if (remaining <= 0) {
            beginCharacterSelection(level, session, state);
        } else if (remaining % 20 == 0) {
            notifyMatched(level, state);
        }
    }

    public static int selectionSlot(UUID boardId, UUID playerId) {
        MatchState state = MATCHES.get(boardId);
        if (state == null || state.mode != BoardMatchmakingMode.MULTIPLAYER || !state.selectionStarted) return -1;
        int slot = 0;
        for (UUID matchedPlayerId : state.playerIds) {
            if (matchedPlayerId.equals(playerId)) return slot;
            slot++;
        }
        return -1;
    }

    public static boolean canSelectCharacter(ServerPlayer player, BoardSession session) {
        MatchState state = MATCHES.get(session.id());
        return state == null || state.selectionStarted && state.playerIds.contains(player.getUUID());
    }

    /** @return true when the confirmation belonged to the new matchmaking flow. */
    public static boolean handleConfirmedSelection(ServerLevel level, BoardSession session) {
        MatchState state = MATCHES.get(session.id());
        if (state == null || !state.selectionStarted) return false;
        if (state.mode == BoardMatchmakingMode.SINGLE_PLAYER) {
            BoardSessionManager.fillBots(level, session);
            session.setLobbyDeadlineTick(AstralServerTickClock.now(level) + SINGLE_START_DELAY_TICKS);
            BoardSessionManager.markChanged(level);
            for (UUID playerId : state.playerIds) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.single_starting",
                            SINGLE_START_DELAY_TICKS / 20), true);
                }
            }
            BoardLobbyService.refreshScreens(level, session);
            return true;
        }

        if (state.playerIds.size() >= BoardSessionManager.REQUIRED_PLAYERS
                && session.participantCount() >= BoardSessionManager.REQUIRED_PLAYERS) {
            BoardSessionManager.startGame(level, session);
        } else {
            BoardLobbyService.refreshScreens(level, session);
        }
        return true;
    }

    public static int selectionTimeoutDuration(BoardSession session) {
        MatchState state = MATCHES.get(session.id());
        if (state != null && state.selectionStarted && state.mode == BoardMatchmakingMode.SINGLE_PLAYER) {
            if (session.participantCount() > 0) return SINGLE_START_DELAY_TICKS;
            if (BoardTutorialPolicy.enabled(session)) return BoardTutorialPolicy.UNLIMITED_DECISION_TICKS;
        }
        return BoardSessionManager.LOBBY_TIMEOUT_TICKS;
    }

    /** @return true when a matchmaking-specific timeout consumed the normal lobby timeout flow. */
    public static boolean handleSelectionTimeout(ServerLevel level, BoardSession session) {
        MatchState state = MATCHES.get(session.id());
        if (state == null || !state.selectionStarted || state.mode != BoardMatchmakingMode.MULTIPLAYER) return false;
        boolean allPlayersOnline = state.playerIds.size() == BoardSessionManager.REQUIRED_PLAYERS
                && state.playerIds.stream().map(level.getServer().getPlayerList()::getPlayer)
                .allMatch(player -> player != null && player.level() == level);
        if (allPlayersOnline && session.participantCount() >= BoardSessionManager.REQUIRED_PLAYERS) return false;
        disband(level.getServer(), state);
        return true;
    }

    public static void leaveCharacterSelection(ServerPlayer player, UUID boardId) {
        MatchState state = MATCHES.get(boardId);
        if (state == null || !state.selectionStarted || !state.playerIds.contains(player.getUUID())) return;
        disband(player.server, state);
    }

    public static void handleLogout(ServerPlayer player) {
        MatchState state = findPlayerMatch(player.getUUID());
        if (state == null) return;
        if (state.selectionStarted) {
            disband(player.server, state);
            return;
        }

        ServerLevel level = player.server.getLevel(state.dimension);
        state.playerIds.remove(player.getUUID());
        state.joinTicks.remove(player.getUUID());
        state.matchFoundTick = -1L;
        if (state.playerIds.isEmpty()) {
            MATCHES.remove(state.boardId);
        } else if (level != null) {
            notifyWaiting(level, state);
        }
    }

    public static boolean active(UUID boardId) {
        return MATCHES.containsKey(boardId);
    }

    public static void clear(UUID boardId) {
        MATCHES.remove(boardId);
    }

    private static void startSinglePlayer(ServerPlayer player, BoardSession session, boolean tutorial) {
        MatchState existing = MATCHES.get(session.id());
        if (existing != null && (!existing.playerIds.contains(player.getUUID()) || existing.mode != BoardMatchmakingMode.SINGLE_PLAYER)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            return;
        }

        BoardTutorialPolicy.setEnabled(session.id(), tutorial);
        MatchState state = existing == null
                ? new MatchState(session.id(), player.level().dimension(), BoardMatchmakingMode.SINGLE_PLAYER)
                : existing;
        state.playerIds.add(player.getUUID());
        MATCHES.put(session.id(), state);
        beginCharacterSelection(player.level(), session, state);
    }

    private static void joinMultiplayer(ServerPlayer player, BoardSession session) {
        MatchState state = MATCHES.get(session.id());
        if (state != null && state.mode != BoardMatchmakingMode.MULTIPLAYER) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            sendIdle(player, session.id());
            return;
        }
        if (state == null) {
            state = new MatchState(session.id(), player.level().dimension(), BoardMatchmakingMode.MULTIPLAYER);
            MATCHES.put(session.id(), state);
        }

        BoardTutorialPolicy.setEnabled(session.id(), false);
        if (pruneWaitingPlayers(player.level(), state)) state.matchFoundTick = -1L;
        if (state.selectionStarted || state.playerIds.size() >= BoardSessionManager.REQUIRED_PLAYERS) {
            if (state.playerIds.contains(player.getUUID())) sendMatchmakingState(player.level(), state, player);
            else {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
                sendIdle(player, session.id());
            }
            return;
        }
        if (state.playerIds.contains(player.getUUID())) {
            sendMatchmakingState(player.level(), state, player);
            return;
        }

        state.playerIds.add(player.getUUID());
        state.joinTicks.put(player.getUUID(), AstralServerTickClock.now(player.level()));
        if (state.playerIds.size() >= BoardSessionManager.REQUIRED_PLAYERS) {
            state.matchFoundTick = AstralServerTickClock.now(player.level());
            notifyMatched(player.level(), state);
        } else {
            notifyWaiting(player.level(), state);
        }
    }

    private static void beginCharacterSelection(ServerLevel level, BoardSession session, MatchState state) {
        if (session.phase() != BoardPhase.READY || state.selectionStarted) return;
        state.selectionStarted = true;
        session.setProtectionEnabled(true);
        session.setPhase(BoardPhase.CHARACTER_SELECTION);
        session.setLobbyDeadlineTick(BoardTutorialPolicy.enabled(session) ? Long.MAX_VALUE
                : AstralServerTickClock.now(level) + BoardSessionManager.LOBBY_TIMEOUT_TICKS);
        BoardSessionManager.markChanged(level);
        BoardProtectionService.refreshProtectedAreas(level, BoardSavedData.get(level));
        for (UUID playerId : state.playerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null && player.level() == level) {
                BoardLobbyService.registerViewer(player, session, selectionSlot(session.id(), playerId));
            }
        }
    }

    private static void sendMatchmakingState(ServerLevel level, MatchState state, ServerPlayer player) {
        if (state.matchFoundTick >= 0L && state.playerIds.size() >= BoardSessionManager.REQUIRED_PLAYERS) {
            sendMatched(level, state, player);
        } else {
            sendWaiting(level, state, player);
        }
    }

    private static void notifyWaiting(ServerLevel level, MatchState state) {
        pruneWaitingPlayers(level, state);
        for (UUID playerId : state.playerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) sendWaiting(level, state, player);
        }
    }

    private static void sendWaiting(ServerLevel level, MatchState state, ServerPlayer player) {
        long joined = state.joinTicks.getOrDefault(player.getUUID(), AstralServerTickClock.now(level));
        int elapsed = (int) Math.clamp(AstralServerTickClock.now(level) - joined, 0L, Integer.MAX_VALUE);
        PacketDistributor.sendToPlayer(player, new OpenBoardMatchmakingModeSelectionPayload(state.boardId, State.WAITING,
                elapsed, 0, playerEntries(level, state)));
    }

    private static void notifyMatched(ServerLevel level, MatchState state) {
        for (UUID playerId : state.playerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) sendMatched(level, state, player);
        }
    }

    private static void sendMatched(ServerLevel level, MatchState state, ServerPlayer player) {
        long now = AstralServerTickClock.now(level);
        long joined = state.joinTicks.getOrDefault(player.getUUID(), now);
        int elapsed = (int) Math.clamp(now - joined, 0L, Integer.MAX_VALUE);
        int countdown = state.matchFoundTick < 0L ? MATCH_FOUND_DELAY_TICKS
                : (int) Math.clamp(state.matchFoundTick + MATCH_FOUND_DELAY_TICKS - now, 0L, MATCH_FOUND_DELAY_TICKS);
        PacketDistributor.sendToPlayer(player, new OpenBoardMatchmakingModeSelectionPayload(state.boardId, State.MATCHED,
                elapsed, countdown, playerEntries(level, state)));
    }

    private static List<PlayerEntry> playerEntries(ServerLevel level, MatchState state) {
        List<PlayerEntry> players = new ArrayList<>();
        for (UUID playerId : state.playerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) players.add(new PlayerEntry(playerId, player.getGameProfile().name()));
        }
        return List.copyOf(players);
    }

    private static void sendIdle(ServerPlayer player, UUID boardId) {
        PacketDistributor.sendToPlayer(player, new OpenBoardMatchmakingModeSelectionPayload(boardId, State.IDLE, 0, 0, List.of()));
    }

    private static boolean pruneWaitingPlayers(ServerLevel level, MatchState state) {
        if (state.selectionStarted) return false;
        boolean removed = state.playerIds.removeIf(playerId -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null && player.level() == level) return false;
            state.joinTicks.remove(playerId);
            if (player != null) sendIdle(player, state.boardId);
            return true;
        });
        return removed;
    }

    private static void disband(MinecraftServer server, MatchState state) {
        ServerLevel level = server.getLevel(state.dimension);
        MATCHES.remove(state.boardId);
        if (level == null) return;
        BoardSession session = BoardSessionManager.session(level, state.boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.CHARACTER_SELECTION) return;
        ArrayList<UUID> players = new ArrayList<>(state.playerIds);
        BoardLobbyService.closeScreens(level, state.boardId);
        for (UUID playerId : players) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.disbanded"), false);
        }
        BoardSessionManager.resetForLobby(level, session);
    }

    private static MatchState findPlayerMatch(UUID playerId) {
        return MATCHES.values().stream().filter(state -> state.playerIds.contains(playerId)).findFirst().orElse(null);
    }

    private static boolean holdsLobbyItem(ServerPlayer player) {
        return player.getMainHandItem().is(AstralItems.BOARD_LOBBY.get()) || player.getOffhandItem().is(AstralItems.BOARD_LOBBY.get());
    }

    private static class MatchState {

        private final UUID boardId;
        private final ResourceKey<Level> dimension;
        private final BoardMatchmakingMode mode;
        private final LinkedHashSet<UUID> playerIds = new LinkedHashSet<>();
        private final Map<UUID, Long> joinTicks = new HashMap<>();
        private boolean selectionStarted;
        private long matchFoundTick = -1L;

        private MatchState(UUID boardId, ResourceKey<Level> dimension, BoardMatchmakingMode mode) {
            this.boardId = boardId;
            this.dimension = dimension;
            this.mode = mode;
        }

    }

}