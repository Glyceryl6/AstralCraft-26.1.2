package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.Optional;

public class AstralCharacterSkillEffects {

    public static void add(LivingEntity target, CharacterSkillEffect effect) {
        if (target == null || effect == null || effect.durationTicks() <= 0) return;
        if (target instanceof ServerPlayer player && !canReceive(player, effect.characterId(), effect.statusId())) return;
        Optional<Holder<MobEffect>> mobEffect = AstralStatusEffects.get(effect.statusId());
        mobEffect.ifPresent(holder -> target.addEffect(new MobEffectInstance(holder, effect.durationTicks(), effect.amplifier(), true, true, true)));
    }

    public static boolean hasStatusEffect(ServerPlayer player, Identifier statusId) {
        if (player == null || statusId == null) return false;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        Optional<Holder<MobEffect>> mobEffect = AstralStatusEffects.get(statusId);
        return state.active() && canReceive(player, state.characterId(), statusId) && mobEffect.isPresent() && player.hasEffect(mobEffect.get());
    }

    public static boolean canReceive(ServerPlayer player, Identifier characterId, Identifier statusId) {
        if (player == null || characterId == null || statusId == null) return false;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active() || !characterId.equals(state.characterId())) return false;
        return characterDefinesStatus(characterId, statusId);
    }

    public static boolean characterDefinesStatus(Identifier characterId, Identifier statusId) {
        if (characterId == null || statusId == null || !CharacterManager.INSTANCE.contains(characterId)) return false;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        for (CharacterSkillDefinition skill : definition.skills()) {
            Optional<Identifier> configured = skill.statusEffectId();
            if (configured.isPresent() && statusId.equals(configured.get())) return true;
        }

        return false;
    }

    public static void clearStatusEffects(ServerPlayer player) {
        if (player == null) return;
        for (Identifier statusId : AstralStatusEffects.registeredStatusIds()) {
            AstralStatusEffects.get(statusId).ifPresent(player::removeEffect);
        }
    }

    public static void removeStatusEffectsNotOwnedByActiveCharacter(ServerPlayer player) {
        if (player == null) return;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active()) {
            clearStatusEffects(player);
            return;
        }

        for (Identifier statusId : AstralStatusEffects.registeredStatusIds()) {
            Optional<Holder<MobEffect>> mobEffect = AstralStatusEffects.get(statusId);
            if (mobEffect.isPresent() && player.hasEffect(mobEffect.get()) && !characterDefinesStatus(state.characterId(), statusId)) {
                player.removeEffect(mobEffect.get());
            }
        }
    }

    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance instance = event.getEffectInstance();
        AstralStatusEffects.statusId(instance).ifPresent(statusId -> {
            if (!hasConfiguredOwner(player, statusId)) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        });
    }

    protected static boolean hasConfiguredOwner(ServerPlayer player, Identifier statusId) {
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        return state.active() && characterDefinesStatus(state.characterId(), statusId);
    }

}
