package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.CharacterSkillEffect;
import com.astral_craft.common.gameplay.character.status.AstralStatusEffectType;
import com.astral_craft.common.gameplay.character.status.AstralStatusMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AstralStatusEffects {

    public static final ResourceKey<Registry<AstralStatusEffectType>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("status_effects"));
    public static final DeferredRegister<AstralStatusEffectType> STATUS_EFFECTS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<AstralStatusEffectType> REGISTRY = STATUS_EFFECTS.makeRegistry(_ -> {});
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, AstralCraft.MOD_ID);
    protected static final List<MobEffectIconEntry> MOB_EFFECT_ICON_ENTRIES = new ArrayList<>();

    public static final Identifier PROPERTY_STATUS = AstralCraft.prefix("status");
    public static final Identifier PROPERTY_ICON = AstralCraft.prefix("icon");
    public static final Identifier PROPERTY_SKIN = AstralCraft.prefix("skin");
    public static final Identifier GENERIC_STATUS_ID = AstralCraft.prefix("generic_status");
    public static final Identifier RECOVERY_PULSE_ID = AstralCraft.prefix("recovery_pulse");
    public static final Identifier PASSIVE_RECOVERY_ID = AstralCraft.prefix("passive_recovery");
    public static final Identifier SNACK_TIME_ID = AstralCraft.prefix("snack_time");
    public static final Identifier ATTACK_PULSE_ID = AstralCraft.prefix("attack_pulse");
    public static final Identifier ARMOR_PULSE_ID = AstralCraft.prefix("armor_pulse");
    public static final Identifier SHADOW_CLOAK_ID = AstralCraft.prefix("shadow_cloak");

    public static final DeferredHolder<MobEffect, MobEffect> GENERIC_STATUS_MOB = registerMobEffect("generic_status", MobEffectCategory.NEUTRAL, 0xD1FE00);

    public static final DeferredHolder<AstralStatusEffectType, AstralStatusEffectType> GENERIC_STATUS = register("generic_status", () -> GENERIC_STATUS_MOB, null);

    public static DeferredHolder<AstralStatusEffectType, AstralStatusEffectType> register(String path, Supplier<Holder<MobEffect>> mobEffect, Identifier defaultIcon) {
        Identifier id = AstralCraft.prefix(path);
        return STATUS_EFFECTS.register(path, () -> new AstralStatusEffectType(id, mobEffect, defaultIcon));
    }

    public static Optional<AstralStatusEffectType> get(Identifier id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.getValue(id));
    }

    public static AstralStatusEffectType getOrDefault(Identifier id) {
        return get(id).orElseGet(GENERIC_STATUS);
    }

    public static List<MobEffectIconEntry> mobEffectIconEntries() {
        return List.copyOf(MOB_EFFECT_ICON_ENTRIES);
    }

    public static Identifier statusId(CharacterSkillEffect effect) {
        if (effect == null) return GENERIC_STATUS_ID;
        Identifier explicit = parseIdentifier(effect.property(PROPERTY_STATUS), null);
        if (explicit != null) return explicit;
        return effect.safeIdAsIdentifier();
    }

    public static Optional<Holder<MobEffect>> mobEffect(CharacterSkillEffect effect) {
        return getOrDefault(statusId(effect)).mobEffect();
    }

    public static void applyMobEffectBridge(LivingEntity target, CharacterSkillEffect effect) {
        if (target == null || effect == null || effect.durationTicks() <= 0) return;
        mobEffect(effect).ifPresent(holder -> target.addEffect(new MobEffectInstance(holder, effect.durationTicks(), effect.amplifier(), true, true, true)));
    }

    public static void removeMobEffectBridge(LivingEntity target, CharacterSkillEffect effect) {
        if (target == null || effect == null) return;
        mobEffect(effect).ifPresent(target::removeEffect);
    }

    public static boolean has(LivingEntity target, Identifier statusId) {
        if (target == null || statusId == null) return false;
        Optional<Holder<MobEffect>> holder = getOrDefault(statusId).mobEffect();
        return holder.isPresent() && target.hasEffect(holder.get());
    }

    public static Optional<Identifier> configuredIcon(CharacterSkillEffect effect) {
        if (effect == null) return Optional.empty();
        Identifier explicit = parseIdentifier(effect.property(PROPERTY_ICON), null);
        if (explicit != null) return Optional.of(explicit);
        return defaultIcon(statusId(effect));
    }

    public static Optional<Identifier> defaultIcon(Identifier statusId) {
        return getOrDefault(statusId).defaultIcon();
    }

    public static Identifier parseIdentifier(String raw, Identifier fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return raw.contains(":") ? Identifier.parse(raw) : AstralCraft.prefix(raw);
        } catch (Exception exception) {
            return fallback;
        }
    }

    public static String propertyKey(Identifier id) {
        return id == null ? "" : id.toString();
    }

    protected static DeferredHolder<MobEffect, MobEffect> registerMobEffect(String path, MobEffectCategory category, int color) {
        Identifier id = AstralCraft.prefix(path);
        Identifier icon = mobEffectIcon(id);
        DeferredHolder<MobEffect, MobEffect> holder = MOB_EFFECTS.register(path, () -> new AstralStatusMobEffect(category, color, id, icon));
        MOB_EFFECT_ICON_ENTRIES.add(new MobEffectIconEntry(holder, id, icon));
        return holder;
    }

    protected static Identifier mobEffectIcon(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png");
    }

    public record MobEffectIconEntry(Holder<MobEffect> holder, Identifier statusId, Identifier iconTexture) {}

}