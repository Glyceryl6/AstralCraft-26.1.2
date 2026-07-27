package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.event.effects.BoardSharedLotteryEventEffect;
import com.astral_craft.common.gameplay.handcard.PendingCardActionManager;
import com.astral_craft.common.network.s2c.BoardAnnouncementPayload;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload;
import com.astral_craft.common.network.s2c.BoardHudSnapshotPayload.PawnView;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Sends typed board snapshots and shared board-state prompts. */
public class BoardHudSyncManager {

    public static final Identifier ROUND_START_SOUND = Identifier.withDefaultNamespace("ui.toast.in");
    public static final Identifier VICTORY_SOUND = Identifier.withDefaultNamespace("ui.toast.challenge_complete");
    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final double HUD_RANGE_SQR = 512.0D * 512.0D;
    private static final UUID EMPTY_SLOT_ID = new UUID(0L, 0L);
    private static final ChatFormatting[] PLAYER_COLORS = {
            ChatFormatting.AQUA, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE
    };
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

        Component prompt = actionPrompt(level, session);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            viewer.sendSystemMessage(prompt, true);
        }
    }

    public static void announce(ServerLevel level, BoardSession session, Component title, Component subtitle,
                                Identifier sound, int durationTicks) {
        BoardAnnouncementPayload payload = new BoardAnnouncementPayload(title, subtitle, durationTicks, sound);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, payload);
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

    private static Component actionPrompt(ServerLevel level, BoardSession session) {
        if (session.phase() != BoardPhase.PLAYING) return Component.empty();
        if (BoardSharedLotteryEventEffect.active(session.id())) {
            return Component.translatable("message.astral_craft.board.prompt.shared_lottery", coloredNames(level, session));
        }

        BoardSession.DiscardState discard = session.discard();
        if (discard != null) {
            return session.participant(discard.slotId()).map(participant -> Component.translatable(
                    "message.astral_craft.board.prompt.discard", coloredName(level, session, participant))).orElse(Component.empty());
        }

        BoardSession.MovementState movement = session.movement();
        if (movement != null && !movement.branchChoices().isEmpty()) {
            return session.participant(movement.slotId()).map(participant -> Component.translatable(
                    "message.astral_craft.board.prompt.direction", coloredName(level, session, participant))).orElse(Component.empty());
        }

        BasePlatform platform = BasePlatform.activeBoardEffect(session.id()).orElse(null);
        BoardParticipant current = session.currentParticipant().orElse(null);
        if (platform != null && current != null) {
            Component prompt = platform.boardActionPrompt(coloredName(level, session, current));
            if (!prompt.equals(Component.empty())) return prompt;
        }

        if (current == null || current.knockedDown()) return Component.empty();
        ServerPlayer controller = current.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
        Component name = coloredName(level, session, current);
        if (controller != null && (PendingCardActionManager.isExclusiveBusy(controller)
                || PendingCardActionManager.hasPendingSelection(controller)
                || PendingCardActionManager.hasBoardCardUi(controller)
                || BoardPanelSelectionService.hasPending(controller))) {
            return Component.translatable("message.astral_craft.board.prompt.preparing_card", name);
        }
        if (movement != null || session.encounter() != null || BoardBattleService.active(session.id())
                || BoardEventService.active(session.id())) return Component.empty();

        return Component.translatable(session.actionPromptDeadlineTick() > 0L
                ? "message.astral_craft.board.prompt.turn_start"
                : "message.astral_craft.board.prompt.thinking", name);
    }

    private static Component coloredNames(ServerLevel level, BoardSession session) {
        MutableComponent names = Component.empty();
        List<BoardParticipant> participants = session.turnOrder().stream().map(session::participant)
                .flatMap(Optional::stream).toList();
        for (int index = 0; index < participants.size(); index++) {
            if (index > 0) names.append(Component.literal("、").withStyle(ChatFormatting.WHITE));
            names.append(coloredName(level, session, participants.get(index)));
        }
        return names;
    }

    private static Component coloredName(ServerLevel level, BoardSession session, BoardParticipant participant) {
        int index = Math.max(0, session.turnOrder().indexOf(participant.slotUuid()));
        return Component.literal(BoardSessionManager.displayName(level, participant))
                .withStyle(PLAYER_COLORS[index % PLAYER_COLORS.length]);
    }

    private static void addParticipant(ServerLevel level, BoardSession session, List<PawnView> pawns, BoardParticipant participant) {
        if (!session.positions().containsKey(participant.currentNodeKey())) return;
        pawns.add(new PawnView(participant.characterId(), participant.skinId(), participant.slotUuid(),
                BoardSessionManager.displayName(level, participant), participant.stats().starCoins(),
                participant.stats().health(), participant.stats().maxHealth(), participant.stats().stars(),
                participant.knockedDown(), participant.disconnectedHuman(), participant.hand().size()));
    }
}
