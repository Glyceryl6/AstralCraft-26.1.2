package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipSelectionService;
import com.astral_craft.common.network.s2c.CloseBoardPresentationPayload;
import com.astral_craft.common.network.s2c.OpenBoardRelicShopPayload;
import com.astral_craft.common.network.s2c.OpenChipSelectionPayload;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class RelicPlatform extends BasePlatform {

    public static final int TIMEOUT_TICKS = 20 * 25;
    private final Map<UUID, RelicState> states = new HashMap<>();

    public RelicPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public Component boardActionPrompt(Component actorName) {
        return Component.translatable("message.astral_craft.board.prompt.relic", actorName);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        this.open(context);
    }

    public static void handleAction(ServerPlayer player, UUID boardId, boolean buy) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> active(session)
                .ifPresent(platform -> platform.handleActionInternal(player, session, buy)));
    }

    @Override
    public void handleBoardChipSelection(ServerPlayer player, BoardSession session, Identifier chipId) {
        this.chooseInternal(player, session, chipId);
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        RelicState state = this.states.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }
        if (AstralServerTickClock.now(level) < state.deadlineTick()) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null) {
            this.finish(level, session);
            return;
        }
        if (!BoardSessionManager.isAutomated(level, participant)) participant = participant.recordTimedOutDecision();
        if (state.stage() == Stage.SELECTING && !state.offers().isEmpty()) {
            Identifier choice = state.offers().get(level.getRandom().nextInt(state.offers().size()));
            participant = ChipSelectionService.applyBoardChoice(participant, state.offers(), choice, true);
        }
        BoardSessionManager.updateParticipant(level, session, participant);
        this.finish(level, session);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        RelicState state = this.states.get(session.id());
        if (state == null || !state.slotId().equals(slotId)) return;
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null) {
            this.finish(level, session);
            return;
        }
        if (state.stage() == Stage.SELECTING && !state.offers().isEmpty()) {
            Identifier choice = state.offers().get(level.getRandom().nextInt(state.offers().size()));
            BoardSessionManager.updateParticipant(level, session,
                    ChipSelectionService.applyBoardChoice(participant, state.offers(), choice, true));
        } else {
            this.automatedPurchase(level, session, participant);
        }
        this.finish(level, session);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.states.remove(boardId);
    }

    private void open(BoardPanelContext context) {
        ServerLevel level = context.level();
        BoardSession session = context.session();
        BoardParticipant participant = context.participant();
        if (this.states.containsKey(session.id())) return;
        if (BoardSessionManager.isAutomated(level, participant)) {
            this.automatedPurchase(level, session, participant);
            return;
        }
        int duration = participant.decisionDurationTicks(TIMEOUT_TICKS);
        RelicState state = new RelicState(session.id(), participant.slotUuid(), Stage.OFFER, List.of(),
                AstralServerTickClock.now(level) + duration, duration);
        this.states.put(session.id(), state);
        this.activateBoardEffect(session);
        participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                .ifPresent(player -> this.sendShop(player, participant, state, 0));
    }

    private void handleActionInternal(ServerPlayer player, BoardSession session, boolean buy) {
        RelicState state = this.states.get(session.id());
        if (state == null || state.stage() != Stage.OFFER) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        participant = participant.recordManualDecision();
        BoardSessionManager.updateParticipant(player.level(), session, participant);
        if (!buy) {
            this.finish(player.level(), session);
            return;
        }
        int price = participant.chipProgress().shopPrice();
        if (participant.stats().starCoins() < price) {
            this.sendShop(player, participant, state, 1);
            return;
        }
        List<ChipDefinition> choices = ChipSelectionService.rollBoardChoices(player.level().getRandom(), participant,
                this.normalDifficulty(), null);
        if (choices.isEmpty()) {
            this.sendShop(player, participant, state, 2);
            return;
        }
        BoardParticipant paid = participant.withStats(participant.stats().spendCoins(price));
        BoardSessionManager.updateParticipant(player.level(), session, paid);
        int duration = paid.decisionDurationTicks(TIMEOUT_TICKS);
        List<Identifier> offers = choices.stream().map(ChipDefinition::registryId).toList();
        RelicState selecting = new RelicState(session.id(), paid.slotUuid(), Stage.SELECTING, offers,
                AstralServerTickClock.now(player.level()) + duration, duration);
        this.states.put(session.id(), selecting);
        PacketDistributor.sendToPlayer(player, new OpenChipSelectionPayload(session.id(),
                ChipSelectionService.toViews(choices), duration, duration));
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, Identifier chipId) {
        RelicState state = this.states.get(session.id());
        if (state == null || state.stage() != Stage.SELECTING || !state.offers().contains(chipId)) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        BoardParticipant updated = ChipSelectionService.applyBoardChoice(participant.recordManualDecision(),
                state.offers(), chipId, true);
        BoardSessionManager.updateParticipant(player.level(), session, updated);
        this.finish(player.level(), session);
    }

    private void automatedPurchase(ServerLevel level, BoardSession session, BoardParticipant participant) {
        int price = participant.chipProgress().shopPrice();
        if (participant.stats().starCoins() < price) return;
        List<ChipDefinition> choices = ChipSelectionService.rollBoardChoices(level.getRandom(), participant,
                this.normalDifficulty(), null);
        if (choices.isEmpty()) return;
        List<Identifier> offers = choices.stream().map(ChipDefinition::registryId).toList();
        Identifier choice = offers.get(level.getRandom().nextInt(offers.size()));
        BoardParticipant paid = participant.withStats(participant.stats().spendCoins(price));
        BoardSessionManager.updateParticipant(level, session,
                ChipSelectionService.applyBoardChoice(paid, offers, choice, true));
    }

    private boolean normalDifficulty() {
        return true;
    }

    private void sendShop(ServerPlayer player, BoardParticipant participant, RelicState state, int noticeCode) {
        int remaining = (int) Math.max(0L, state.deadlineTick() - AstralServerTickClock.now(player.level()));
        PacketDistributor.sendToPlayer(player, new OpenBoardRelicShopPayload(state.boardId(),
                participant.chipProgress().shopPrice(), participant.stats().starCoins(), remaining,
                state.durationTicks(), participant.characterId(), participant.skinId(), noticeCode));
    }

    private void finish(ServerLevel level, BoardSession session) {
        this.states.remove(session.id());
        this.deactivateBoardEffect(session.id());
        for (ServerPlayer player : BoardSessionManager.humanPlayers(level, session)) {
            PacketDistributor.sendToPlayer(player, new CloseBoardPresentationPayload(session.id()));
        }
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private static Optional<RelicPlatform> active(BoardSession session) {
        return activeBoardEffect(session.id()).filter(RelicPlatform.class::isInstance).map(RelicPlatform.class::cast);
    }

    private enum Stage { OFFER, SELECTING }

    private record RelicState(UUID boardId, UUID slotId, Stage stage, List<Identifier> offers,
                              long deadlineTick, int durationTicks) {
        private RelicState {
            offers = List.copyOf(offers);
            durationTicks = Math.max(1, durationTicks);
        }
    }

}
