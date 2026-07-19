package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.*;
import com.astral_craft.common.gameplay.event.conditions.*;
import com.astral_craft.common.gameplay.event.effects.*;
import com.astral_craft.common.gameplay.event.type.AstralEventKinds;
import com.astral_craft.common.gameplay.event.type.AstralEventLocalizationKeys;
import com.astral_craft.common.gameplay.event.type.AstralEventRepeatModes;
import com.astral_craft.common.gameplay.event.type.AstralEventTimings;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
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
        return event("lucky_find", AstralEventKinds.GOOD, false, List.of(),
                AstralEventTargetDefinition.DEFAULT, AstralEventTriggerSettings.DEFAULT,
                List.of(), List.of(), List.of(new BlockBreakEventCondition()),
                List.of(new GiveItemEventEffect(Items.EMERALD.builtInRegistryHolder(), 1)),
                600, 0.2D, AstralEventTimings.DURATION, 1200, 20);
    }

    public static AstralEventDefinition ambush() {
        return event("ambush", AstralEventKinds.BAD, false, List.of(),
                List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings(AstralEventRepeatModes.COOLDOWN, "", 2400, 0, true),
                List.of(new SummonEntityEventEffect(HolderSet.direct(EntityType.ZOMBIE.builtInRegistryHolder()), 1)),
                List.of(), List.of(), List.of(), 2400, 0.004D, AstralEventTimings.INSTANT, 0, 20);
    }

    public static AstralEventDefinition astralBlessing() {
        AstralEventEffect bonusMining = new ChanceEventEffect(0.25D, new GiveItemEventEffect(Items.LAPIS_LAZULI.builtInRegistryHolder(), 1));
        return event("astral_blessing", AstralEventKinds.GOOD, false, List.of(), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings(AstralEventRepeatModes.WHILE_INACTIVE, "", 2400, 0, true),
                List.of(new MobEffectEventEffect(MobEffects.REGENERATION, 200, 0)), List.of(), List.of(new BlockBreakEventCondition()), List.of(bonusMining),
                2400, 0.004D, AstralEventTimings.DURATION, 200, 40);
    }

    public static AstralEventDefinition lowHealthAid() {
        return event("low_health_aid", AstralEventKinds.GOOD, false, List.of(), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings(AstralEventRepeatModes.COOLDOWN, "", 3600, 0, true),
                List.of(), List.of(),
                List.of(new ActiveAnyOfEventCondition(List.of(new PlayerHurtEventCondition(), new EntityHurtPlayerEventCondition())),
                        new HealthEventCondition(0.0F, Float.MAX_VALUE, 0.35F)),
                List.of(new HealEventEffect(4.0F), new MobEffectEventEffect(MobEffects.ABSORPTION, 200, 0)),
                3600, 0.35D, AstralEventTimings.DURATION, 1200, 20);
    }

    public static AstralEventDefinition nightAmbush() {
        return event("night_ambush", AstralEventKinds.BAD, false, List.of(new TimeOfDayEventCondition(13000L, 23000L)),
                List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD), AstralEventTargetDefinition.DEFAULT,
                new AstralEventTriggerSettings(AstralEventRepeatModes.COOLDOWN, "", 3600, 0, true),
                List.of(new SummonEntityEventEffect(HolderSet.direct(EntityType.SKELETON.builtInRegistryHolder()), 2, 4.0D)),
                List.of(), List.of(), List.of(), 3600, 0.002D, AstralEventTimings.INSTANT, 0, 20);
    }

    public static AstralEventDefinition caveCache() {
        return event("cave_cache", AstralEventKinds.GOOD, false, List.of(),
                AstralEventTargetDefinition.DEFAULT, new AstralEventTriggerSettings(AstralEventRepeatModes.COOLDOWN, "", 1200, 0, true),
                List.of(), List.of(), List.of(new BlockBreakEventCondition(), new PositionEventCondition(Integer.MIN_VALUE, 40, 0, 0, -1)),
                List.of(new AddExperienceEventEffect(5), new ChanceEventEffect(0.5D, new GiveItemEventEffect(Items.IRON_NUGGET.builtInRegistryHolder(), 3))),
                1200, 0.08D, AstralEventTimings.DURATION, 1200, 20);
    }

    public static AstralEventDefinition event(
            String id, Identifier kind, boolean triggers, List<AstralEventTriggerCondition> conditions,
            AstralEventTargetDefinition target, AstralEventTriggerSettings triggerSettings,
            List<AstralEventEffect> effects, List<AstralEventEffect> intervalEffects,
            List<AstralActiveEventCondition> activeConditions, List<AstralEventEffect> activeEffects,
            int cooldownTicks, double chance, Identifier timing, int durationTicks, int intervalTicks) {
        return event(id, kind, triggers, conditions, List.of(), target, triggerSettings, effects, intervalEffects,
                activeConditions, activeEffects, cooldownTicks, chance, timing, durationTicks, intervalTicks);
    }

    public static AstralEventDefinition event(
            String id, Identifier kind, boolean triggers,
            List<AstralEventTriggerCondition> conditions,
            List<Difficulty> difficulties,
            AstralEventTargetDefinition target,
            AstralEventTriggerSettings triggerSettings,
            List<AstralEventEffect> effects,
            List<AstralEventEffect> intervalEffects,
            List<AstralActiveEventCondition> activeConditions,
            List<AstralEventEffect> activeEffects,
            int cooldownTicks, double chance, Identifier timing,
            int durationTicks, int intervalTicks) {
        Identifier eventId = AstralCraft.prefix(id);
        return new AstralEventDefinition(eventId,
                AstralEventLocalizationKeys.name(eventId),
                AstralEventLocalizationKeys.description(eventId), kind,
                AstralEventKinds.texture(kind),
                triggers, conditions, difficulties, target, triggerSettings,
                effects, intervalEffects, activeConditions, activeEffects,
                List.of(), cooldownTicks, chance, false, timing,
                durationTicks, intervalTicks);
    }

}
