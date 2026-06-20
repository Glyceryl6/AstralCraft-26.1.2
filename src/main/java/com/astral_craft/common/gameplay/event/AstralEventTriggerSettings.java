package com.astral_craft.common.gameplay.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AstralEventTriggerSettings(String repeatMode, String uniqueKey, int retriggerDelayTicks, int maxTriggers, boolean preventWhileActive) {

    public static final AstralEventTriggerSettings DEFAULT = new AstralEventTriggerSettings("cooldown", "", 0, 0, true);

    public static final Codec<AstralEventTriggerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("repeat_mode", "cooldown").forGetter(AstralEventTriggerSettings::repeatMode),
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
        return "always".equalsIgnoreCase(this.repeatMode);
    }

    public boolean oncePerTarget() {
        return "once_per_target".equalsIgnoreCase(this.repeatMode) || "once".equalsIgnoreCase(this.repeatMode);
    }

    public boolean oncePerPlayer() {
        return "once_per_player".equalsIgnoreCase(this.repeatMode);
    }

    public boolean whileInactiveOnly() {
        return "while_inactive".equalsIgnoreCase(this.repeatMode);
    }

}