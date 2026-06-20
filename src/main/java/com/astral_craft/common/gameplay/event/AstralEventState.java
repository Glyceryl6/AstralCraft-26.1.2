package com.astral_craft.common.gameplay.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public record AstralEventState(
        Map<String, AstralActiveEventInstance> activeEvents,
        Map<String, Integer> cooldowns,
        Map<String, Integer> triggerCounts) {

    public static final AstralEventState EMPTY = new AstralEventState(Map.of(), Map.of(), Map.of());

    public static final Codec<AstralEventState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, AstralActiveEventInstance.CODEC).optionalFieldOf("active_events", Map.of()).forGetter(AstralEventState::activeEvents),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("cooldowns", Map.of()).forGetter(AstralEventState::cooldowns),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("trigger_counts", Map.of()).forGetter(AstralEventState::triggerCounts)
    ).apply(instance, AstralEventState::new));

    public AstralEventState {
        activeEvents = Map.copyOf(activeEvents);
        cooldowns = Map.copyOf(cooldowns);
        triggerCounts = Map.copyOf(triggerCounts);
    }

    public static AstralEventState empty() {
        return EMPTY;
    }

    public boolean active(String key) {
        return this.activeEvents.containsKey(key);
    }

    public int cooldownLeft(String key) {
        return Math.max(0, this.cooldowns.getOrDefault(key, 0));
    }

    public int triggerCount(String key) {
        return Math.max(0, this.triggerCounts.getOrDefault(key, 0));
    }

    public AstralEventState withActive(String key, AstralActiveEventInstance instance) {
        Map<String, AstralActiveEventInstance> next = new LinkedHashMap<>(this.activeEvents);
        next.put(key, instance);
        return new AstralEventState(next, this.cooldowns, this.triggerCounts);
    }

    public AstralEventState withoutActive(String key) {
        Map<String, AstralActiveEventInstance> next = new LinkedHashMap<>(this.activeEvents);
        next.remove(key);
        return new AstralEventState(next, this.cooldowns, this.triggerCounts);
    }

    public AstralEventState withCooldown(String key, int ticks) {
        if (ticks <= 0) return this;
        Map<String, Integer> next = new LinkedHashMap<>(this.cooldowns);
        next.put(key, ticks);
        return new AstralEventState(this.activeEvents, next, this.triggerCounts);
    }

    public AstralEventState markTriggered(String key) {
        Map<String, Integer> next = new LinkedHashMap<>(this.triggerCounts);
        next.put(key, Math.max(0, next.getOrDefault(key, 0)) + 1);
        return new AstralEventState(this.activeEvents, this.cooldowns, next);
    }

    public AstralEventState tickCooldowns() {
        if (this.cooldowns.isEmpty()) return this;
        Map<String, Integer> next = new LinkedHashMap<>();
        this.cooldowns.forEach((key, value) -> {
            int remaining = Math.max(0, value - 1);
            if (remaining > 0) {
                next.put(key, remaining);
            }
        });

        return new AstralEventState(this.activeEvents, next, this.triggerCounts);
    }

    public AstralEventState updateActive(String key, AstralActiveEventInstance instance) {
        if (!this.activeEvents.containsKey(key)) return this;
        Map<String, AstralActiveEventInstance> next = new LinkedHashMap<>(this.activeEvents);
        next.put(key, instance);
        return new AstralEventState(next, this.cooldowns, this.triggerCounts);
    }

}