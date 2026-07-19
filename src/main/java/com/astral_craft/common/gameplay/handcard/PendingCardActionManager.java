package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Server-side scheduler for reveal-gated card actions and short-lived card choice sessions.
 * All callbacks are executed by {@code CommonEventSubscriber} on the server tick thread.
 */
public class PendingCardActionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingCardActionManager.class);
    private static final Queue<PendingAction> ACTIONS = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> EXCLUSIVE_OWNERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, PendingTargetSelection> TARGET_SELECTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingNumberSelection> NUMBER_SELECTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingBoardUi> BOARD_UI = new ConcurrentHashMap<>();
    private static final int SELECTION_TIMEOUT_TICKS = 20 * 30;
    private static final int BOARD_UI_TIMEOUT_TICKS = 20 * 120;

    public static void schedule(ServerPlayer player, int delayTicks, Runnable action) {
        if (player == null) return;
        schedule(player.getUUID(), delayTicks, action);
    }

    public static void schedule(UUID owner, int delayTicks, Runnable action) {
        if (owner == null || action == null) return;
        ACTIONS.add(new PendingAction(owner, Math.max(0, delayTicks), action, false));
    }

    public static boolean scheduleExclusive(ServerPlayer player, int delayTicks, Runnable action) {
        UUID owner = player.getUUID();
        if (!EXCLUSIVE_OWNERS.add(owner)) return false;
        ACTIONS.add(new PendingAction(owner, Math.max(0, delayTicks), action, true));
        return true;
    }

    public static boolean isExclusiveBusy(ServerPlayer player) {
        return player != null && EXCLUSIVE_OWNERS.contains(player.getUUID());
    }

    public static boolean hasPendingSelection(ServerPlayer player) {
        if (player == null) return false;
        UUID owner = player.getUUID();
        return TARGET_SELECTIONS.containsKey(owner) || NUMBER_SELECTIONS.containsKey(owner);
    }

    public static void cancel(ServerPlayer player) {
        if (player == null) return;
        UUID owner = player.getUUID();
        TARGET_SELECTIONS.remove(owner);
        NUMBER_SELECTIONS.remove(owner);
        BOARD_UI.remove(owner);
        EXCLUSIVE_OWNERS.remove(owner);
        ACTIONS.removeIf(action -> action.owner.equals(owner));
    }


    public static void beginBoardCardUi(ServerPlayer player, UUID boardId, boolean waitForDamage) {
        if (player == null || boardId == null) return;
        BOARD_UI.put(player.getUUID(), new PendingBoardUi(boardId, waitForDamage,
                player.level().getGameTime(), BOARD_UI_TIMEOUT_TICKS));
    }

    public static void completeBoardCardUi(ServerPlayer player) {
        if (player == null) return;
        PendingBoardUi pending = BOARD_UI.remove(player.getUUID());
        if (pending != null) {
            BoardSessionManager.resumeAfterCardUi(player, pending.boardId(),
                    Math.max(0L, player.level().getGameTime() - pending.startedAtTick()));
        }
    }

    public static void notifyBoardDamage(LivingEntity source) {
        ServerPlayer controller = source instanceof ServerPlayer player ? player
                : source instanceof AstralCharacterEntity character
                ? BoardSessionManager.controllerFor(character).orElse(null) : null;
        if (controller == null) return;
        PendingBoardUi pending = BOARD_UI.get(controller.getUUID());
        if (pending != null && pending.waitForDamage()) completeBoardCardUi(controller);
    }

    public static boolean waitsForBoardDamage(ServerPlayer player) {
        PendingBoardUi pending = player == null ? null : BOARD_UI.get(player.getUUID());
        return pending != null && pending.waitForDamage();
    }

    public static boolean hasBoardCardUi(ServerPlayer player) {
        return player != null && BOARD_UI.containsKey(player.getUUID());
    }

    public static void beginTargetSelection(ServerPlayer player, ItemStack cardStack, int handIndex) {
        TARGET_SELECTIONS.put(player.getUUID(), new PendingTargetSelection(
                cardStack.copyWithCount(1), handIndex, SELECTION_TIMEOUT_TICKS));
    }

    @Nullable
    public static PendingTargetSelection consumeTargetSelection(ServerPlayer player, ItemStack requestedStack, int handIndex) {
        PendingTargetSelection selection = TARGET_SELECTIONS.get(player.getUUID());
        if (selection == null || requestedStack.isEmpty()) return null;
        if (selection.handIndex() != handIndex
                || !ItemStack.isSameItemSameComponents(selection.cardStack(), requestedStack)) return null;
        TARGET_SELECTIONS.remove(player.getUUID(), selection);
        return selection;
    }

    public static void beginNumberSelection(ServerPlayer player, ItemStack cardStack, int minValue, int maxValue) {
        int safeMin = Math.min(minValue, maxValue);
        int safeMax = Math.max(minValue, maxValue);
        NUMBER_SELECTIONS.put(player.getUUID(), new PendingNumberSelection(
                cardStack.copyWithCount(1), safeMin, safeMax, SELECTION_TIMEOUT_TICKS));
    }

    @Nullable
    public static PendingNumberSelection consumeNumberSelection(ServerPlayer player, ItemStack requestedStack, int value) {
        PendingNumberSelection selection = NUMBER_SELECTIONS.get(player.getUUID());
        if (selection == null || requestedStack.isEmpty()) return null;
        if (!ItemStack.isSameItemSameComponents(selection.cardStack(), requestedStack)) return null;
        if (value < selection.minValue() || value > selection.maxValue()) return null;
        NUMBER_SELECTIONS.remove(player.getUUID(), selection);
        return selection;
    }

    public static void serverTick() {
        int size = ACTIONS.size();
        for (int i = 0; i < size; i++) {
            PendingAction action = ACTIONS.poll();
            if (action == null) break;
            if (!action.tick()) ACTIONS.add(action);
        }

        TARGET_SELECTIONS.replaceAll((_, selection) -> selection.tick());
        TARGET_SELECTIONS.entrySet().removeIf(entry -> entry.getValue().expired());
        NUMBER_SELECTIONS.replaceAll((_, selection) -> selection.tick());
        NUMBER_SELECTIONS.entrySet().removeIf(entry -> entry.getValue().expired());
        BOARD_UI.replaceAll((_, pending) -> pending.tick());
        for (Map.Entry<UUID, PendingBoardUi> entry : Set.copyOf(BOARD_UI.entrySet())) {
            if (!entry.getValue().expired()) continue;
            PendingBoardUi pending = BOARD_UI.remove(entry.getKey());
            if (pending == null) continue;
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer() == null ? null
                    : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                BoardSessionManager.resumeAfterCardUi(player, pending.boardId(),
                        Math.max(0L, player.level().getGameTime() - pending.startedAtTick()));
            }
        }
    }

    public record PendingTargetSelection(ItemStack cardStack, int handIndex, int ticksLeft) {
        private PendingTargetSelection tick() {
            return new PendingTargetSelection(this.cardStack, this.handIndex, this.ticksLeft - 1);
        }

        private boolean expired() {
            return this.ticksLeft <= 0;
        }
    }

    public record PendingNumberSelection(ItemStack cardStack, int minValue, int maxValue, int ticksLeft) {
        private PendingNumberSelection tick() {
            return new PendingNumberSelection(this.cardStack, this.minValue, this.maxValue, this.ticksLeft - 1);
        }

        private boolean expired() {
            return this.ticksLeft <= 0;
        }
    }


    private record PendingBoardUi(UUID boardId, boolean waitForDamage, long startedAtTick, int ticksLeft) {
        private PendingBoardUi tick() {
            return new PendingBoardUi(this.boardId, this.waitForDamage, this.startedAtTick, this.ticksLeft - 1);
        }

        private boolean expired() {
            return this.ticksLeft <= 0;
        }
    }

    private static class PendingAction {
        private final UUID owner;
        private int ticksLeft;
        private final Runnable action;
        private final boolean exclusive;

        private PendingAction(UUID owner, int ticksLeft, Runnable action, boolean exclusive) {
            this.owner = owner;
            this.ticksLeft = ticksLeft;
            this.action = action;
            this.exclusive = exclusive;
        }

        private boolean tick() {
            if (this.ticksLeft-- > 0) return false;
            try {
                this.action.run();
            } catch (RuntimeException exception) {
                LOGGER.error("Card reveal callback failed for owner {}", this.owner, exception);
            } finally {
                if (this.exclusive) EXCLUSIVE_OWNERS.remove(this.owner);
            }
            return true;
        }
    }

}
