package com.astral_craft.common.gameplay.buff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One registered board buff with a total level and a separately preserved intrinsic portion. */
public record BoardBuffInstance(int duration, int amplifier, int intrinsicLevels, boolean fresh) {

    public static final int PERMANENT = -1;
    public static final Codec<BoardBuffInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("duration", 1).forGetter(BoardBuffInstance::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(BoardBuffInstance::amplifier),
            Codec.INT.optionalFieldOf("intrinsic_levels", 0).forGetter(BoardBuffInstance::intrinsicLevels),
            Codec.BOOL.optionalFieldOf("intrinsic", false).forGetter(BoardBuffInstance::fullyIntrinsic),
            Codec.BOOL.optionalFieldOf("fresh", false).forGetter(BoardBuffInstance::fresh)
    ).apply(instance, BoardBuffInstance::fromCodec));

    public BoardBuffInstance(int duration, int amplifier) {
        this(duration, amplifier, 0, true);
    }

    public BoardBuffInstance(int duration, int amplifier, boolean intrinsic) {
        this(duration, amplifier, intrinsic ? Math.max(0, amplifier) + 1 : 0, true);
    }

    public BoardBuffInstance(int duration, int amplifier, boolean intrinsic, boolean fresh) {
        this(duration, amplifier, intrinsic ? Math.max(0, amplifier) + 1 : 0, fresh);
    }

    public BoardBuffInstance {
        amplifier = Math.max(0, amplifier);
        intrinsicLevels = Math.clamp(intrinsicLevels, 0, amplifier + 1);
        duration = intrinsicLevels >= amplifier + 1 ? PERMANENT
                : duration == PERMANENT ? PERMANENT : Math.max(1, duration);
        fresh = intrinsicLevels < amplifier + 1 && fresh;
    }

    private static BoardBuffInstance fromCodec(
            int duration, int amplifier, int intrinsicLevels,
            boolean legacyIntrinsic, boolean fresh) {
        int level = Math.max(0, amplifier) + 1;
        int safeIntrinsicLevels = intrinsicLevels > 0 ? intrinsicLevels : legacyIntrinsic ? level : 0;
        return new BoardBuffInstance(duration, amplifier, safeIntrinsicLevels, fresh);
    }

    public int level() {
        return this.amplifier + 1;
    }

    public int acquiredLevels() {
        return Math.max(0, this.level() - this.intrinsicLevels);
    }

    public boolean fullyIntrinsic() {
        return this.intrinsicLevels >= this.level();
    }

    public boolean permanent() {
        return this.duration == PERMANENT;
    }

    public BoardBuffInstance activate() {
        return this.fresh ? new BoardBuffInstance(this.duration, this.amplifier, this.intrinsicLevels, false) : this;
    }

    public BoardBuffInstance tickDown() {
        return this.permanent() ? this : new BoardBuffInstance(Math.max(1, this.duration - 1), this.amplifier, this.intrinsicLevels, false);
    }

    public BoardBuffInstance withAcquiredLevels(int levels) {
        int safeLevels = Math.max(0, levels);
        int total = this.intrinsicLevels + safeLevels;
        return total <= 0 ? null : new BoardBuffInstance(safeLevels <= 0 ? PERMANENT : this.duration,
                total - 1, this.intrinsicLevels, safeLevels > 0 && this.fresh);
    }

    public BoardBuffInstance withoutAcquiredLevels() {
        return this.intrinsicLevels <= 0 ? null : new BoardBuffInstance(PERMANENT, this.intrinsicLevels - 1, this.intrinsicLevels, false);
    }

}