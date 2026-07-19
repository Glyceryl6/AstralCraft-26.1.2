package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.BoardPanelEdgeView;
import com.astral_craft.common.network.BoardPanelNodeView;
import com.astral_craft.common.network.BoardPanelOccupantView;
import com.astral_craft.common.network.c2s.BoardPanelSelectionPayload;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.network.s2c.OpenBoardPanelSelectionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class BoardPanelSelectionService {

    private static final Map<UUID, PendingSelection> PENDING = new HashMap<>();

    public static boolean hasPending(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    public static boolean hasValidNode(ServerPlayer player, int range) {
        BoardSession session = BoardSessionManager.findByController(player).orElse(null);
        BoardParticipant source = session == null ? null : session.participantByController(player.getUUID()).orElse(null);
        return session != null && source != null && !validNodes(session, source, range).isEmpty();
    }

    public static boolean begin(ServerPlayer player, ItemStack cardStack, int handIndex, BoardPanelPlacementCard placementCard) {
        BoardSession session = BoardSessionManager.findByController(player).orElse(null);
        BoardParticipant source = session == null ? null : session.participantByController(player.getUUID()).orElse(null);
        if (session == null || source == null || !CardUseService.isBoardHandIndex(handIndex)) return false;
        List<String> validNodes = validNodes(session, source, placementCard.boardPlacementRange());
        if (validNodes.isEmpty()) return false;
        PENDING.put(player.getUUID(), new PendingSelection(session.id(), cardStack.copyWithCount(1), handIndex,
                placementCard.boardTrapType(), placementCard.boardPlacementRange(), placementCard.revealWhenPlaced()));
        PacketDistributor.sendToPlayer(player, createPayload(session, cardStack, handIndex, validNodes));
        return true;
    }

    public static void submit(ServerPlayer player, BoardPanelSelectionPayload payload) {
        PendingSelection pending = PENDING.remove(player.getUUID());
        if (pending == null || !pending.boardId().equals(payload.boardId())
                || pending.handIndex() != payload.handIndex()
                || !ItemStack.isSameItemSameComponents(pending.cardStack(), payload.cardStack())) return;
        BoardSession session = BoardSessionManager.session(player.level(), pending.boardId()).orElse(null);
        BoardParticipant source = session == null ? null : session.participantByController(player.getUUID()).orElse(null);
        if (session == null || source == null) return;
        if (payload.nodeId().isEmpty()) {
            reopenTurnScreen(player, source);
            return;
        }

        String nodeId = payload.nodeId().get().toString();
        if (!validNodes(session, source, pending.range()).contains(nodeId)) return;
        int cardIndex = CardUseService.boardCardIndex(pending.handIndex());
        ItemStack current = BoardSessionManager.boardCardStack(player, cardIndex);
        if (!ItemStack.isSameItemSameComponents(current, pending.cardStack())) return;
        Runnable apply = () -> {
            BoardSession refreshed = BoardSessionManager.session(player.level(), pending.boardId()).orElse(null);
            BoardParticipant refreshedSource = refreshed == null ? null
                    : refreshed.participantByController(player.getUUID()).orElse(null);
            if (refreshed == null || refreshedSource == null
                    || !validNodes(refreshed, refreshedSource, pending.range()).contains(nodeId)) return;
            BoardWorldObjectService.placeTrap(player.level(), refreshed, pending.type(), refreshedSource.slotUuid(), nodeId);
            BoardSessionManager.consumeBoardCard(player, cardIndex);
            if (pending.revealWhenPlaced()) {
                PendingCardActionManager.completeBoardCardUi(player);
            } else {
                BoardSessionManager.reopenTurnScreen(player, pending.boardId());
            }
        };

        if (pending.revealWhenPlaced() && pending.cardStack().getItem() instanceof BaseHandCard card) {
            PendingCardActionManager.beginBoardCardUi(player, pending.boardId(), false);
            var definition = card.definition(pending.cardStack());
            for (ServerPlayer viewer : BoardSessionManager.humanPlayers(player.level(), session)) {
                CardUseService.sendReveal(viewer, pending.cardStack(), player, definition,
                        CardRevealPayload.ANIMATION_FLIP, CardUseService.CARD_REVEAL_DURATION_TICKS);
            }
            if (!PendingCardActionManager.scheduleExclusive(player,
                    CardUseService.CARD_REVEAL_DURATION_TICKS + CardUseService.CARD_EFFECT_POST_REVEAL_DELAY_TICKS,
                    apply)) {
                PendingCardActionManager.completeBoardCardUi(player);
            }
        } else {
            apply.run();
        }
    }

    public static void clear(UUID boardId) {
        PENDING.entrySet().removeIf(entry -> entry.getValue().boardId().equals(boardId));
    }

    private static OpenBoardPanelSelectionPayload createPayload(BoardSession session, ItemStack stack, int handIndex, List<String> validNodes) {
        Set<String> valid = Set.copyOf(validNodes);
        List<BoardPanelNodeView> nodes = new ArrayList<>();
        for (Map.Entry<String, BlockPos> entry : session.positions().entrySet()) {
            List<BoardPanelOccupantView> occupants = session.participants().stream()
                    .filter(participant -> participant.currentNodeKey().equals(entry.getKey()))
                    .sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder))
                    .map(participant -> new BoardPanelOccupantView(participant.characterId(), participant.skinId())).toList();
            nodes.add(new BoardPanelNodeView(Identifier.parse(entry.getKey()), entry.getValue(),
                    valid.contains(entry.getKey()), occupants));
        }

        List<BoardPanelEdgeView> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (BoardNode node : session.nodes().values()) {
            for (String next : node.next()) {
                String key = node.id().compareTo(next) <= 0 ? node.id() + "|" + next : next + "|" + node.id();
                if (seen.add(key)) edges.add(new BoardPanelEdgeView(Identifier.parse(node.id()), Identifier.parse(next)));
            }
        }

        return new OpenBoardPanelSelectionPayload(session.id(), stack.copyWithCount(1), handIndex, nodes, edges);
    }

    private static List<String> validNodes(BoardSession session, BoardParticipant source, int range) {
        int maximum = Math.max(0, range);
        return session.nodes().keySet().stream().filter(nodeId -> {
                    int distance = BoardRouteService.graphDistance(session, source.currentNodeKey(), nodeId, maximum);
                    return distance >= 0 && distance <= maximum;
                }).sorted().toList();
    }

    private static void reopenTurnScreen(ServerPlayer player, BoardParticipant participant) {
        var entity = BoardEntityService.entity(player.level(), participant);
        if (entity != null) BoardSessionManager.openTurnScreen(player, entity);
    }

    private record PendingSelection(UUID boardId, ItemStack cardStack, int handIndex,
                                    BoardMechanicsState.BoardTrapType type, int range,
                                    boolean revealWhenPlaced) {}
}
