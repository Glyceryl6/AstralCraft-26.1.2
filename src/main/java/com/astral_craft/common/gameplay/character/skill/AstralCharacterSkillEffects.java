package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.gameplay.character.skill.effect.AstralStatusMobEffect;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Optional;

public class AstralCharacterSkillEffects {

    public static boolean add(LivingEntity target, Identifier statusId, int durationTicks, int amplifier) {
        if (target == null || statusId == null || durationTicks <= 0) return false;
        Optional<Holder<MobEffect>> mobEffect = AstralStatusEffects.get(statusId);
        if (mobEffect.isEmpty()) return false;
        MobEffect effect = mobEffect.get().value();
        if (effect instanceof AstralStatusMobEffect statusEffect && !statusEffect.canApplyTo(target)) return false;
        return target.addEffect(new MobEffectInstance(mobEffect.get(), durationTicks, amplifier, true, true, true));
    }

    public static boolean hasStatusEffect(ServerPlayer player, Identifier statusId) {
        if (player == null || statusId == null) return false;
        Optional<Holder<MobEffect>> mobEffect = AstralStatusEffects.get(statusId);
        if (mobEffect.isEmpty() || !player.hasEffect(mobEffect.get())) return false;
        MobEffect effect = mobEffect.get().value();
        return !(effect instanceof AstralStatusMobEffect statusEffect) || statusEffect.canApplyTo(player);
    }

    public static void clearStatusEffects(ServerPlayer player) {
        for (Holder<MobEffect> holder : AstralStatusEffects.registeredEffects()) {
            player.removeEffect(holder);
        }
    }

    public static void removeStatusEffectsNotOwnedByActiveCharacter(ServerPlayer player) {
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            MobEffect effect = instance.getEffect().value();
            if (effect instanceof AstralStatusMobEffect statusEffect && !statusEffect.canApplyTo(player)) {
                player.removeEffect(instance.getEffect());
            }
        }
    }

}