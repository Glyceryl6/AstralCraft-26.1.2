package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.config.AstralGameplayConfig;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.network.CharacterSkillCutinPayload;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralStatusEffects;
import com.astral_craft.common.registry.AstralCharacterSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class AstralCharacterSkillService {

    public static final double PUBLIC_CUTIN_RANGE = 96.0D;
    public static final int DEFAULT_STATUS_DURATION_SECONDS = 20;

    public static void useActiveSkill(ServerPlayer player) {
        if (player == null) return;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (!state.active()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.need_character"), true);
            return;
        }

        CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId());
        Optional<CharacterSkillDefinition> maybeSkill = activeSkill(definition);
        if (maybeSkill.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.no_active"), true);
            return;
        }

        CharacterSkillDefinition skill = maybeSkill.get();
        Identifier handlerId = skill.safeHandler(definition.id());
        AstralCharacterSkillSet skillSet = AstralCharacterSkills.get(handlerId).orElseGet(() -> AstralCharacterSkills.getOrDefault(definition.id()));
        if (!skillSet.hasActiveSkill()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.no_active"), true);
            return;
        }

        String key = cooldownKey(definition, skill, handlerId);
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        int cooldown = skillState.cooldown(key);
        if (cooldown > 0 && !canBypassCooldown(player)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.cooldown", Component.translatable(displayNameKey(definition, skill)), seconds(cooldown)), true);
            return;
        }

        CharacterSkillContext context = new CharacterSkillContext(player, state, definition, skill, skillState);
        if (!skillSet.useActive(context)) return;
        int nextCooldown = cooldownTicks(skill);
        if (nextCooldown > 0 && !canBypassCooldown(player)) {
            skillState.setCooldown(key, nextCooldown);
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }

        sendCutin(player, state, definition, skill, skillSet);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used", Component.translatable(displayNameKey(definition, skill))).withStyle(ChatFormatting.AQUA), true);
    }

    public static void serverTick(ServerPlayer player) {
        if (player == null) return;
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        boolean changed = skillState.tick();
        CharacterSkillEffectState effectState = player.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        boolean effectChanged = tickEffects(player, effectState);
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (state.active()) {
            List<CharacterSkillEffect> removed = effectState.removeEffectsNotFrom(state.characterId());
            for (CharacterSkillEffect effect : removed) {
                callEffectEnd(player, effect);
            }

            effectChanged |= !removed.isEmpty();
            CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId());
            CharacterSkillDefinition skill = activeSkill(definition).orElse(new CharacterSkillDefinition("passive", 0));
            Identifier handlerId = skill.safeHandler(definition.id());
            AstralCharacterSkillSet skillSet = AstralCharacterSkills.get(handlerId).orElseGet(() -> AstralCharacterSkills.getOrDefault(definition.id()));
            skillSet.serverTick(new CharacterSkillContext(player, state, definition, skill, skillState));
        } else {
            List<CharacterSkillEffect> removed = effectState.clearAndCollectRemoved();
            for (CharacterSkillEffect effect : removed) {
                callEffectEnd(player, effect);
            }
            effectChanged |= !removed.isEmpty();
        }
        if (changed) {
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }
        if (effectChanged) {
            player.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, effectState);
        }
    }

    public static void addStatusEffect(ServerPlayer player, CharacterSkillEffect effect) {
        if (player == null || effect == null || effect.durationTicks() <= 0) return;
        CharacterSkillEffectState effectState = player.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        CharacterSkillEffect replaced = effectState.add(effect);
        if (replaced != null) {
            callEffectEnd(player, replaced);
        }

        player.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, effectState);
        callEffectStart(player, effect);
    }

    public static void addStatusEffect(LivingEntity target, CharacterSkillEffect effect) {
        if (target instanceof ServerPlayer player) {
            addStatusEffect(player, effect);
            return;
        }

        AstralCharacterSkillEffects.add(target, effect);
    }

    public static boolean hasStatusEffect(Entity target, String id) {
        Identifier statusId = AstralStatusEffects.parseIdentifier(id, null);
        return AstralCharacterSkillEffects.has(target, id) || (AstralCharacterSkillEffects.hasStatusType(target, statusId));
    }

    public static boolean hasStatusEffect(Entity target, Identifier id) {
        return AstralCharacterSkillEffects.has(target, id) || AstralCharacterSkillEffects.hasStatusType(target, id);
    }

    public static boolean hasStatusType(Entity target, Identifier id) {
        return AstralCharacterSkillEffects.hasStatusType(target, id);
    }

    public static void clearStatusEffects(ServerPlayer player) {
        if (player == null) return;
        CharacterSkillEffectState effectState = player.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        List<CharacterSkillEffect> removed = effectState.clearAndCollectRemoved();
        for (CharacterSkillEffect effect : removed) {
            callEffectEnd(player, effect);
        }

        if (!removed.isEmpty()) {
            player.setData(AstralAttachments.CHARACTER_SKILL_EFFECTS, effectState);
        }
    }

    public static void serverTickEntity(LivingEntity entity) {
        if (entity instanceof ServerPlayer) return;
        AstralCharacterSkillEffects.tickNonPlayer(entity);
    }

    public static int durationTicks(CharacterSkillDefinition skill) {
        int seconds = skill.durationSeconds();
        if (seconds <= 0) {
            seconds = DEFAULT_STATUS_DURATION_SECONDS;
        }

        seconds = Math.clamp(seconds, 1, AstralGameplayConfig.skillMaximumCooldownSeconds());
        return Math.max(1, seconds * 20);
    }

    protected static Optional<CharacterSkillDefinition> activeSkill(CharacterDefinition definition) {
        if (definition == null || definition.skills().isEmpty()) return Optional.empty();
        for (CharacterSkillDefinition skill : definition.skills()) {
            if ("active".equalsIgnoreCase(skill.id())) {
                return Optional.of(skill);
            }
        }

        for (CharacterSkillDefinition skill : definition.skills()) {
            if (skill.cooldown() > 0 || skill.pvpCooldown() > 0 || skill.pveCooldown() > 0) {
                return Optional.of(skill);
            }
        }

        return Optional.empty();
    }

    protected static boolean tickEffects(ServerPlayer player, CharacterSkillEffectState effectState) {
        CharacterSkillEffectState.TickResult result = effectState.tickAndCollectExpired();
        for (CharacterSkillEffect effect : result.ticked()) {
            callEffectTick(player, effect);
        }
        for (CharacterSkillEffect effect : result.expired()) {
            callEffectEnd(player, effect);
        }
        return result.changed();
    }

    protected static CharacterSkillContext contextForEffect(ServerPlayer player, CharacterSkillEffect effect) {
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(effect.safeCharacterId());
        CharacterSkillDefinition skill = activeSkill(definition).orElse(new CharacterSkillDefinition(effect.safeId(), 0, effect.safeHandlerId(), "skill"));
        return new CharacterSkillContext(player, state, definition, skill, player.getData(AstralAttachments.CHARACTER_SKILLS));
    }

    protected static AstralCharacterSkillSet skillSetForEffect(CharacterSkillEffect effect) {
        return AstralCharacterSkills.get(effect.safeHandlerId()).orElseGet(() -> AstralCharacterSkills.getOrDefault(effect.safeCharacterId()));
    }

    protected static void callEffectStart(ServerPlayer player, CharacterSkillEffect effect) {
        AstralStatusEffects.applyMobEffectBridge(player, effect);
        skillSetForEffect(effect).onEffectStart(contextForEffect(player, effect), effect);
    }

    protected static void callEffectTick(ServerPlayer player, CharacterSkillEffect effect) {
        skillSetForEffect(effect).onEffectTick(contextForEffect(player, effect), effect);
    }

    protected static void callEffectEnd(ServerPlayer player, CharacterSkillEffect effect) {
        AstralStatusEffects.removeMobEffectBridge(player, effect);
        skillSetForEffect(effect).onEffectEnd(contextForEffect(player, effect), effect);
    }

    protected static String cooldownKey(CharacterDefinition definition, CharacterSkillDefinition skill, Identifier handlerId) {
        return definition.id() + ":" + handlerId + ":" + skill.id();
    }

    protected static int cooldownTicks(CharacterSkillDefinition skill) {
        int rounds = skill.cooldown(CharacterSkillDefinition.SkillMode.PVP);
        if (rounds <= 0) return 0;
        int seconds = rounds * AstralGameplayConfig.skillCooldownSecondsPerRound();
        seconds = Math.clamp(seconds, AstralGameplayConfig.skillMinimumCooldownSeconds(), AstralGameplayConfig.skillMaximumCooldownSeconds());
        return Math.max(0, seconds * 20);
    }

    protected static int seconds(int ticks) {
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    protected static boolean canBypassCooldown(ServerPlayer player) {
        return player.getAbilities().instabuild;
    }

    protected static String displayNameKey(CharacterDefinition definition, CharacterSkillDefinition skill) {
        if (definition != null && skill != null) return definition.skillNameKey(skill, CharacterSkillDefinition.SkillMode.PVP);
        return "message.astral_craft.skill.default_name";
    }

    protected static String displayDescriptionKey(CharacterDefinition definition, CharacterSkillDefinition skill) {
        if (definition != null && skill != null) return definition.skillDescriptionKey(skill, CharacterSkillDefinition.SkillMode.PVP);
        return "message.astral_craft.skill.default_description";
    }

    protected static void sendCutin(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill, AstralCharacterSkillSet skillSet) {
        int duration = AstralGameplayConfig.skillCutinDurationTicks();
        if (duration <= 0) return;
        String action = skill.safeAnimationAction();
        if ("skill".equals(action) && skillSet != null) {
            action = skillSet.fallbackAnimation();
        }
        CharacterSkillCutinPayload payload = new CharacterSkillCutinPayload(definition.id().toString(), state.skinId(), skill.id(), action, duration);
        String audience = AstralGameplayConfig.skillCutinAudience();
        if ("none".equals(audience)) return;
        if ("nearby".equals(audience)) {
            double maxDistanceSqr = PUBLIC_CUTIN_RANGE * PUBLIC_CUTIN_RANGE;
            for (ServerPlayer viewer : player.level().players()) {
                if (viewer.distanceToSqr(player) <= maxDistanceSqr) {
                    PacketDistributor.sendToPlayer(viewer, payload);
                }
            }
            return;
        }

        PacketDistributor.sendToPlayer(player, payload);
    }

}