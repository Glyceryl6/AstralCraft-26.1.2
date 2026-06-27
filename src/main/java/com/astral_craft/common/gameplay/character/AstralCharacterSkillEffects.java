package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AstralCharacterSkillEffects {

    public static CharacterSkillEffectState state(Entity entity) {
        if (entity == null) return CharacterSkillEffectState.empty();
        return entity.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
    }

    public static List<CharacterSkillEffect> activeEffects(Entity entity) {
        return state(entity).activeEffects();
    }

    public static boolean has(Entity entity, String id) {
        return id != null && !id.isBlank() && state(entity).contains(id);
    }

    public static boolean has(Entity entity, Identifier id) {
        return id != null && has(entity, id.toString());
    }

    public static boolean hasFromCharacter(Entity entity, Identifier characterId) {
        return characterId != null && first(entity, effect -> characterId.equals(effect.safeCharacterId())).isPresent();
    }

    public static boolean hasHandler(Entity entity, Identifier handlerId) {
        return handlerId != null && first(entity, effect -> handlerId.equals(effect.safeHandlerId())).isPresent();
    }

    public static boolean hasStatusType(Entity entity, Identifier statusId) {
        return statusId != null && first(entity, effect -> statusId.equals(AstralStatusEffects.statusId(effect))).isPresent();
    }

    public static Optional<CharacterSkillEffect> first(Entity entity, Predicate<CharacterSkillEffect> predicate) {
        if (predicate == null) return Optional.empty();
        for (CharacterSkillEffect effect : activeEffects(entity)) {
            if (predicate.test(effect)) {
                return Optional.of(effect);
            }
        }
        return Optional.empty();
    }

    public static void add(LivingEntity target, CharacterSkillEffect effect) {
        if (target == null || effect == null || effect.durationTicks() <= 0) return;
        CharacterSkillEffectState state = target.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        CharacterSkillEffect replaced = state.add(effect);
        if (replaced != null) {
            AstralStatusEffects.removeMobEffectBridge(target, replaced);
        }
        target.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, state);
        AstralStatusEffects.applyMobEffectBridge(target, effect);
    }

    public static boolean remove(Entity entity, String id) {
        if (entity == null || id == null || id.isBlank()) return false;
        CharacterSkillEffectState state = entity.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        CharacterSkillEffect removed = state.remove(id);
        if (removed != null) {
            if (entity instanceof LivingEntity livingEntity) {
                AstralStatusEffects.removeMobEffectBridge(livingEntity, removed);
            }
            entity.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, state);
            return true;
        }
        return false;
    }

    public static boolean clear(Entity entity) {
        if (entity == null) return false;
        CharacterSkillEffectState state = entity.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        List<CharacterSkillEffect> removed = state.clearAndCollectRemoved();
        if (!removed.isEmpty()) {
            if (entity instanceof LivingEntity livingEntity) {
                for (CharacterSkillEffect effect : removed) {
                    AstralStatusEffects.removeMobEffectBridge(livingEntity, effect);
                }
            }
            entity.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, state);
        }
        return !removed.isEmpty();
    }

    public static void tickNonPlayer(LivingEntity entity) {
        if (entity == null) return;
        CharacterSkillEffectState state = entity.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        CharacterSkillEffectState.TickResult result = state.tickAndCollectExpired();
        if (result.changed()) {
            for (CharacterSkillEffect effect : result.expired()) {
                AstralStatusEffects.removeMobEffectBridge(entity, effect);
            }
            entity.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, state);
        }
    }

}
