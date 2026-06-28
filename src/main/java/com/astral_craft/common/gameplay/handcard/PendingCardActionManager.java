package com.astral_craft.common.gameplay.handcard;

import net.minecraft.server.level.ServerPlayer;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Small server-side scheduler used to make card effects wait until the reveal animation has finished.
 * The queued runnable is executed on the server tick thread from CommonEventSubscriber.
 */
public class PendingCardActionManager {

    private static final Queue<PendingAction> ACTIONS = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> EXCLUSIVE_OWNERS = ConcurrentHashMap.newKeySet();

    public static void schedule(ServerPlayer player, int delayTicks, Runnable action) {
        ACTIONS.add(new PendingAction(player.getUUID(), Math.max(0, delayTicks), action, false));
    }

    /**
     * Schedules a reveal-gated action. Only one exclusive reveal action may be active per player.
     *
     * @return {@code true} when the action was queued, {@code false} when this player is still in a reveal window.
     */
    public static boolean scheduleExclusive(ServerPlayer player, int delayTicks, Runnable action) {
        UUID owner = player.getUUID();
        if (!EXCLUSIVE_OWNERS.add(owner)) {
            return false;
        }

        ACTIONS.add(new PendingAction(owner, Math.max(0, delayTicks), action, true));
        return true;
    }

    public static boolean isExclusiveBusy(ServerPlayer player) {
        return player != null && EXCLUSIVE_OWNERS.contains(player.getUUID());
    }

    public static void serverTick() {
        int size = ACTIONS.size();
        for (int i = 0; i < size; i++) {
            PendingAction action = ACTIONS.poll();
            if (action == null) {
                return;
            }
            if (!action.tick()) {
                ACTIONS.add(action);
            }
        }
    }

    private static final class PendingAction {
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
            if (this.ticksLeft-- > 0) {
                return false;
            }

            try {
                this.action.run();
            } finally {
                if (this.exclusive) {
                    EXCLUSIVE_OWNERS.remove(this.owner);
                }
            }
            return true;
        }
    }

}
