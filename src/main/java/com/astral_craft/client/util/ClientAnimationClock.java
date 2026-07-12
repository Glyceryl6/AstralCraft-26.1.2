package com.astral_craft.client.util;

import net.minecraft.client.DeltaTracker;

/**
 * Pause-aware client presentation clock measured in vanilla ticks.
 *
 * <p>The clock advances from the render-frame delta instead of a level's game time or a
 * client-tick counter. This keeps animations smooth at the current frame rate, preserves
 * vanilla singleplayer pause behaviour and remains independent from server-side game-time
 * changes or tick freezing.</p>
 */
public class ClientAnimationClock {

    private static double animationTicks;
    private static boolean paused;
    private static boolean discardNextFrameDelta = true;

    public static void updateFrame(DeltaTracker deltaTracker) {
        float frameDeltaTicks = deltaTracker.getRealtimeDeltaTicks();
        if (discardNextFrameDelta) {
            discardNextFrameDelta = false;
            return;
        }

        if (paused || !Float.isFinite(frameDeltaTicks) || frameDeltaTicks <= 0.0F) return;
        animationTicks += frameDeltaTicks;
    }

    public static void setPaused(boolean paused) {
        if (ClientAnimationClock.paused == paused) return;
        ClientAnimationClock.paused = paused;
        discardNextFrameDelta = true;
    }

    public static double nowTicks() {
        return animationTicks;
    }

    public static float elapsedTicks(double startedAtTick) {
        double elapsed = Math.max(0.0D, animationTicks - startedAtTick);
        return elapsed >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) elapsed;
    }

    public static long elapsedWholeTicks(double startedAtTick) {
        return Math.max(0L, (long) Math.floor(animationTicks - startedAtTick));
    }

    public static float phaseTicks(int periodTicks) {
        int safePeriod = Math.max(1, periodTicks);
        return (float) (animationTicks % safePeriod);
    }

}