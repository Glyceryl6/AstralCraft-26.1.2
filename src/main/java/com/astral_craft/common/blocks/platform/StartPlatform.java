package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.gameplay.board.BoardTutorialPolicy;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardEntityService;
import com.astral_craft.common.gameplay.board.BoardHudSyncManager;
import com.astral_craft.common.gameplay.board.BoardMode;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipSelectionService;
import com.astral_craft.common.network.s2c.CloseBoardPresentationPayload;
import com.astral_craft.common.network.s2c.OpenBoardStartChoicePayload;
import com.astral_craft.common.network.s2c.OpenChipSelectionPayload;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StartPlatform extends BasePlatform {

    public static final int TIMEOUT_TICKS = 20 * 10;
    private static final int CHIP_TIMEOUT_TICKS = 20 * 25;
    private static final int[] STAR_COSTS = {0, 15, 30, 50};
    private final Map<UUID, StartChoiceState> choices = new HashMap<>();
    private final boolean checkpoint;

    public StartPlatform(Block.Properties properties) {
        this(properties, false);
    }

    protected StartPlatform(Block.Properties properties, boolean checkpoint) {
        super(properties, Trigger.BOTH);
        this.checkpoint = checkpoint;
    }

    @Override
    public boolean characterStart() {
        return !this.checkpoint;
    }

    @Override
    public Component boardActionPrompt(Component actorName) {
        return Component.translatable("message.astral_craft.board.prompt.stop", actorName);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        this.arrive(context);
    }

    public static void choose(ServerPlayer player, UUID boardId, boolean stop) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> active(session)
                .ifPresent(platform -> platform.chooseInternal(player, session, stop)));
    }

    @Override
    public void handleBoardChipSelection(ServerPlayer player, BoardSession session, Identifier chipId) {
        this.chooseChipInternal(player, session, chipId);
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        StartChoiceState state = this.choices.get(session.id());
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
        if (state.stage() == Stage.CHIP_SELECTION && !state.offers().isEmpty()) {
            Identifier choice = state.offers().get(level.getRandom().nextInt(state.offers().size()));
            participant = ChipSelectionService.applyBoardChoice(participant, state.offers(), choice, false);
            BoardSessionManager.updateParticipant(level, session, participant);
            this.finish(level, session);
            return;
        }
        BoardSessionManager.updateParticipant(level, session, participant);
        this.resolve(level, session, participant, false, false);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        StartChoiceState state = this.choices.get(session.id());
        if (state == null || !state.slotId().equals(slotId)) return;
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null) {
            this.finish(level, session);
            return;
        }
        if (state.stage() == Stage.CHIP_SELECTION && !state.offers().isEmpty()) {
            Identifier choice = state.offers().get(level.getRandom().nextInt(state.offers().size()));
            BoardSessionManager.updateParticipant(level, session,
                    ChipSelectionService.applyBoardChoice(participant, state.offers(), choice, false));
            this.finish(level, session);
            return;
        }
        this.resolve(level, session, participant, true, true);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.choices.remove(boardId);
    }

    private void chooseInternal(ServerPlayer player, BoardSession session, boolean stop) {
        StartChoiceState state = this.choices.get(session.id());
        if (state == null || state.stage() != Stage.STOP_CHOICE) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        participant = participant.recordManualDecision();
        BoardSessionManager.updateParticipant(player.level(), session, participant);
        this.resolve(player.level(), session, participant, stop, stop);
    }

    private void chooseChipInternal(ServerPlayer player, BoardSession session, Identifier chipId) {
        StartChoiceState state = this.choices.get(session.id());
        if (state == null || state.stage() != Stage.CHIP_SELECTION || !state.offers().contains(chipId)) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        BoardParticipant updated = ChipSelectionService.applyBoardChoice(participant.recordManualDecision(),
                state.offers(), chipId, false);
        BoardSessionManager.updateParticipant(player.level(), session, updated);
        this.finish(player.level(), session);
    }

    private void arrive(BoardPanelContext context) {
        BoardSession.MovementState movement = context.session().movement();
        if (movement == null) return;
        if (context.landing()) {
            this.resolve(context.level(), context.session(), context.participant(), true, false);
            return;
        }
        if (!this.checkpoint && !context.session().canStopAtStart(context.participant(), context.participant().currentNodeKey())) return;
        if (BoardSessionManager.isAutomated(context.level(), context.participant())) {
            this.resolve(context.level(), context.session(), context.participant(), true, true);
        } else {
            this.open(context.level(), context.session(), context.participant());
        }
    }

    private void open(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (this.choices.containsKey(session.id())) return;
        ServerPlayer player = participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (player == null) {
            this.resolve(level, session, participant, true, true);
            return;
        }
        int duration = BoardTutorialPolicy.decisionDurationTicks(session, participant, TIMEOUT_TICKS);
        this.choices.put(session.id(), new StartChoiceState(participant.slotUuid(), Stage.STOP_CHOICE, List.of(),
                AstralServerTickClock.now(level) + duration, duration));
        this.activateBoardEffect(session);
        PacketDistributor.sendToPlayer(player, new OpenBoardStartChoicePayload(session.id(),
                participant.stats().health(), participant.stats().maxHealth(), participant.stats().stars(),
                participant.stats().starCoins(), nextStarCost(participant.stats().stars()), duration, duration,
                this.checkpoint, participant.characterId(), participant.skinId()));
    }

    private void resolve(ServerLevel level, BoardSession session, BoardParticipant participant, boolean stop, boolean settleArrival) {
        BoardSession.MovementState movement = session.movement();
        if (movement == null || !movement.slotId().equals(participant.slotUuid())) {
            this.finish(level, session);
            return;
        }
        if (stop) {
            session.setMovement(movement.stop());
            if (settleArrival) {
                BoardWorldObjectService.ArrivalResult result = BoardWorldObjectService.triggerArrival(level, session, participant, true);
                participant = result.participant();
                if (!participant.knockedDown()) {
                    BoardWorldObjectService.pickupAtArrival(level, session, participant);
                    participant = session.participant(participant.slotUuid()).orElse(participant);
                }
            }
            if (!participant.knockedDown()) {
                BenefitResult result = this.applyBenefits(level, session, participant);
                if (this.checkVictory(level, session, result.participant())) return;
                if (result.leveled() && session.mode() == BoardMode.PVE
                        && this.beginChipSelection(level, session, result.participant())) return;
            }
        }
        this.finish(level, session);
    }

    private BenefitResult applyBenefits(ServerLevel level, BoardSession session, BoardParticipant participant) {
        var stats = participant.stats().heal(2);
        int starCoinThreshold = nextStarCost(stats.stars());
        boolean leveled = starCoinThreshold > 0 && stats.starCoins() >= starCoinThreshold && stats.stars() < 3;
        if (leveled) stats = session.mode() == BoardMode.PVE
                ? stats.spendCoins(starCoinThreshold).addStars(1) : stats.addStars(1);
        BoardParticipant updated = participant.withStats(stats);
        BoardSessionManager.updateParticipant(level, session, updated);
        ServerPlayer player = updated.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (leveled) {
            var entity = BoardEntityService.entity(level, updated);
            if (entity != null) level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.05F);
        }
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_healed", 2), true);
            if (leveled) player.sendSystemMessage(Component.translatable(session.mode() == BoardMode.PVE
                    ? "message.astral_craft.board.star_up_with_cost" : "message.astral_craft.board.star_up_without_cost",
                    stats.stars(), starCoinThreshold).withStyle(ChatFormatting.GOLD), true);
        }
        return new BenefitResult(updated, leveled);
    }

    private boolean beginChipSelection(ServerLevel level, BoardSession session, BoardParticipant participant) {
        List<ChipDefinition> choices = ChipSelectionService.rollBoardChoices(level.getRandom(), participant,
                true, null);
        if (choices.isEmpty()) return false;
        List<Identifier> offers = choices.stream().map(ChipDefinition::registryId).toList();
        if (BoardSessionManager.isAutomated(level, participant)) {
            Identifier choice = offers.get(level.getRandom().nextInt(offers.size()));
            BoardSessionManager.updateParticipant(level, session,
                    ChipSelectionService.applyBoardChoice(participant, offers, choice, false));
            return false;
        }
        ServerPlayer player = participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        if (player == null) return false;
        int duration = BoardTutorialPolicy.decisionDurationTicks(session, participant, CHIP_TIMEOUT_TICKS);
        this.choices.put(session.id(), new StartChoiceState(participant.slotUuid(), Stage.CHIP_SELECTION, offers,
                AstralServerTickClock.now(level) + duration, duration));
        this.activateBoardEffect(session);
        PacketDistributor.sendToPlayer(player, new OpenChipSelectionPayload(session.id(),
                ChipSelectionService.toViews(choices), duration, duration));
        return true;
    }

    private boolean checkVictory(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (participant.stats().stars() < 3) return false;
        String winner = BoardSessionManager.displayName(level, participant);
        for (ServerPlayer player : BoardSessionManager.humanPlayers(level, session)) {
            MutableComponent component = Component.translatable("message.astral_craft.board.victory", winner);
            player.sendSystemMessage(component.withStyle(ChatFormatting.GOLD), false);
        }
        var winnerEntity = BoardEntityService.entity(level, participant);
        if (winnerEntity != null) level.playSound(null, winnerEntity.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.2F, 0.95F);
        BoardHudSyncManager.announce(level, session,
                Component.translatable("message.astral_craft.board.announcement.victory",
                        Component.literal(winner).withStyle(ChatFormatting.GOLD)), Component.empty(),
                BoardHudSyncManager.VICTORY_SOUND, 80);
        this.choices.remove(session.id());
        this.deactivateBoardEffect(session.id());
        BoardSessionManager.endGame(level, session, true);
        return true;
    }

    private void finish(ServerLevel level, BoardSession session) {
        this.choices.remove(session.id());
        this.deactivateBoardEffect(session.id());
        for (ServerPlayer player : BoardSessionManager.humanPlayers(level, session)) {
            PacketDistributor.sendToPlayer(player, new CloseBoardPresentationPayload(session.id()));
        }
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private static Optional<StartPlatform> active(BoardSession session) {
        return activeBoardEffect(session.id()).filter(StartPlatform.class::isInstance).map(StartPlatform.class::cast);
    }

    public static int nextStarCost(int stars) {
        int next = Math.clamp(stars + 1, 0, STAR_COSTS.length - 1);
        return next >= 3 && stars >= 3 ? 0 : STAR_COSTS[next];
    }

    private enum Stage {
        STOP_CHOICE,
        CHIP_SELECTION
    }

    private record StartChoiceState(UUID slotId, Stage stage, List<Identifier> offers,
                                    long deadlineTick, int durationTicks) {
        private StartChoiceState {
            offers = List.copyOf(offers);
        }
    }

    private record BenefitResult(BoardParticipant participant, boolean leveled) {}
}
