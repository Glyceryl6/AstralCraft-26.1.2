package com.astral_craft.common.gameplay;

import net.minecraft.server.level.ServerPlayer;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Small server-side scheduler used to make card effects wait until the reveal animation has finished.
 * The queued runnable is executed on the server tick thread from CommonEventSubscriber.
 */
public final class PendingCardActionManager {

    private static final Queue<PendingAction> ACTIONS = new ConcurrentLinkedQueue<>();

    private PendingCardActionManager() {}

    public static void schedule(ServerPlayer player, int delayTicks, Runnable action) {
        ACTIONS.add(new PendingAction(player.getUUID(), Math.max(0, delayTicks), action));
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
        @SuppressWarnings("unused")
        private final UUID owner;
        private int ticksLeft;
        private final Runnable action;

        private PendingAction(UUID owner, int ticksLeft, Runnable action) {
            this.owner = owner;
            this.ticksLeft = ticksLeft;
            this.action = action;
        }

        private boolean tick() {
            if (this.ticksLeft-- > 0) {
                return false;
            }

            this.action.run();
            return true;
        }
    }

}