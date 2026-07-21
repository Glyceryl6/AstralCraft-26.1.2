package com.astral_craft.common.gameplay.board;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** One server-side step in a board event execution. */
public interface BoardEventTask {

    static BoardEventTask action(Runnable action, int waitTicks) {
        return new BoardEventTask() {
            private boolean applied;
            private int remaining = Math.max(0, waitTicks);

            @Override
            public boolean tick() {
                if (!this.applied) {
                    this.applied = true;
                    action.run();
                }
                if (this.remaining <= 0) return false;
                this.remaining--;
                return this.remaining > 0;
            }
        };
    }

    /** @return true while this task still owns the board event flow. */
    boolean tick();

    default void participantBecameAutomated(UUID slotId) {}

    default boolean chooseLotteryNumber(ServerPlayer player, int number) {
        return false;
    }

    default void close() {}
}
