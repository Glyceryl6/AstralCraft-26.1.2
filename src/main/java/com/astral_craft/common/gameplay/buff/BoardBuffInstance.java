package com.astral_craft.common.gameplay.buff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/** One logical board buff instance backed by a reusable registered behavior. */
public record BoardBuffInstance(
        Identifier id,
        BoardBuff buff,
        int duration,
        int amplifier,
        int intrinsicLevels,
        boolean fresh,
        int value,
        Optional<Component> customName,
        Optional<Identifier> customIcon,
        Optional<Integer> customColor,
        boolean consumeAfterIncomingDamage,
        boolean consumeAfterMoveRoll) {

    public static final int PERMANENT = -1;
    public static final Codec<BoardBuffInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(BoardBuffInstance::id),
            BoardBuff.CODEC.fieldOf("buff").forGetter(BoardBuffInstance::buff),
            Codec.INT.optionalFieldOf("duration", 1).forGetter(BoardBuffInstance::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(BoardBuffInstance::amplifier),
            Codec.INT.optionalFieldOf("intrinsic_levels", 0).forGetter(BoardBuffInstance::intrinsicLevels),
            Codec.BOOL.optionalFieldOf("fresh", false).forGetter(BoardBuffInstance::fresh),
            Codec.INT.optionalFieldOf("value", 1).forGetter(BoardBuffInstance::value),
            ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(BoardBuffInstance::customName),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(BoardBuffInstance::customIcon),
            Codec.INT.optionalFieldOf("color").forGetter(BoardBuffInstance::customColor),
            Codec.BOOL.optionalFieldOf("consume_after_incoming_damage", false).forGetter(BoardBuffInstance::consumeAfterIncomingDamage),
            Codec.BOOL.optionalFieldOf("consume_after_move_roll", false).forGetter(BoardBuffInstance::consumeAfterMoveRoll)
    ).apply(instance, BoardBuffInstance::new));

    public BoardBuffInstance {
        if (id == null) throw new IllegalArgumentException("Board buff instance id cannot be null");
        if (buff == null) throw new IllegalArgumentException("Board buff behavior cannot be null");
        amplifier = Math.max(0, amplifier);
        intrinsicLevels = Math.clamp(intrinsicLevels, 0, amplifier + 1);
        duration = intrinsicLevels >= amplifier + 1 ? PERMANENT
                : duration == PERMANENT ? PERMANENT : Math.max(1, duration);
        fresh = intrinsicLevels < amplifier + 1 && fresh;
    }

    public static Builder builder(Identifier id, BoardBuff buff) {
        return new Builder(id, buff);
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

    public Component displayName() {
        return this.customName.orElseGet(this.buff::displayName);
    }

    public Identifier icon() {
        return this.customIcon.orElseGet(this.buff::icon);
    }

    public Optional<Integer> color() {
        return this.customColor;
    }

    public BoardBuffInstance activate() {
        return this.fresh ? this.copy(this.id, this.duration, this.amplifier, this.intrinsicLevels, false) : this;
    }

    public BoardBuffInstance tickDown() {
        return this.permanent() ? this : this.copy(this.id, Math.max(1, this.duration - 1), this.amplifier, this.intrinsicLevels, false);
    }

    public BoardBuffInstance withId(Identifier id) {
        return this.copy(id, this.duration, this.amplifier, this.intrinsicLevels, this.fresh);
    }

    public BoardBuffInstance withAcquiredLevels(int levels) {
        int safeLevels = Math.max(0, levels);
        int total = this.intrinsicLevels + safeLevels;
        return total <= 0 ? null : this.copy(this.id, safeLevels == 0 ? PERMANENT : this.duration, total - 1,
                this.intrinsicLevels, safeLevels > 0 && this.fresh);
    }

    public BoardBuffInstance withoutAcquiredLevels() {
        return this.intrinsicLevels <= 0 ? null
                : this.copy(this.id, PERMANENT, this.intrinsicLevels - 1, this.intrinsicLevels, false);
    }

    public BoardBuffInstance withPresentation(Component name, Identifier icon) {
        return new BoardBuffInstance(this.id, this.buff, this.duration, this.amplifier, this.intrinsicLevels, this.fresh,
                this.value, Optional.ofNullable(name), Optional.ofNullable(icon), this.customColor,
                this.consumeAfterIncomingDamage, this.consumeAfterMoveRoll);
    }

    public BoardBuffInstance withValue(int value) {
        return new BoardBuffInstance(this.id, this.buff, this.duration, this.amplifier, this.intrinsicLevels, this.fresh,
                value, this.customName, this.customIcon, this.customColor, this.consumeAfterIncomingDamage,
                this.consumeAfterMoveRoll);
    }

    private BoardBuffInstance copy(Identifier id, int duration, int amplifier, int intrinsicLevels, boolean fresh) {
        return new BoardBuffInstance(id, this.buff, duration, amplifier, intrinsicLevels, fresh, this.value,
                this.customName, this.customIcon, this.customColor, this.consumeAfterIncomingDamage,
                this.consumeAfterMoveRoll);
    }

    public static class Builder {
        private final Identifier id;
        private final BoardBuff buff;
        private int duration = 1;
        private int amplifier;
        private int intrinsicLevels;
        private boolean fresh = true;
        private int value = 1;
        private Component customName;
        private Identifier customIcon;
        private Integer customColor;
        private boolean consumeAfterIncomingDamage;
        private boolean consumeAfterMoveRoll;

        private Builder(Identifier id, BoardBuff buff) {
            this.id = id;
            this.buff = buff;
        }

        public Builder duration(int duration) { this.duration = duration; return this; }
        public Builder permanent() { this.duration = PERMANENT; return this; }
        public Builder level(int level) { this.amplifier = Math.max(0, level - 1); return this; }
        public Builder amplifier(int amplifier) { this.amplifier = Math.max(0, amplifier); return this; }
        public Builder value(int value) { this.value = value; return this; }
        public Builder intrinsic() { this.intrinsicLevels = this.amplifier + 1; this.duration = PERMANENT; return this; }
        public Builder intrinsicLevels(int levels) { this.intrinsicLevels = Math.max(0, levels); return this; }
        public Builder fresh(boolean fresh) { this.fresh = fresh; return this; }
        public Builder displayName(Component displayName) { this.customName = displayName; return this; }
        public Builder icon(Identifier icon) { this.customIcon = icon; return this; }
        public Builder color(int color) { this.customColor = color; return this; }
        public Builder presentation(Component displayName, Identifier icon) { return this.displayName(displayName).icon(icon); }
        public Builder consumeAfterIncomingDamage() { this.consumeAfterIncomingDamage = true; return this; }
        public Builder consumeAfterMoveRoll() { this.consumeAfterMoveRoll = true; return this; }

        public BoardBuffInstance build() {
            return new BoardBuffInstance(this.id, this.buff, this.duration, this.amplifier, this.intrinsicLevels,
                    this.fresh, this.value, Optional.ofNullable(this.customName), Optional.ofNullable(this.customIcon),
                    Optional.ofNullable(this.customColor), this.consumeAfterIncomingDamage, this.consumeAfterMoveRoll);
        }
    }
}
