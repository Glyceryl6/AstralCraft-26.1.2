package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.conditions.ActiveEventCondition;
import com.astral_craft.common.gameplay.event.conditions.AllOfEventCondition;
import com.astral_craft.common.gameplay.event.conditions.AnyOfEventCondition;
import com.astral_craft.common.gameplay.event.conditions.BlockAtEventCondition;
import com.astral_craft.common.gameplay.event.conditions.DimensionEventCondition;
import com.astral_craft.common.gameplay.event.conditions.EntityCategoryEventCondition;
import com.astral_craft.common.gameplay.event.conditions.EntityTypeEventCondition;
import com.astral_craft.common.gameplay.event.conditions.HasItemEventCondition;
import com.astral_craft.common.gameplay.event.conditions.HealthEventCondition;
import com.astral_craft.common.gameplay.event.conditions.MobEffectEventCondition;
import com.astral_craft.common.gameplay.event.conditions.NoopEventCondition;
import com.astral_craft.common.gameplay.event.conditions.NotEventCondition;
import com.astral_craft.common.gameplay.event.conditions.PositionEventCondition;
import com.astral_craft.common.gameplay.event.conditions.RandomChanceEventCondition;
import com.astral_craft.common.gameplay.event.conditions.TimeOfDayEventCondition;
import com.astral_craft.common.gameplay.event.conditions.WeatherEventCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralEventConditionTypes {

    public static final ResourceKey<Registry<MapCodec<? extends AstralEventCondition>>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_event_condition_types"));
    public static final DeferredRegister<MapCodec<? extends AstralEventCondition>> CONDITION_TYPES = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<MapCodec<? extends AstralEventCondition>> REGISTRY = CONDITION_TYPES.makeRegistry(_ -> {});

    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<NoopEventCondition>> NOOP = register("noop", NoopEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<DimensionEventCondition>> DIMENSION = register("dimension", DimensionEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<PositionEventCondition>> POSITION = register("position", PositionEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<EntityTypeEventCondition>> ENTITY_TYPE = register("entity_type", EntityTypeEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<EntityCategoryEventCondition>> ENTITY_CATEGORY = register("entity_category", EntityCategoryEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<RandomChanceEventCondition>> RANDOM_CHANCE = register("random_chance", RandomChanceEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<HealthEventCondition>> HEALTH = register("health", HealthEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<HasItemEventCondition>> HAS_ITEM = register("has_item", HasItemEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<MobEffectEventCondition>> HAS_EFFECT = register("has_effect", MobEffectEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<WeatherEventCondition>> WEATHER = register("weather", WeatherEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<TimeOfDayEventCondition>> TIME_OF_DAY = register("time_of_day", TimeOfDayEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<BlockAtEventCondition>> BLOCK_AT = register("block_at", BlockAtEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<ActiveEventCondition>> ACTIVE_EVENT = register("active_event", ActiveEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<AllOfEventCondition>> ALL_OF = register("all_of", AllOfEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<AnyOfEventCondition>> ANY_OF = register("any_of", AnyOfEventCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<NotEventCondition>> NOT = register("not", NotEventCondition.CODEC);

    public static <T extends AstralEventCondition> DeferredHolder<MapCodec<? extends AstralEventCondition>, MapCodec<T>> register(String path, MapCodec<T> codec) {
        return CONDITION_TYPES.register(path, () -> codec);
    }

    public static DeferredRegister<MapCodec<? extends AstralEventCondition>> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Codec<? extends AstralEventCondition> codecFor(String rawId) {
        Identifier id = parse(rawId);
        MapCodec<? extends AstralEventCondition> codec = REGISTRY.getValue(id);
        if (codec == null) {
            codec = NoopEventCondition.CODEC;
        }
        return codec.codec();
    }

    public static Identifier parse(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return AstralCraft.prefix("noop");
        }
        try {
            return rawId.contains(":") ? Identifier.parse(rawId) : AstralCraft.prefix(rawId);
        } catch (Exception ignored) {
            return AstralCraft.prefix("noop");
        }
    }

    private AstralEventConditionTypes() {}
}
