package com.astral_craft.common.util;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persistent server tick clock that is independent from each level's game time. */
public class AstralServerTickClock extends SavedData {

    public static final Codec<AstralServerTickClock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("ticks", 0L).forGetter(AstralServerTickClock::ticks)
    ).apply(instance, AstralServerTickClock::new));

    public static final SavedDataType<AstralServerTickClock> TYPE = new SavedDataType<>(
            AstralCraft.prefix("server_tick_clock"), AstralServerTickClock::new, CODEC);

    private long ticks;

    public AstralServerTickClock() {}

    public AstralServerTickClock(long ticks) {
        this.ticks = Math.max(0L, ticks);
    }

    public static void tick(MinecraftServer server) {
        AstralServerTickClock clock = get(server);
        clock.ticks++;
        clock.setDirty();
    }

    public static long now(MinecraftServer server) {
        return get(server).ticks();
    }

    public static long now(ServerLevel level) {
        return now(level.getServer());
    }

    public static long now(Level level) {
        MinecraftServer server = level.getServer();
        return server == null ? 0L : now(server);
    }

    public long ticks() {
        return this.ticks;
    }

    private static AstralServerTickClock get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
