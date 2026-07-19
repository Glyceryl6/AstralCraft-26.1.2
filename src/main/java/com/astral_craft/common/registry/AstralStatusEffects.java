package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.skill.effect.AstralPhaseMobEffect;
import com.astral_craft.common.gameplay.character.skill.effect.AstralStatusMobEffect;
import com.astral_craft.common.gameplay.character.skill.effect.ShadowCloakMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

public class AstralStatusEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, AstralCraft.MOD_ID);

    protected static final Map<Identifier, Holder<MobEffect>> MOB_EFFECTS_BY_STATUS = new LinkedHashMap<>();
    protected static final Map<Identifier, Identifier> DEFAULT_ICONS = new LinkedHashMap<>();
    protected static final List<MobEffectIconEntry> MOB_EFFECT_ICON_ENTRIES = new ArrayList<>();

    public static final DeferredHolder<MobEffect, MobEffect> SHADOW_CLOAK = registerMobEffect("shadow_cloak", AstralCraft.prefix("ink_shadow"), null,
            (statusId, icon, characterId) -> new ShadowCloakMobEffect(MobEffectCategory.BENEFICIAL, 0x6741B8, statusId, icon, characterId));
    public static final DeferredHolder<MobEffect, MobEffect> ASTRAL_PHASE = registerMobEffect("astral_phase", AstralCraft.prefix("astral_phase"), null,
            (statusId, icon, characterId) -> new AstralPhaseMobEffect(MobEffectCategory.BENEFICIAL, 0x8E86FF, statusId, icon, characterId));

    public static Optional<Holder<MobEffect>> get(Identifier id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(MOB_EFFECTS_BY_STATUS.get(id));
    }

    public static Optional<Identifier> statusId(MobEffectInstance instance) {
        if (instance == null) return Optional.empty();
        return statusId(instance.getEffect());
    }

    public static Optional<Identifier> statusId(Holder<MobEffect> holder) {
        if (holder == null || !(holder.value() instanceof AstralStatusMobEffect effect)) return Optional.empty();
        return Optional.ofNullable(effect.statusId());
    }

    public static boolean isAstralStatus(Holder<MobEffect> holder) {
        return holder != null && holder.value() instanceof AstralStatusMobEffect;
    }

    public static List<MobEffectIconEntry> mobEffectIconEntries() {
        return List.copyOf(MOB_EFFECT_ICON_ENTRIES);
    }

    public static List<Holder<MobEffect>> registeredEffects() {
        return List.copyOf(MOB_EFFECTS_BY_STATUS.values());
    }

    public static List<Identifier> registeredStatusIds() {
        return List.copyOf(MOB_EFFECTS_BY_STATUS.keySet());
    }

    public static Optional<Identifier> defaultIcon(Identifier statusId) {
        if (statusId == null) return Optional.empty();
        return Optional.ofNullable(DEFAULT_ICONS.get(statusId));
    }

    public static Identifier parseIdentifier(String raw, Identifier fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return raw.contains(":") ? Identifier.parse(raw) : AstralCraft.prefix(raw);
        } catch (Exception exception) {
            return fallback;
        }
    }

    protected static DeferredHolder<MobEffect, MobEffect> registerMobEffect(String path, Identifier characterId, Identifier defaultIcon, StatusMobEffectFactory factory) {
        Identifier id = AstralCraft.prefix(path);
        Identifier icon = defaultIcon == null ? mobEffectIcon(id) : defaultIcon;
        DeferredHolder<MobEffect, MobEffect> holder = MOB_EFFECTS.register(path, () -> factory.create(id, icon, characterId));
        MOB_EFFECTS_BY_STATUS.put(id, holder);
        DEFAULT_ICONS.put(id, icon);
        MOB_EFFECT_ICON_ENTRIES.add(new MobEffectIconEntry(holder, id, icon));
        return holder;
    }

    protected static Identifier mobEffectIcon(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png");
    }

    protected interface StatusMobEffectFactory {
        MobEffect create(Identifier statusId, Identifier iconTexture, Identifier characterId);
    }

    public record MobEffectIconEntry(Holder<MobEffect> holder, Identifier statusId, Identifier iconTexture) {}

}