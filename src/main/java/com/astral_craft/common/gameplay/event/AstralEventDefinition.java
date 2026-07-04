package com.astral_craft.common.gameplay.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.type.AstralEventIdentifiers;
import com.astral_craft.common.gameplay.event.type.AstralEventKinds;
import com.astral_craft.common.gameplay.event.type.AstralEventLocalizationKeys;
import com.astral_craft.common.gameplay.event.type.AstralEventTimings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;

import java.util.List;

public record AstralEventDefinition(
        Identifier id,
        String nameKey,
        String descriptionKey,
        Identifier kind,
        Identifier texture,
        boolean triggers,
        List<AstralEventCondition> conditions,
        List<Difficulty> difficulties,
        AstralEventTargetDefinition target,
        AstralEventTriggerSettings triggerSettings,
        List<AstralEventEffect> effects,
        List<AstralEventEffect> intervalEffects,
        List<AstralEventCondition> activeConditions,
        List<AstralEventEffect> activeEffects,
        List<AstralEventEffect> endEffects,
        int cooldownTicks,
        double chance,
        boolean broadcast,
        Identifier timing,
        int durationTicks,
        int intervalTicks) {

    private static final MapCodec<AstralEventIdentity> IDENTITY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("unknown_event")).forGetter(AstralEventIdentity::id),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(AstralEventIdentity::nameKey),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(AstralEventIdentity::descriptionKey),
            AstralEventIdentifiers.CODEC.optionalFieldOf("kind", AstralEventKinds.NEUTRAL).forGetter(AstralEventIdentity::kind),
            Identifier.CODEC.optionalFieldOf("texture", AstralCraft.prefix("textures/gui/cards/event.png")).forGetter(AstralEventIdentity::texture)
    ).apply(instance, AstralEventIdentity::new));

    private static final MapCodec<AstralEventTriggerPart> TRIGGER_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("triggers", true).forGetter(AstralEventTriggerPart::triggers),
            AstralEventCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(AstralEventTriggerPart::conditions),
            Difficulty.CODEC.listOf().optionalFieldOf("difficulties", List.of()).forGetter(AstralEventTriggerPart::difficulties),
            AstralEventTargetDefinition.CODEC.optionalFieldOf("target", AstralEventTargetDefinition.DEFAULT).forGetter(AstralEventTriggerPart::target),
            AstralEventTriggerSettings.CODEC.optionalFieldOf("trigger_settings", AstralEventTriggerSettings.DEFAULT).forGetter(AstralEventTriggerPart::triggerSettings),
            Codec.INT.optionalFieldOf("cooldown_ticks", 600).forGetter(AstralEventTriggerPart::cooldownTicks),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(AstralEventTriggerPart::chance),
            Codec.BOOL.optionalFieldOf("broadcast", false).forGetter(AstralEventTriggerPart::broadcast)
    ).apply(instance, AstralEventTriggerPart::new));

    private static final MapCodec<AstralEventEffectsPart> EFFECTS_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(AstralEventEffectsPart::effects),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("interval_effects", List.of()).forGetter(AstralEventEffectsPart::intervalEffects),
            AstralEventCondition.CODEC.listOf().optionalFieldOf("active_conditions", List.of()).forGetter(AstralEventEffectsPart::activeConditions),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("active_effects", List.of()).forGetter(AstralEventEffectsPart::activeEffects),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("end_effects", List.of()).forGetter(AstralEventEffectsPart::endEffects),
            AstralEventIdentifiers.CODEC.optionalFieldOf("timing", AstralEventTimings.INSTANT).forGetter(AstralEventEffectsPart::timing),
            Codec.INT.optionalFieldOf("duration_ticks", 0).forGetter(AstralEventEffectsPart::durationTicks),
            Codec.INT.optionalFieldOf("interval_ticks", 20).forGetter(AstralEventEffectsPart::intervalTicks)
    ).apply(instance, AstralEventEffectsPart::new));

    public static final Codec<AstralEventDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTITY_CODEC.forGetter(AstralEventDefinition::identity),
            TRIGGER_CODEC.forGetter(AstralEventDefinition::triggerPart),
            EFFECTS_CODEC.forGetter(AstralEventDefinition::effectsPart)
    ).apply(instance, AstralEventDefinition::fromCodecParts));

    private static AstralEventDefinition fromCodecParts(AstralEventIdentity identity, AstralEventTriggerPart trigger, AstralEventEffectsPart effects) {
        Identifier id = identity.id();
        String nameKey = AstralEventLocalizationKeys.normalizeName(id, identity.nameKey());
        String descriptionKey = AstralEventLocalizationKeys.normalizeDescription(id, identity.descriptionKey());
        return new AstralEventDefinition(
                id,
                nameKey,
                descriptionKey,
                identity.kind(),
                identity.texture(),
                trigger.triggers(),
                trigger.conditions(),
                trigger.difficulties(),
                trigger.target(),
                trigger.triggerSettings(),
                effects.effects(),
                effects.intervalEffects(),
                effects.activeConditions(),
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
        return new AstralEventTriggerPart(this.triggers, this.conditions, this.difficulties, this.target, this.triggerSettings, this.cooldownTicks, this.chance, this.broadcast);
    }

    private AstralEventEffectsPart effectsPart() {
        return new AstralEventEffectsPart(this.effects, this.intervalEffects, this.activeConditions, this.activeEffects, this.endEffects, this.timing, this.durationTicks, this.intervalTicks);
    }

    public boolean canAutoTrigger() {
        return this.triggers;
    }

    public boolean canTriggerInDifficulty(Difficulty difficulty) {
        if (this.difficulties == null || this.difficulties.isEmpty()) return true;
        Difficulty safeDifficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
        return this.difficulties.contains(safeDifficulty);
    }

    public boolean testConditions(AstralEventContext context) {
        for (AstralEventCondition condition : this.conditions) {
            if (condition != null && !condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    public boolean testActiveConditions(AstralEventContext context) {
        if (this.activeConditions.isEmpty()) return false;
        for (AstralEventCondition condition : this.activeConditions) {
            if (condition != null && !condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    public boolean good() {
        return AstralEventKinds.good(this.kind);
    }

    public boolean bad() {
        return AstralEventKinds.bad(this.kind);
    }

    public boolean durationBased() {
        return AstralEventTimings.duration(this.timing) || this.durationTicks > 0;
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

    private record AstralEventIdentity(Identifier id, String nameKey, String descriptionKey, Identifier kind, Identifier texture) {}

    private record AstralEventTriggerPart(
            boolean triggers,
            List<AstralEventCondition> conditions,
            List<Difficulty> difficulties,
            AstralEventTargetDefinition target,
            AstralEventTriggerSettings triggerSettings,
            int cooldownTicks,
            double chance,
            boolean broadcast) {}

    private record AstralEventEffectsPart(
            List<AstralEventEffect> effects,
            List<AstralEventEffect> intervalEffects,
            List<AstralEventCondition> activeConditions,
            List<AstralEventEffect> activeEffects,
            List<AstralEventEffect> endEffects,
            Identifier timing,
            int durationTicks,
            int intervalTicks) {}

}
