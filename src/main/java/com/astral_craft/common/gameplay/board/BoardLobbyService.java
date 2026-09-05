package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgress;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.s2c.BoardCharacterAvailability;
import com.astral_craft.common.network.s2c.BoardCharacterSelectionEntry;
import com.astral_craft.common.network.s2c.OpenBoardCharacterSelectionPayload;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Character-selection lobby viewers, live previews and server-authoritative unlock/selection locking.
 */
public class BoardLobbyService {

    private static final Map<UUID, Set<UUID>> VIEWERS = new HashMap<>();
    private static final Map<UUID, LobbyState> LOBBIES = new HashMap<>();

    public static void updateSelection(ServerPlayer player, UUID boardId, Identifier characterId, Identifier skinId, boolean confirmed) {
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (session == null || session.phase() != BoardPhase.CHARACTER_SELECTION || !CharacterManager.INSTANCE.contains(characterId)) return;
        if (!BoardMatchmakingService.canSelectCharacter(player, session)) return;
        BoardParticipant existing = session.participantByController(player.getUUID()).orElse(null);
        if (existing != null) {
            sendSelection(player, session, true);
            return;
        }

        CharacterProgress progress = CharacterProgressManager.progress(player);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        if (!isCharacterUnlocked(progress, definition)) {
            sendSelection(player, session, true);
            return;
        }

        boolean occupied = session.participants().stream().anyMatch(participant -> participant.characterId().equals(characterId));
        if (occupied) {
            if (confirmed) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.character_taken"), true);
            }

            sendSelection(player, session, true);
            return;
        }

        CharacterSkinDefinition selectedSkin = definition.skinOrDefault(skinId.getPath());
        if (!isSkinUnlocked(progress, definition, selectedSkin)) selectedSkin = preferredSkin(progress, definition);
        Identifier safeSkin = BoardParticipant.skinIdentifier(characterId, selectedSkin.id());
        LobbyState lobby = lobby(session.id());
        LobbySelection selection = lobby.selection(player, characterId, safeSkin);
        lobby.put(selection.withChoice(characterId, safeSkin, true, confirmed));
        if (!confirmed) {
            refreshScreens(player.level(), session);
            return;
        }

        BoardParticipant participant = createParticipant(session, player, characterId, safeSkin);
        session.putParticipant(participant);
        session.setLobbyDeadlineTick(Math.max(session.lobbyDeadlineTick(), AstralServerTickClock.now(player.level()) + 20L));
        BoardSessionManager.markChanged(player.level());
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.character_confirmed",
                Component.translatable(definition.getDescriptionId())).withStyle(ChatFormatting.GREEN), true);
        if (BoardMatchmakingService.handleConfirmedSelection(player.level(), session)) return;
        if (readyToStartImmediately(player, session)) {
            BoardSessionManager.startGame(player.level(), session);
        } else {
            refreshScreens(player.level(), session);
        }
    }

    public static void registerViewer(ServerPlayer player, BoardSession session) {
        VIEWERS.computeIfAbsent(session.id(), ignored -> new LinkedHashSet<>()).add(player.getUUID());
        LobbyState lobby = lobby(session.id());
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        CharacterProgress progress = CharacterProgressManager.progress(player);
        CharacterDefinition fallback = preferredCharacter(progress, session);
        Identifier fallbackSkin = BoardParticipant.skinIdentifier(fallback.id(), preferredSkin(progress, fallback).id());
        LobbySelection selection = lobby.selection(player, participant == null ? fallback.id() : participant.characterId(),
                participant == null ? fallbackSkin : participant.skinId());
        if (participant != null) {
            lobby.put(selection.withChoice(participant.characterId(), participant.skinId(), true, true));
        }

        refreshScreens(player.level(), session);
    }

    public static void finalizeTimedOutSelections(ServerLevel level, BoardSession session) {
        LobbyState lobby = LOBBIES.get(session.id());
        if (lobby == null) return;
        boolean changed = false;
        for (LobbySelection selection : lobby.ordered()) {
            if (session.participantByController(selection.playerId()).isPresent()) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(selection.playerId());
            if (player == null || player.level() != level) continue;
            CharacterProgress progress = CharacterProgressManager.progress(player);
            CharacterDefinition selectedDefinition = CharacterManager.INSTANCE.get(selection.characterId());
            boolean selectedAllowed = selection.selected() && isCharacterUnlocked(progress, selectedDefinition) && !session.hasCharacter(selection.characterId());
            Identifier characterId = selectedAllowed ? selection.characterId() : firstAvailableCharacter(session, progress);
            if (characterId == null) break;
            CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
            CharacterSkinDefinition skin = selectedAllowed ? definition.skinOrDefault(selection.skinId().getPath()) : preferredSkin(progress, definition);
            if (!isSkinUnlocked(progress, definition, skin)) skin = preferredSkin(progress, definition);
            Identifier safeSkin = BoardParticipant.skinIdentifier(characterId, skin.id());
            session.putParticipant(createParticipant(session, player, characterId, safeSkin));
            lobby.put(selection.withChoice(characterId, safeSkin, true, true));
            changed = true;
        }

        if (changed) BoardSessionManager.markChanged(level);
    }

    public static void sendSelection(ServerPlayer player, BoardSession session, boolean refresh) {
        LobbyState lobby = lobby(session.id());
        CharacterProgress progress = CharacterProgressManager.progress(player);
        CharacterDefinition fallback = preferredCharacter(progress, session);
        Identifier fallbackSkin = BoardParticipant.skinIdentifier(fallback.id(), preferredSkin(progress, fallback).id());
        LobbySelection own = lobby.selection(player, fallback.id(), fallbackSkin);
        BoardParticipant selected = session.participantByController(player.getUUID()).orElse(null);
        if (selected != null && !own.confirmed()) {
            own = own.withChoice(selected.characterId(), selected.skinId(), true, true);
            lobby.put(own);
        }

        List<CharacterDefinition> definitions = CharacterManager.INSTANCE.values();
        List<BoardCharacterAvailability> availability = definitions.stream()
                .map(definition -> availability(progress, definition)).toList();
        List<Identifier> occupiedCharacters = session.participants().stream().map(BoardParticipant::characterId).toList();
        int timeoutDuration = BoardMatchmakingService.selectionTimeoutDuration(session);
        int remaining = (int) Math.clamp(session.lobbyDeadlineTick() - AstralServerTickClock.now(player.level()), 0L, timeoutDuration);
        PacketDistributor.sendToPlayer(player, new OpenBoardCharacterSelectionPayload(session.id(), definitions,
                new ArrayList<>(occupiedCharacters), lobby.entries(player.level(), session), availability,
                own.selected() ? own.characterId() : fallback.id(), own.selected() ? own.skinId() : fallbackSkin,
                remaining, timeoutDuration, selected != null, refresh));
    }

    public static void refreshScreens(ServerLevel level, BoardSession session) {
        Set<UUID> viewers = VIEWERS.get(session.id());
        if (viewers == null || viewers.isEmpty()) return;
        LobbyState lobby = lobby(session.id());
        viewers.removeIf(viewerId -> {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer == null || viewer.level() != level) {
                lobby.remove(viewerId);
                return true;
            }
            return false;
        });

        for (UUID viewerId : viewers) {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) sendSelection(viewer, session, true);
        }

        if (viewers.isEmpty()) {
            VIEWERS.remove(session.id());
            LOBBIES.remove(session.id());
        }
    }

    public static List<BoardParticipant> orderedParticipants(BoardSession session) {
        LobbyState lobby = LOBBIES.get(session.id());
        if (lobby == null) {
            return session.participants().stream().sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
        }

        Map<Integer, BoardParticipant> bySlot = new HashMap<>();
        Set<UUID> assigned = new LinkedHashSet<>();
        for (LobbySelection selection : lobby.ordered()) {
            BoardParticipant participant = session.participantByController(selection.playerId()).orElse(null);
            if (participant == null || !assigned.add(participant.slotUuid())) continue;
            bySlot.put(selection.slot(), participant);
        }

        List<BoardParticipant> remaining = session.participants().stream()
                .filter(participant -> !assigned.contains(participant.slotUuid()))
                .sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
        int cursor = 0;
        for (int slot = 0; slot < BoardSessionManager.REQUIRED_PLAYERS && cursor < remaining.size(); slot++) {
            if (!bySlot.containsKey(slot)) bySlot.put(slot, remaining.get(cursor++));
        }

        List<BoardParticipant> ordered = new ArrayList<>();
        for (int slot = 0; slot < BoardSessionManager.REQUIRED_PLAYERS; slot++) {
            BoardParticipant participant = bySlot.get(slot);
            if (participant != null) ordered.add(participant);
        }

        return List.copyOf(ordered);
    }

    public static void closeScreens(ServerLevel level, UUID boardId) {
        Set<UUID> viewers = VIEWERS.remove(boardId);
        LOBBIES.remove(boardId);
        if (viewers == null) return;
        CharacterDefinition fallback = CharacterManager.INSTANCE.defaultCharacter();
        Identifier fallbackSkin = BoardParticipant.skinIdentifier(fallback.id(), fallback.skinOrDefault("default").id());
        OpenBoardCharacterSelectionPayload closePayload = new OpenBoardCharacterSelectionPayload(boardId,
                List.of(), List.of(), List.of(), List.of(), fallback.id(), fallbackSkin, 0,
                BoardSessionManager.LOBBY_TIMEOUT_TICKS, false, false);
        for (UUID viewerId : viewers) {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) PacketDistributor.sendToPlayer(viewer, closePayload);
        }
    }

    public static void clear(UUID boardId) {
        VIEWERS.remove(boardId);
        LOBBIES.remove(boardId);
    }

    private static BoardParticipant createParticipant(BoardSession session, ServerPlayer player, Identifier characterId, Identifier skinId) {
        return new BoardParticipant(UUID.randomUUID(), player.getUUID(), false,
                characterId, skinId, BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID,
                null, AstralPlayerStats.DEFAULT, List.of(), Map.of(), 0, 0, 0, 7,
                session.nextArrivalOrder());
    }

    private static Identifier firstAvailableCharacter(BoardSession session, CharacterProgress progress) {
        return CharacterManager.INSTANCE.values().stream().filter(definition -> isCharacterUnlocked(progress, definition))
                .map(CharacterDefinition::id).filter(id -> !session.hasCharacter(id)).findFirst().orElse(null);
    }

    private static CharacterDefinition preferredCharacter(CharacterProgress progress, BoardSession session) {
        Identifier selected = progress.selectedCharacter();
        CharacterDefinition preferred = CharacterManager.INSTANCE.get(selected);
        if (isCharacterUnlocked(progress, preferred) && !session.hasCharacter(preferred.id())) return preferred;
        return CharacterManager.INSTANCE.values().stream().filter(definition -> isCharacterUnlocked(progress, definition))
                .filter(definition -> !session.hasCharacter(definition.id())).findFirst()
                .orElse(CharacterManager.INSTANCE.defaultCharacter());
    }

    private static BoardCharacterAvailability availability(CharacterProgress progress, CharacterDefinition definition) {
        CharacterSkinDefinition preferred = preferredSkin(progress, definition);
        List<Identifier> skins = definition.skins().stream().filter(skin -> isSkinUnlocked(progress, definition, skin))
                .map(skin -> BoardParticipant.skinIdentifier(definition.id(), skin.id())).toList();
        return new BoardCharacterAvailability(definition.id(),
                BoardParticipant.skinIdentifier(definition.id(), preferred.id()),
                isCharacterUnlocked(progress, definition), skins);
    }

    private static CharacterSkinDefinition preferredSkin(CharacterProgress progress, CharacterDefinition definition) {
        CharacterProgressEntry entry = progress.entry(definition.id());
        CharacterSkinDefinition preferred = definition.skinOrDefault(entry.selectedSkin());
        if (isSkinUnlocked(progress, definition, preferred)) return preferred;
        return definition.skins().stream().filter(skin -> isSkinUnlocked(progress, definition, skin))
                .findFirst().orElse(definition.skinOrDefault("default"));
    }

    private static boolean isCharacterUnlocked(CharacterProgress progress, CharacterDefinition definition) {
        return definition.unlockedByDefault() || progress.isCharacterUnlocked(definition.id());
    }

    private static boolean isSkinUnlocked(CharacterProgress progress, CharacterDefinition definition, CharacterSkinDefinition skin) {
        if (skin == null) return false;
        boolean defaultSkin = "default".equals(skin.id()) || !definition.skins().isEmpty() && definition.skins().getFirst().id().equals(skin.id());
        return defaultSkin || skin.unlockedByDefault() || progress.isSkinUnlocked(definition.id(), skin.id());
    }

    private static LobbyState lobby(UUID boardId) {
        return LOBBIES.computeIfAbsent(boardId, ignored -> new LobbyState());
    }

    private static boolean readyToStartImmediately(ServerPlayer player, BoardSession session) {
        Set<UUID> viewers = VIEWERS.getOrDefault(session.id(), Set.of());
        long onlineViewers = viewers.stream().map(player.server.getPlayerList()::getPlayer)
                .filter(Objects::nonNull).filter(viewer -> viewer.level() == player.level()).count();
        int requiredHumans = (int) Math.clamp(Math.max(1L, onlineViewers), 1L, BoardSessionManager.REQUIRED_PLAYERS);
        return session.participantCount() >= requiredHumans;
    }

    private static class LobbyState {

        private final Map<UUID, LobbySelection> selections = new HashMap<>();

        private LobbySelection selection(ServerPlayer player, Identifier fallbackCharacter, Identifier fallbackSkin) {
            LobbySelection existing = this.selections.get(player.getUUID());
            if (existing != null) return existing;
            Set<Integer> used = this.selections.values().stream().map(LobbySelection::slot).collect(Collectors.toSet());
            List<Integer> available = new ArrayList<>();
            for (int slot = 0; slot < BoardSessionManager.REQUIRED_PLAYERS; slot++)
                if (!used.contains(slot)) available.add(slot);
            int slot = available.isEmpty() ? this.selections.size() % BoardSessionManager.REQUIRED_PLAYERS
                    : available.get(player.getRandom().nextInt(available.size()));
            LobbySelection created = new LobbySelection(player.getUUID(), slot, player.getScoreboardName(),
                    fallbackCharacter, fallbackSkin, false, false);
            this.selections.put(player.getUUID(), created);
            return created;
        }

        private void put(LobbySelection selection) {
            this.selections.put(selection.playerId(), selection);
        }

        private void remove(UUID playerId) {
            this.selections.remove(playerId);
        }

        private List<LobbySelection> ordered() {
            return this.selections.values().stream().sorted(Comparator.comparingInt(LobbySelection::slot)).toList();
        }

        private List<BoardCharacterSelectionEntry> entries(ServerLevel level, BoardSession session) {
            List<BoardCharacterSelectionEntry> entries = new ArrayList<>(this.ordered().stream()
                    .map(selection -> new BoardCharacterSelectionEntry(selection.slot(), selection.playerName(),
                            selection.characterId(), selection.skinId(), selection.selected(), selection.confirmed()))
                    .toList());
            Set<Integer> usedSlots = entries.stream().map(BoardCharacterSelectionEntry::slot).collect(Collectors.toSet());
            List<BoardParticipant> bots = session.participants().stream().filter(BoardParticipant::bot)
                    .filter(participant -> !participant.monster()).sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
            int botIndex = 0;
            for (int slot = 0; slot < BoardSessionManager.REQUIRED_PLAYERS && botIndex < bots.size(); slot++) {
                if (usedSlots.contains(slot)) continue;
                BoardParticipant bot = bots.get(botIndex++);
                entries.add(new BoardCharacterSelectionEntry(slot, BoardSessionManager.displayName(level, bot),
                        bot.characterId(), bot.skinId(), true, true));
            }

            return entries.stream().sorted(Comparator.comparingInt(BoardCharacterSelectionEntry::slot)).toList();
        }

    }

    private record LobbySelection(UUID playerId, int slot, String playerName, Identifier characterId, Identifier skinId, boolean selected, boolean confirmed) {

        private LobbySelection withChoice(Identifier characterId, Identifier skinId, boolean selected, boolean confirmed) {
            return new LobbySelection(this.playerId, this.slot, this.playerName, characterId, skinId, selected, confirmed);
        }

    }

}