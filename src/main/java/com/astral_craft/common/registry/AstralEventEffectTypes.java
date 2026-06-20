package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.event.effects.AddExperienceEventEffect;
import com.astral_craft.common.gameplay.event.effects.AddHungerEventEffect;
import com.astral_craft.common.gameplay.event.effects.ChanceEventEffect;
import com.astral_craft.common.gameplay.event.effects.ClearMobEffectEventEffect;
import com.astral_craft.common.gameplay.event.effects.CommandEventEffect;
import com.astral_craft.common.gameplay.event.effects.DamageEventEffect;
import com.astral_craft.common.gameplay.event.effects.DropItemEventEffect;
import com.astral_craft.common.gameplay.event.effects.GainSelectedCharacterFriendshipEventEffect;
import com.astral_craft.common.gameplay.event.effects.GiveItemEventEffect;
import com.astral_craft.common.gameplay.event.effects.HealEventEffect;
import com.astral_craft.common.gameplay.event.effects.MobEffectEventEffect;
import com.astral_craft.common.gameplay.event.effects.NoopEventEffect;
import com.astral_craft.common.gameplay.event.effects.SetFireEventEffect;
import com.astral_craft.common.gameplay.event.effects.SummonEntityEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralEventEffectTypes {

    public static final ResourceKey<Registry<MapCodec<? extends AstralEventEffect>>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_event_effect_types"));
    public static final DeferredRegister<MapCodec<? extends AstralEventEffect>> EFFECT_TYPES = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<MapCodec<? extends AstralEventEffect>> REGISTRY = EFFECT_TYPES.makeRegistry(_ -> {});

    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<NoopEventEffect>> NOOP = register("noop", NoopEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<ChanceEventEffect>> CHANCE = register("chance", ChanceEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<GiveItemEventEffect>> GIVE_ITEM = register("give_item", GiveItemEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<DropItemEventEffect>> DROP_ITEM = register("drop_item", DropItemEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<SummonEntityEventEffect>> SUMMON_ENTITY = register("summon_entity", SummonEntityEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<MobEffectEventEffect>> MOB_EFFECT = register("effect", MobEffectEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<ClearMobEffectEventEffect>> CLEAR_MOB_EFFECT = register("clear_effect", ClearMobEffectEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<DamageEventEffect>> DAMAGE = register("damage", DamageEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<HealEventEffect>> HEAL = register("heal", HealEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<AddExperienceEventEffect>> ADD_EXPERIENCE = register("add_experience", AddExperienceEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<AddHungerEventEffect>> ADD_HUNGER = register("add_hunger", AddHungerEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<SetFireEventEffect>> SET_FIRE = register("set_fire", SetFireEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<CommandEventEffect>> COMMAND = register("command", CommandEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<GainSelectedCharacterFriendshipEventEffect>> GAIN_SELECTED_CHARACTER_FRIENDSHIP = register("gain_selected_character_friendship", GainSelectedCharacterFriendshipEventEffect.CODEC);

    public static <T extends AstralEventEffect> DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<T>> register(String path, MapCodec<T> codec) {
        return EFFECT_TYPES.register(path, () -> codec);
    }

    public static DeferredRegister<MapCodec<? extends AstralEventEffect>> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Codec<? extends AstralEventEffect> codecFor(String rawId) {
        Identifier id = parse(rawId);
        MapCodec<? extends AstralEventEffect> codec = REGISTRY.getValue(id);
        if (codec == null) {
            codec = NoopEventEffect.CODEC;
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

    public static String id(Identifier id) {
        return id.toString();
    }

}