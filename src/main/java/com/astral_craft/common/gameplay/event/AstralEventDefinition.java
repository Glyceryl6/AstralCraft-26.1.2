package com.astral_craft.common.gameplay.event;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record AstralEventDefinition(
        Identifier id,
        String nameKey,
        String descriptionKey,
        String kind,
        Identifier texture,
        List<String> triggers,
        List<AstralEventCondition> conditions,
        AstralEventTargetDefinition target,
        AstralEventTriggerSettings triggerSettings,
        List<AstralEventEffect> effects,
        List<AstralEventEffect> intervalEffects,
        List<String> activeTriggers,
        List<AstralEventEffect> activeEffects,
        List<AstralEventEffect> endEffects,
        int cooldownTicks,
        double chance,
        boolean broadcast,
        String timing,
        int durationTicks,
        int intervalTicks) {

    private static final MapCodec<AstralEventIdentity> IDENTITY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("unknown_event")).forGetter(AstralEventIdentity::id),
            Codec.STRING.fieldOf("name_key").forGetter(AstralEventIdentity::nameKey),
            Codec.STRING.fieldOf("description_key").forGetter(AstralEventIdentity::descriptionKey),
            Codec.STRING.optionalFieldOf("kind", "neutral").forGetter(AstralEventIdentity::kind),
            Identifier.CODEC.optionalFieldOf("texture", AstralCraft.prefix("textures/gui/cards/event.png")).forGetter(AstralEventIdentity::texture)
    ).apply(instance, AstralEventIdentity::new));

    private static final MapCodec<AstralEventTriggerPart> TRIGGER_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("triggers", List.of()).forGetter(AstralEventTriggerPart::triggers),
            AstralEventCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(AstralEventTriggerPart::conditions),
            AstralEventTargetDefinition.CODEC.optionalFieldOf("target", AstralEventTargetDefinition.DEFAULT).forGetter(AstralEventTriggerPart::target),
            AstralEventTriggerSettings.CODEC.optionalFieldOf("trigger_settings", AstralEventTriggerSettings.DEFAULT).forGetter(AstralEventTriggerPart::triggerSettings),
            Codec.INT.optionalFieldOf("cooldown_ticks", 600).forGetter(AstralEventTriggerPart::cooldownTicks),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(AstralEventTriggerPart::chance),
            Codec.BOOL.optionalFieldOf("broadcast", false).forGetter(AstralEventTriggerPart::broadcast)
    ).apply(instance, AstralEventTriggerPart::new));

    private static final MapCodec<AstralEventEffectsPart> EFFECTS_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(AstralEventEffectsPart::effects),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("interval_effects", List.of()).forGetter(AstralEventEffectsPart::intervalEffects),
            Codec.STRING.listOf().optionalFieldOf("active_triggers", List.of()).forGetter(AstralEventEffectsPart::activeTriggers),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("active_effects", List.of()).forGetter(AstralEventEffectsPart::activeEffects),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("end_effects", List.of()).forGetter(AstralEventEffectsPart::endEffects),
            Codec.STRING.optionalFieldOf("timing", "instant").forGetter(AstralEventEffectsPart::timing),
            Codec.INT.optionalFieldOf("duration_ticks", 0).forGetter(AstralEventEffectsPart::durationTicks),
            Codec.INT.optionalFieldOf("interval_ticks", 20).forGetter(AstralEventEffectsPart::intervalTicks)
    ).apply(instance, AstralEventEffectsPart::new));

    public static final Codec<AstralEventDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTITY_CODEC.forGetter(AstralEventDefinition::identity),
            TRIGGER_CODEC.forGetter(AstralEventDefinition::triggerPart),
            EFFECTS_CODEC.forGetter(AstralEventDefinition::effectsPart)
    ).apply(instance, AstralEventDefinition::fromCodecParts));

    private static AstralEventDefinition fromCodecParts(AstralEventIdentity identity, AstralEventTriggerPart trigger, AstralEventEffectsPart effects) {
        return new AstralEventDefinition(
                identity.id(),
                identity.nameKey(),
                identity.descriptionKey(),
                identity.kind(),
                identity.texture(),
                trigger.triggers(),
                trigger.conditions(),
                trigger.target(),
                trigger.triggerSettings(),
                effects.effects(),
                effects.intervalEffects(),
                effects.activeTriggers(),
                effects.activeEffects(),
                effects.endEffects(),
                trigger.cooldownTicks(),
                trigger.chance(),
                trigger.broadcast(),
                effects.timing(),
                effects.durationTicks(),
                effects.intervalTicks());
    }

    private AstralEventIdentity identity() {
        return new AstralEventIdentity(this.id, this.nameKey, this.descriptionKey, this.kind, this.texture);
    }

    private AstralEventTriggerPart triggerPart() {
        return new AstralEventTriggerPart(this.triggers, this.conditions, this.target, this.triggerSettings, this.cooldownTicks, this.chance, this.broadcast);
    }

    private AstralEventEffectsPart effectsPart() {
        return new AstralEventEffectsPart(this.effects, this.intervalEffects, this.activeTriggers, this.activeEffects, this.endEffects, this.timing, this.durationTicks, this.intervalTicks);
    }

    public boolean canTriggerFrom(String trigger) {
        if (trigger == null || trigger.isBlank()) return false;
        return this.triggers.contains(trigger) || this.triggers.contains("*");
    }

    public boolean canApplyDuring(String trigger) {
        if (trigger == null || trigger.isBlank()) return false;
        return this.activeTriggers.contains(trigger) || this.activeTriggers.contains("*");
    }

    public boolean testConditions(AstralEventContext context) {
        for (AstralEventCondition condition : this.conditions) {
            if (condition != null && !condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    public boolean good() {
        return "good".equalsIgnoreCase(this.kind);
    }

    public boolean bad() {
        return "bad".equalsIgnoreCase(this.kind);
    }

    public boolean durationBased() {
        return "duration".equalsIgnoreCase(this.timing) || this.durationTicks > 0;
    }

    public int safeDurationTicks() {
        return Math.max(0, this.durationTicks);
    }

    public int safeIntervalTicks() {
        return Math.max(1, this.intervalTicks);
    }

    public List<AstralEventEffect> safeIntervalEffects() {
        return this.intervalEffects.isEmpty() ? this.effects : this.intervalEffects;
    }

    private record AstralEventIdentity(Identifier id, String nameKey, String descriptionKey, String kind, Identifier texture) {}

    private record AstralEventTriggerPart(
            List<String> triggers,
            List<AstralEventCondition> conditions,
            AstralEventTargetDefinition target,
            AstralEventTriggerSettings triggerSettings,
            int cooldownTicks,
            double chance,
            boolean broadcast) {}

    private record AstralEventEffectsPart(
            List<AstralEventEffect> effects,
            List<AstralEventEffect> intervalEffects,
            List<String> activeTriggers,
            List<AstralEventEffect> activeEffects,
            List<AstralEventEffect> endEffects,
            String timing,
            int durationTicks,
            int intervalTicks) {}

}