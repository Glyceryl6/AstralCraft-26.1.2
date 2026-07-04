package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.gameplay.event.type.AstralEventIdentifiers;
import com.astral_craft.common.gameplay.event.type.AstralEventRepeatModes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record AstralEventTriggerSettings(Identifier repeatMode, String uniqueKey, int retriggerDelayTicks, int maxTriggers, boolean preventWhileActive) {

    public static final AstralEventTriggerSettings DEFAULT = new AstralEventTriggerSettings(AstralEventRepeatModes.COOLDOWN, "", 0, 0, true);

    public static final Codec<AstralEventTriggerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AstralEventIdentifiers.CODEC.optionalFieldOf("repeat_mode", AstralEventRepeatModes.COOLDOWN).forGetter(AstralEventTriggerSettings::repeatMode),
            Codec.STRING.optionalFieldOf("unique_key", "").forGetter(AstralEventTriggerSettings::uniqueKey),
            Codec.INT.optionalFieldOf("retrigger_delay_ticks", 0).forGetter(AstralEventTriggerSettings::retriggerDelayTicks),
            Codec.INT.optionalFieldOf("max_triggers", 0).forGetter(AstralEventTriggerSettings::maxTriggers),
            Codec.BOOL.optionalFieldOf("prevent_while_active", true).forGetter(AstralEventTriggerSettings::preventWhileActive)
    ).apply(instance, AstralEventTriggerSettings::new));

    public String key(AstralEventDefinition definition) {
        if (this.uniqueKey != null && !this.uniqueKey.isBlank()) {
            return this.uniqueKey;
        }
        return definition.id().toString();
    }

    public int safeRetriggerDelayTicks(AstralEventDefinition definition) {
        if (this.retriggerDelayTicks > 0) {
            return this.retriggerDelayTicks;
        }

        return Math.max(0, definition.cooldownTicks());
    }

    public boolean always() {
        return AstralEventRepeatModes.isAlways(this.repeatMode);
    }

    public boolean oncePerTarget() {
        return AstralEventRepeatModes.isOncePerTarget(this.repeatMode);
    }

    public boolean oncePerPlayer() {
        return AstralEventRepeatModes.isOncePerPlayer(this.repeatMode);
    }

    public boolean whileInactiveOnly() {
        return AstralEventRepeatModes.isWhileInactive(this.repeatMode);
    }

}
