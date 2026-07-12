package com.astral_craft.client.util;

import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;

/**
 * Client presentation clock which advances at the normal client tick rate but is
 * independent from a level's game time. The clock keeps the vanilla singleplayer
 * pause behaviour while ignoring server-side game-time resets and tick freezing.
 */
public class ClientAnimationClock {

    private static long tickCount;
    private static float partialTick;
    private static boolean paused;

    public static void tick() {
        if (!paused) {
            tickCount++;
        }
    }

    public static void updateFrame(DeltaTracker deltaTracker) {
        float value = deltaTracker.getGameTimeDeltaPartialTick(true);
        partialTick = Float.isFinite(value) ? Mth.clamp(value, 0.0F, 1.0F) : 0.0F;
    }

    public static void setPaused(boolean paused) {
        ClientAnimationClock.paused = paused;
    }

    public static long nowTicks() {
        return tickCount;
    }

    public static float elapsedTicks(long startedAtTick) {
        return Math.max(0.0F, tickCount - startedAtTick + partialTick);
    }

    public static long elapsedWholeTicks(long startedAtTick) {
        return Math.max(0L, tickCount - startedAtTick);
    }

    public static float phaseTicks(int periodTicks) {
        int safePeriod = Math.max(1, periodTicks);
        return Math.floorMod(tickCount, safePeriod) + partialTick;
    }

}