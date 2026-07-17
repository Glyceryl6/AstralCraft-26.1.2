package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.network.s2c.OpenBoardCharacterSelectionPayload;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Character-selection lobby viewers and server-authoritative selection locking. */
public class BoardLobbyService {

    private static final Map<UUID, Set<UUID>> VIEWERS = new HashMap<>();

    public static void selectCharacter(ServerPlayer player, String rawBoardId, Identifier characterId, Identifier skinId) {
        BoardSession session = parseSession(player.level(), rawBoardId);
        if (session == null || session.phase() != BoardPhase.CHARACTER_SELECTION) return;
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        Optional<BoardParticipant> existing = session.participantByController(player.getUUID());
        if (existing.isPresent()) {
            sendSelection(player, session, true);
            return;
        }
        boolean occupied = session.participants().stream()
                .anyMatch(participant -> participant.characterId().equals(characterId));
        if (occupied) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.character_taken"), true);
            sendSelection(player, session, true);
            return;
        }

        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        String safeSkin = definition.skinOrDefault(skinId.getPath()).id();
        UUID slotId = UUID.randomUUID();
        BoardParticipant participant = new BoardParticipant(slotId, Optional.of(player.getUUID()), false,
                characterId, BoardParticipant.skinIdentifier(characterId, safeSkin), BoardParticipant.EMPTY_NODE_ID,
                BoardParticipant.EMPTY_NODE_ID, Optional.empty(), AstralPlayerStats.DEFAULT, List.of(), Map.of(),
                0, 0, 0, 7, session.nextArrivalOrder());
        session.putParticipant(participant);
        session.setLobbyDeadlineTick(Math.max(session.lobbyDeadlineTick(), player.level().getGameTime() + 20L));
        BoardSessionManager.markChanged(player.level());
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.character_confirmed",
                Component.translatable(definition.nameKey())).withStyle(ChatFormatting.GREEN), true);
        if (session.participantCount() >= BoardSessionManager.REQUIRED_PLAYERS || isSoloIntegratedServer(player)) {
            BoardSessionManager.startGame(player.level(), session);
        } else {
            refreshScreens(player.level(), session);
        }
    }

    public static void registerViewer(ServerPlayer player, BoardSession session) {
        VIEWERS.computeIfAbsent(session.id(), ignored -> new LinkedHashSet<>()).add(player.getUUID());
        sendSelection(player, session, false);
    }

    public static void sendSelection(ServerPlayer player, BoardSession session, boolean refresh) {
        BoardParticipant selected = session.participantByController(player.getUUID()).orElse(null);
        List<Identifier> occupiedCharacters = session.participants().stream().map(BoardParticipant::characterId).toList();
        CharacterDefinition fallback = CharacterManager.INSTANCE.defaultCharacter();
        String fallbackSkin = fallback.skins().isEmpty() ? "default" : fallback.skins().getFirst().id();
        int remaining = (int) Math.clamp(session.lobbyDeadlineTick() - player.level().getGameTime(), 0L,
                BoardSessionManager.LOBBY_TIMEOUT_TICKS);
        PacketDistributor.sendToPlayer(player, new OpenBoardCharacterSelectionPayload(session.id().toString(),
                CharacterManager.INSTANCE.encodeList(), new ArrayList<>(occupiedCharacters),
                selected == null ? fallback.id() : selected.characterId(),
                selected == null ? BoardParticipant.skinIdentifier(fallback.id(), fallbackSkin) : selected.skinId(),
                remaining, BoardSessionManager.LOBBY_TIMEOUT_TICKS, selected != null, refresh));
    }

    public static void refreshScreens(ServerLevel level, BoardSession session) {
        Set<UUID> viewers = VIEWERS.get(session.id());
        if (viewers == null || viewers.isEmpty()) return;
        viewers.removeIf(viewerId -> {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer == null || viewer.level() != level) return true;
            sendSelection(viewer, session, true);
            return false;
        });
        if (viewers.isEmpty()) VIEWERS.remove(session.id());
    }

    public static void closeScreens(ServerLevel level, UUID boardId) {
        Set<UUID> viewers = VIEWERS.remove(boardId);
        if (viewers == null) return;
        CharacterDefinition fallback = CharacterManager.INSTANCE.defaultCharacter();
        String fallbackSkin = fallback.skins().isEmpty() ? "default" : fallback.skins().getFirst().id();
        OpenBoardCharacterSelectionPayload closePayload = new OpenBoardCharacterSelectionPayload(boardId.toString(),
                "", List.of(), fallback.id(), BoardParticipant.skinIdentifier(fallback.id(), fallbackSkin), 0,
                BoardSessionManager.LOBBY_TIMEOUT_TICKS, false, false);
        for (UUID viewerId : viewers) {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) PacketDistributor.sendToPlayer(viewer, closePayload);
        }
    }

    public static void clear(UUID boardId) {
        VIEWERS.remove(boardId);
    }

    private static BoardSession parseSession(ServerLevel level, String rawBoardId) {
        try {
            return BoardSessionManager.session(level, UUID.fromString(rawBoardId)).orElse(null);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isSoloIntegratedServer(ServerPlayer player) {
        return player.server.isSingleplayer() && player.server.getPlayerList().getPlayerCount() <= 1;
    }

}