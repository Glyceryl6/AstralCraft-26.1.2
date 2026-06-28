package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.*;
import com.astral_craft.common.gameplay.event.conditions.HealthEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PositionEventCondition;
import com.astral_craft.common.gameplay.event.conditions.TimeOfDayEventCondition;
import com.astral_craft.common.gameplay.event.effects.*;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

import java.util.List;

/** @noinspection deprecation*/
public class AstralEventBootstrap {

    public static final ResourceKey<Registry<AstralEventDefinition>> EVENTS = ResourceKey.createRegistryKey(AstralCraft.prefix("events"));
    public static final ResourceKey<AstralEventDefinition> LUCKY_FIND = key("lucky_find");
    public static final ResourceKey<AstralEventDefinition> AMBUSH = key("ambush");
    public static final ResourceKey<AstralEventDefinition> ASTRAL_BLESSING = key("astral_blessing");
    public static final ResourceKey<AstralEventDefinition> LOW_HEALTH_AID = key("low_health_aid");
    public static final ResourceKey<AstralEventDefinition> NIGHT_AMBUSH = key("night_ambush");
    public static final ResourceKey<AstralEventDefinition> CAVE_CACHE = key("cave_cache");

    public static void bootstrap(BootstrapContext<AstralEventDefinition> context) {
        context.register(LUCKY_FIND, luckyFind());
        context.register(AMBUSH, ambush());
        context.register(ASTRAL_BLESSING, astralBlessing());
        context.register(LOW_HEALTH_AID, lowHealthAid());
        context.register(NIGHT_AMBUSH, nightAmbush());
        context.register(CAVE_CACHE, caveCache());
    }

    public static ResourceKey<AstralEventDefinition> key(String path) {
        return ResourceKey.create(EVENTS, AstralCraft.prefix(path));
    }

    public static AstralEventDefinition luckyFind() {
        return event("lucky_find", "good", List.of("block_break"), List.of(),
                AstralEventTargetDefinition.DEFAULT, AstralEventTriggerSettings.DEFAULT,
                List.of(new GiveItemEventEffect(Items.EMERALD.builtInRegistryHolder(), 1)), List.of(), List.of(), List.of(),
                600, 0.2D, "instant", 0, 20);
    }

    public static AstralEventDefinition ambush() {
        return event("ambush", "bad", List.of("tick"), List.of(),
                List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings("cooldown", "", 2400, 0, true),
                List.of(new SummonEntityEventEffect(HolderSet.direct(EntityType.ZOMBIE.builtInRegistryHolder()), 1)),
                List.of(), List.of(), List.of(), 2400, 0.004D, "instant", 0, 20);
    }

    public static AstralEventDefinition astralBlessing() {
        AstralEventEffect bonusMining = new ChanceEventEffect(0.25D, new GiveItemEventEffect(Items.LAPIS_LAZULI.builtInRegistryHolder(), 1));
        return event("astral_blessing", "good", List.of("tick"), List.of(), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings("while_inactive", "", 2400, 0, true),
                List.of(new MobEffectEventEffect(MobEffects.REGENERATION, 200, 0)), List.of(), List.of("block_break"), List.of(bonusMining),
                2400, 0.004D, "duration", 200, 40);
    }

    public static AstralEventDefinition lowHealthAid() {
        return event("low_health_aid", "good", List.of("player_hurt", "entity_hurt_player"),
                List.of(new HealthEventCondition(0.0F, Float.MAX_VALUE, 0.35F)), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings("cooldown", "", 3600, 0, true),
                List.of(new HealEventEffect(4.0F), new MobEffectEventEffect(MobEffects.ABSORPTION, 200, 0)),
                List.of(), List.of(), List.of(), 3600, 0.35D, "instant", 0, 20);
    }

    public static AstralEventDefinition nightAmbush() {
        return event("night_ambush", "bad", List.of("tick"), List.of(new TimeOfDayEventCondition(13000L, 23000L)),
                List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings("cooldown", "", 3600, 0, true),
                List.of(new SummonEntityEventEffect(HolderSet.direct(EntityType.SKELETON.builtInRegistryHolder()), 2, 4.0D)),
                List.of(), List.of(), List.of(), 3600, 0.002D, "instant", 0, 20);
    }

    public static AstralEventDefinition caveCache() {
        return event("cave_cache", "good", List.of("block_break"), List.of(new PositionEventCondition(Integer.MIN_VALUE, 40, 0, 0, -1)),
                AstralEventTargetDefinition.DEFAULT, new AstralEventTriggerSettings("cooldown", "", 1200, 0, true),
                List.of(new AddExperienceEventEffect(5), new ChanceEventEffect(0.5D, new GiveItemEventEffect(Items.IRON_NUGGET.builtInRegistryHolder(), 3))),
                List.of(), List.of(), List.of(), 1200, 0.08D, "instant", 0, 20);
    }

    public static AstralEventDefinition event(
            String id, String kind, List<String> triggers, List<AstralEventCondition> conditions,
            AstralEventTargetDefinition target, AstralEventTriggerSettings triggerSettings,
            List<AstralEventEffect> effects, List<AstralEventEffect> intervalEffects,
            List<String> activeTriggers, List<AstralEventEffect> activeEffects,
            int cooldownTicks, double chance, String timing, int durationTicks, int intervalTicks) {
        return event(id, kind, triggers, conditions, List.of(), target, triggerSettings, effects, intervalEffects,
                activeTriggers, activeEffects, cooldownTicks, chance, timing, durationTicks, intervalTicks);
    }

    public static AstralEventDefinition event(
            String id, String kind, List<String> triggers,
            List<AstralEventCondition> conditions,
            List<Difficulty> difficulties,
            AstralEventTargetDefinition target,
            AstralEventTriggerSettings triggerSettings,
            List<AstralEventEffect> effects,
            List<AstralEventEffect> intervalEffects,
            List<String> activeTriggers,
            List<AstralEventEffect> activeEffects,
            int cooldownTicks, double chance, String timing,
            int durationTicks, int intervalTicks) {
        return new AstralEventDefinition(AstralCraft.prefix(id),
                "event.astral_craft." + id + ".name",
                "event.astral_craft." + id + ".description", kind,
                AstralCraft.prefix("textures/gui/cards/event_" + kind + ".png"),
                triggers, conditions, difficulties, target, triggerSettings,
                effects, intervalEffects, activeTriggers, activeEffects,
                List.of(), cooldownTicks, chance, false, timing,
                durationTicks, intervalTicks);
    }

}