package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralActiveEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventTriggerCondition;
import com.astral_craft.common.gameplay.event.conditions.ActiveAllOfEventCondition;
import com.astral_craft.common.gameplay.event.conditions.ActiveAnyOfEventCondition;
import com.astral_craft.common.gameplay.event.conditions.ActiveEventCondition;
import com.astral_craft.common.gameplay.event.conditions.ActiveNotEventCondition;
import com.astral_craft.common.gameplay.event.conditions.BlockAtEventCondition;
import com.astral_craft.common.gameplay.event.conditions.BlockBreakEventCondition;
import com.astral_craft.common.gameplay.event.conditions.DimensionEventCondition;
import com.astral_craft.common.gameplay.event.conditions.EntityCategoryEventCondition;
import com.astral_craft.common.gameplay.event.conditions.EntityHurtPlayerEventCondition;
import com.astral_craft.common.gameplay.event.conditions.EntityTypeEventCondition;
import com.astral_craft.common.gameplay.event.conditions.HasItemEventCondition;
import com.astral_craft.common.gameplay.event.conditions.HealthEventCondition;
import com.astral_craft.common.gameplay.event.conditions.MobEffectEventCondition;
import com.astral_craft.common.gameplay.event.conditions.NoopEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PlayerHurtEntityEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PlayerHurtEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PlayerKilledEntityEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PlayerKilledEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PositionEventCondition;
import com.astral_craft.common.gameplay.event.conditions.RandomChanceEventCondition;
import com.astral_craft.common.gameplay.event.conditions.TimeOfDayEventCondition;
import com.astral_craft.common.gameplay.event.conditions.TriggerAllOfEventCondition;
import com.astral_craft.common.gameplay.event.conditions.TriggerAnyOfEventCondition;
import com.astral_craft.common.gameplay.event.conditions.TriggerNotEventCondition;
import com.astral_craft.common.gameplay.event.conditions.WeatherEventCondition;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class AstralEventConditionTypes {

    public static final ResourceKey<Registry<MapCodec<? extends AstralEventTriggerCondition>>> TRIGGER_REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_event_trigger_condition_types"));
    public static final ResourceKey<Registry<MapCodec<? extends AstralActiveEventCondition>>> ACTIVE_REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_active_event_condition_types"));

    public static final DeferredRegister<MapCodec<? extends AstralEventTriggerCondition>> TRIGGER_CONDITION_TYPES = DeferredRegister.create(TRIGGER_REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends AstralActiveEventCondition>> ACTIVE_CONDITION_TYPES = DeferredRegister.create(ACTIVE_REGISTRY_KEY, AstralCraft.MOD_ID);

    public static final Registry<MapCodec<? extends AstralEventTriggerCondition>> TRIGGER_REGISTRY = TRIGGER_CONDITION_TYPES.makeRegistry(_ -> {});
    public static final Registry<MapCodec<? extends AstralActiveEventCondition>> ACTIVE_REGISTRY = ACTIVE_CONDITION_TYPES.makeRegistry(_ -> {});

    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<NoopEventCondition>> TRIGGER_NOOP = registerTrigger("noop", NoopEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<NoopEventCondition>> ACTIVE_NOOP = registerActive("noop", NoopEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<DimensionEventCondition>> TRIGGER_DIMENSION = registerTrigger("dimension", DimensionEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<DimensionEventCondition>> ACTIVE_DIMENSION = registerActive("dimension", DimensionEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<PositionEventCondition>> TRIGGER_POSITION = registerTrigger("position", PositionEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<PositionEventCondition>> ACTIVE_POSITION = registerActive("position", PositionEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<EntityTypeEventCondition>> TRIGGER_ENTITY_TYPE = registerTrigger("entity_type", EntityTypeEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<EntityTypeEventCondition>> ACTIVE_ENTITY_TYPE = registerActive("entity_type", EntityTypeEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<EntityCategoryEventCondition>> TRIGGER_ENTITY_CATEGORY = registerTrigger("entity_category", EntityCategoryEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<EntityCategoryEventCondition>> ACTIVE_ENTITY_CATEGORY = registerActive("entity_category", EntityCategoryEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<RandomChanceEventCondition>> TRIGGER_RANDOM_CHANCE = registerTrigger("random_chance", RandomChanceEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<RandomChanceEventCondition>> ACTIVE_RANDOM_CHANCE = registerActive("random_chance", RandomChanceEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<HealthEventCondition>> TRIGGER_HEALTH = registerTrigger("health", HealthEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<HealthEventCondition>> ACTIVE_HEALTH = registerActive("health", HealthEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<HasItemEventCondition>> TRIGGER_HAS_ITEM = registerTrigger("has_item", HasItemEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<HasItemEventCondition>> ACTIVE_HAS_ITEM = registerActive("has_item", HasItemEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<MobEffectEventCondition>> TRIGGER_HAS_EFFECT = registerTrigger("has_effect", MobEffectEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<MobEffectEventCondition>> ACTIVE_HAS_EFFECT = registerActive("has_effect", MobEffectEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<WeatherEventCondition>> TRIGGER_WEATHER = registerTrigger("weather", WeatherEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<WeatherEventCondition>> ACTIVE_WEATHER = registerActive("weather", WeatherEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<TimeOfDayEventCondition>> TRIGGER_TIME_OF_DAY = registerTrigger("time_of_day", TimeOfDayEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<TimeOfDayEventCondition>> ACTIVE_TIME_OF_DAY = registerActive("time_of_day", TimeOfDayEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<BlockAtEventCondition>> TRIGGER_BLOCK_AT = registerTrigger("block_at", BlockAtEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<BlockAtEventCondition>> ACTIVE_BLOCK_AT = registerActive("block_at", BlockAtEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<ActiveEventCondition>> TRIGGER_ACTIVE_EVENT = registerTrigger("active_event", ActiveEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<ActiveEventCondition>> ACTIVE_ACTIVE_EVENT = registerActive("active_event", ActiveEventCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<TriggerAllOfEventCondition>> TRIGGER_ALL_OF = registerTrigger("all_of", TriggerAllOfEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<ActiveAllOfEventCondition>> ACTIVE_ALL_OF = registerActive("all_of", ActiveAllOfEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<TriggerAnyOfEventCondition>> TRIGGER_ANY_OF = registerTrigger("any_of", TriggerAnyOfEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<ActiveAnyOfEventCondition>> ACTIVE_ANY_OF = registerActive("any_of", ActiveAnyOfEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<TriggerNotEventCondition>> TRIGGER_NOT = registerTrigger("not", TriggerNotEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<ActiveNotEventCondition>> ACTIVE_NOT = registerActive("not", ActiveNotEventCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<BlockBreakEventCondition>> ACTIVE_BLOCK_BREAK = registerActive("block_break", BlockBreakEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<PlayerHurtEventCondition>> ACTIVE_PLAYER_HURT = registerActive("player_hurt", PlayerHurtEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<EntityHurtPlayerEventCondition>> ACTIVE_ENTITY_HURT_PLAYER = registerActive("entity_hurt_player", EntityHurtPlayerEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<PlayerHurtEntityEventCondition>> ACTIVE_PLAYER_HURT_ENTITY = registerActive("player_hurt_entity", PlayerHurtEntityEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<PlayerKilledEventCondition>> ACTIVE_PLAYER_KILLED = registerActive("player_killed", PlayerKilledEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<PlayerKilledEntityEventCondition>> ACTIVE_PLAYER_KILLED_ENTITY = registerActive("player_killed_entity", PlayerKilledEntityEventCondition.CODEC);

    public static <T extends AstralEventTriggerCondition> DeferredHolder<MapCodec<? extends AstralEventTriggerCondition>, MapCodec<T>> registerTrigger(String path, MapCodec<T> codec) {
        return TRIGGER_CONDITION_TYPES.register(path, () -> codec);
    }

    public static <T extends AstralActiveEventCondition> DeferredHolder<MapCodec<? extends AstralActiveEventCondition>, MapCodec<T>> registerActive(String path, MapCodec<T> codec) {
        return ACTIVE_CONDITION_TYPES.register(path, () -> codec);
    }

    public static <T extends AstralEventGeneralCondition> void registerBoth(String path, MapCodec<T> codec) {
        registerTrigger(path, codec);
        registerActive(path, codec);
    }

    public static DeferredRegister<MapCodec<? extends AstralEventTriggerCondition>> createTriggerRegister(String modId) {
        return DeferredRegister.create(TRIGGER_REGISTRY_KEY, modId);
    }

    public static DeferredRegister<MapCodec<? extends AstralActiveEventCondition>> createActiveRegister(String modId) {
        return DeferredRegister.create(ACTIVE_REGISTRY_KEY, modId);
    }

}