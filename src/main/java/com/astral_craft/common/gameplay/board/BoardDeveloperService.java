package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.BoardDeveloperConfigPayload;
import com.astral_craft.common.network.s2c.OpenBoardDeveloperPayload;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Temporary TeaCon/development setup flow for deterministic solo board recordings and tests. */
public class BoardDeveloperService {

    private static final Map<UUID, UUID> OWNERS = new HashMap<>();
    private static final Map<UUID, Map<UUID, List<Identifier>>> INITIAL_HANDS = new HashMap<>();

    public static void begin(ServerPlayer player, BoardSession session) {
        if (player == null || session == null) return;
        OWNERS.put(session.id(), player.getUUID());
        INITIAL_HANDS.remove(session.id());
    }

    public static boolean active(UUID boardId) {
        return boardId != null && OWNERS.containsKey(boardId);
    }

    public static boolean ownedBy(UUID boardId, UUID playerId) {
        return boardId != null && playerId != null && playerId.equals(OWNERS.get(boardId));
    }

    public static boolean ownerOnline(ServerLevel level, UUID boardId) {
        UUID owner = boardId == null ? null : OWNERS.get(boardId);
        return level != null && owner != null && level.getServer().getPlayerList().getPlayer(owner) != null;
    }

    public static boolean openConfiguration(ServerPlayer player, BoardSession session) {
        if (!ownedBy(session.id(), player.getUUID()) || session.phase() != BoardPhase.CHARACTER_SELECTION) return false;
        List<CharacterDefinition> characters = CharacterManager.INSTANCE.values().stream()
                .filter(definition -> !session.hasCharacter(definition.id()))
                .filter(definition -> CharacterManager.INSTANCE.character(definition.id()).botSelectable()).toList();
        PacketDistributor.sendToPlayer(player, new OpenBoardDeveloperPayload(session.id(), characters,
                developerCardIds()));
        return true;
    }

    public static void apply(ServerPlayer player, BoardDeveloperConfigPayload payload) {
        BoardSession session = BoardSessionManager.session(player.level(), payload.boardId()).orElse(null);
        if (session == null || session.phase() != BoardPhase.CHARACTER_SELECTION
                || !ownedBy(session.id(), player.getUUID())) return;
        if (payload.bots().isEmpty()) {
            BoardSessionManager.resetForLobby(player.level(), session);
            return;
        }

        if (payload.bots().size() != BoardSessionManager.REQUIRED_PLAYERS - 1) return;
        BoardParticipant human = session.participantByController(player.getUUID()).orElse(null);
        if (human == null || session.humanCount() != 1) return;
        Set<Identifier> allowedCards = new HashSet<>(developerCardIds());
        Set<Identifier> usedCharacters = new HashSet<>();
        usedCharacters.add(human.characterId());
        List<ConfiguredBot> configured = new ArrayList<>();
        for (BoardDeveloperConfigPayload.BotSetup setup : payload.bots()) {
            if (!CharacterManager.INSTANCE.contains(setup.characterId()) || !usedCharacters.add(setup.characterId())) return;
            if (!CharacterManager.INSTANCE.character(setup.characterId()).botSelectable()) return;
            CharacterDefinition definition = CharacterManager.INSTANCE.get(setup.characterId());
            CharacterSkinDefinition skin = definition.skinOrDefault(setup.skinId().getPath());
            Identifier skinId = BoardParticipant.skinIdentifier(definition.id(), skin.id());
            if (!skinId.equals(setup.skinId())) return;
            List<Identifier> hand = expandHand(setup.cards(), allowedCards);
            if (hand == null) return;
            configured.add(new ConfiguredBot(definition.id(), skinId, hand));
        }

        Map<UUID, List<Identifier>> initialHands = new LinkedHashMap<>();
        for (ConfiguredBot bot : configured) {
            BoardParticipant participant = new BoardParticipant(UUID.randomUUID(), null, true,
                    bot.characterId(), bot.skinId(), BoardParticipant.EMPTY_NODE_ID, BoardParticipant.EMPTY_NODE_ID,
                    null, AstralPlayerStats.DEFAULT, bot.hand(), Map.of(), 0, 0, 0,
                    Math.max(7, bot.hand().size()), session.nextArrivalOrder());
            session.putParticipant(participant);
            initialHands.put(participant.slotUuid(), bot.hand());
        }

        INITIAL_HANDS.put(session.id(), Map.copyOf(initialHands));
        BoardSessionManager.markChanged(player.level());
        BoardSessionManager.startGame(player.level(), session);
    }

    public static List<Identifier> initialHand(UUID boardId, UUID slotId) {
        Map<UUID, List<Identifier>> hands = INITIAL_HANDS.get(boardId);
        return hands == null ? null : hands.get(slotId);
    }

    public static void clear(UUID boardId) {
        if (boardId == null) return;
        OWNERS.remove(boardId);
        INITIAL_HANDS.remove(boardId);
    }

    private static List<Identifier> expandHand(List<BoardDeveloperConfigPayload.CardCount> entries, Set<Identifier> allowedCards) {
        List<Identifier> hand = new ArrayList<>();
        int total = 0;
        for (BoardDeveloperConfigPayload.CardCount entry : entries) {
            if (entry.count() <= 0 || !allowedCards.contains(entry.cardId())) return null;
            total += entry.count();
            if (total > BoardParticipant.MAX_SUPPORTED_HAND_SIZE) return null;
            for (int index = 0; index < entry.count(); index++) hand.add(entry.cardId());
        }
        return List.copyOf(hand);
    }

    private static List<Identifier> developerCardIds() {
        List<Identifier> candidates = new ArrayList<>();
        for (AstralItems.ModelledCardItem entry : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = entry.item().get();
            if (item instanceof BaseHandCard) candidates.add(BuiltInRegistries.ITEM.getKey(item));
        }
        return List.copyOf(candidates);
    }

    private record ConfiguredBot(Identifier characterId, Identifier skinId, List<Identifier> hand) {}

}