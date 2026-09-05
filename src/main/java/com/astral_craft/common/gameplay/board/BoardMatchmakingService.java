package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload;
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
    private static final int TUTORIAL_DECISION_TICKS = Integer.MAX_VALUE;
    private static final Map<UUID, MatchState> MATCHES = new HashMap<>();
    private static final Set<UUID> TUTORIAL_BOARDS = new HashSet<>();

    public static void openModeSelection(ServerPlayer player, BoardSession session) {
        MatchState ownMatch = findPlayerMatch(player.getUUID());
        if (ownMatch != null) {
            if (!ownMatch.boardId.equals(session.id())) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.already_matching"), true);
                return;
            }
            if (!ownMatch.selectionStarted && ownMatch.mode == BoardMatchmakingMode.MULTIPLAYER) {
                notifyWaiting(player.level(), ownMatch);
                return;
            }
        }

        PacketDistributor.sendToPlayer(player, new OpenBoardMatchmakingModeSelectionPayload(session.id()));
    }

    public static void selectMode(ServerPlayer player, UUID boardId, BoardMatchmakingMode mode, boolean tutorial) {
        if (mode == null || !holdsLobbyItem(player) || tutorial && mode != BoardMatchmakingMode.SINGLE_PLAYER) return;
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.READY) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            return;
        }

        MatchState ownMatch = findPlayerMatch(player.getUUID());
        if (ownMatch != null && !ownMatch.boardId.equals(boardId)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.already_matching"), true);
            return;
        }

        if (tutorial) TUTORIAL_BOARDS.add(boardId);
        else TUTORIAL_BOARDS.remove(boardId);
        if (mode == BoardMatchmakingMode.SINGLE_PLAYER) {
            startSinglePlayer(player, session);
        } else {
            joinMultiplayer(player, session);
        }
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
            if (tutorial(session.id())) return TUTORIAL_DECISION_TICKS;
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
        if (state.playerIds.isEmpty()) {
            MATCHES.remove(state.boardId);
        } else if (level != null) {
            notifyWaiting(level, state);
        }
    }

    public static boolean active(UUID boardId) {
        return MATCHES.containsKey(boardId);
    }

    public static boolean tutorial(UUID boardId) {
        return boardId != null && TUTORIAL_BOARDS.contains(boardId);
    }

    public static int decisionDurationTicks(BoardSession session, BoardParticipant participant, int baseTicks) {
        if (tutorial(session.id()) && !participant.bot()) return TUTORIAL_DECISION_TICKS;
        return participant.decisionDurationTicks(baseTicks);
    }

    public static void clearTutorial(UUID boardId) {
        TUTORIAL_BOARDS.remove(boardId);
    }

    public static void clear(UUID boardId) {
        MATCHES.remove(boardId);
    }

    private static void startSinglePlayer(ServerPlayer player, BoardSession session) {
        MatchState existing = MATCHES.get(session.id());
        if (existing != null && (!existing.playerIds.contains(player.getUUID()) || existing.mode != BoardMatchmakingMode.SINGLE_PLAYER)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            return;
        }

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
            return;
        }
        if (state == null) {
            state = new MatchState(session.id(), player.level().dimension(), BoardMatchmakingMode.MULTIPLAYER);
            MATCHES.put(session.id(), state);
        }

        pruneWaitingPlayers(player.level(), state);
        if (state.selectionStarted || state.playerIds.size() >= BoardSessionManager.REQUIRED_PLAYERS) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            return;
        }

        state.playerIds.add(player.getUUID());
        if (state.playerIds.size() >= BoardSessionManager.REQUIRED_PLAYERS) {
            beginCharacterSelection(player.level(), session, state);
        } else {
            notifyWaiting(player.level(), state);
        }
    }

    private static void beginCharacterSelection(ServerLevel level, BoardSession session, MatchState state) {
        if (session.phase() != BoardPhase.READY || state.selectionStarted) return;
        state.selectionStarted = true;
        session.setProtectionEnabled(true);
        session.setPhase(BoardPhase.CHARACTER_SELECTION);
        session.setLobbyDeadlineTick(tutorial(session.id()) ? Long.MAX_VALUE
                : AstralServerTickClock.now(level) + BoardSessionManager.LOBBY_TIMEOUT_TICKS);
        BoardSessionManager.markChanged(level);
        BoardProtectionService.refreshProtectedAreas(level, BoardSavedData.get(level));
        for (UUID playerId : state.playerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null && player.level() == level) BoardLobbyService.registerViewer(player, session);
        }
    }

    private static void notifyWaiting(ServerLevel level, MatchState state) {
        pruneWaitingPlayers(level, state);
        Component message = Component.translatable("message.astral_craft.board.matchmaking.waiting",
                state.playerIds.size(), BoardSessionManager.REQUIRED_PLAYERS);
        for (UUID playerId : state.playerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) player.sendSystemMessage(message, true);
        }
    }

    private static void pruneWaitingPlayers(ServerLevel level, MatchState state) {
        if (state.selectionStarted) return;
        state.playerIds.removeIf(playerId -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            return player == null || player.level() != level;
        });
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
        private boolean selectionStarted;

        private MatchState(UUID boardId, ResourceKey<Level> dimension, BoardMatchmakingMode mode) {
            this.boardId = boardId;
            this.dimension = dimension;
            this.mode = mode;
        }

    }

}