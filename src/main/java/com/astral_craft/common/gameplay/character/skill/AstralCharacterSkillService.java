package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.common.config.AstralGameplayConfig;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.network.CharacterSkillCutinPayload;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralCharacterSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
        Optional<AstralCharacterSkillSet> maybeSkillSet = AstralCharacterSkills.get(handlerId);
        if (maybeSkillSet.isEmpty() || !maybeSkillSet.get().hasActiveSkill()) {
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

        AstralCharacterSkillSet skillSet = maybeSkillSet.get();
        CharacterSkillContext context = new CharacterSkillContext(player, player, state, definition, skill, skillState);
        if (!skillSet.useActive(context)) return;
        int nextCooldown = cooldownTicks(skill);
        if (nextCooldown > 0 && !canBypassCooldown(player)) {
            skillState.setCooldown(key, nextCooldown);
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }

        sendCutin(player, state, definition, skill, skillSet);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used", Component.translatable(displayNameKey(definition, skill))).withStyle(ChatFormatting.AQUA), true);
    }


    /**
     * Executes an active skill for a board-controlled pawn. The board owns the round cooldown, so
     * this path intentionally does not write a tick cooldown into CharacterSkillState.
     *
     * @return cooldown in board turns, or -1 when the skill was rejected
     */
    public static int useActiveSkillForBoard(ServerPlayer player, LivingEntity actor, Identifier characterId, String skinId,
                                             List<ServerPlayer> viewers) {
        if (player == null || actor == null || characterId == null
                || !CharacterManager.INSTANCE.contains(characterId)) return -1;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        Optional<CharacterSkillDefinition> maybeSkill = activeSkill(definition);
        if (maybeSkill.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.no_active"), true);
            return -1;
        }
        CharacterSkillDefinition skill = maybeSkill.get();
        Identifier handlerId = skill.safeHandler(definition.id());
        Optional<AstralCharacterSkillSet> maybeSkillSet = AstralCharacterSkills.get(handlerId);
        if (maybeSkillSet.isEmpty() || !maybeSkillSet.get().hasActiveSkill()) return -1;

        var stats = com.astral_craft.common.stats.AstralStats.getOrDefault(actor);
        ActiveCharacterState state = new ActiveCharacterState(true, characterId, skinId, 1, 1,
                stats.attack(), stats.defense(), stats.maxHealth());
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        AstralCharacterSkillSet skillSet = maybeSkillSet.get();
        if (!skillSet.useActive(new CharacterSkillContext(player, actor, state, definition, skill, skillState))) return -1;
        int duration = AstralGameplayConfig.skillCutinDurationTicks();
        Identifier animation = skill.safeAnimation(skillSet.fallbackAnimation());
        if (duration > 0 && animation != null) {
            CharacterSkillCutinPayload payload = new CharacterSkillCutinPayload(definition.id(), state.skinId(),
                    skill.serializedId(), animation, duration);
            for (ServerPlayer viewer : viewers == null ? List.<ServerPlayer>of() : viewers) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }

        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used",
                Component.translatable(displayNameKey(definition, skill))).withStyle(ChatFormatting.AQUA), true);
        return Math.max(0, skill.cooldown(CharacterSkillDefinition.SkillMode.PVP) - stats.skillCooldownReduction());
    }

    public static void serverTick(ServerPlayer player) {
        if (player == null) return;
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        boolean changed = skillState.tick();
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (state.active()) {
            AstralCharacterSkillEffects.removeStatusEffectsNotOwnedByActiveCharacter(player);
            CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId());
            Optional<CharacterSkillDefinition> maybeSkill = firstSkill(definition);
            if (maybeSkill.isPresent()) {
                CharacterSkillDefinition skill = maybeSkill.get();
                Identifier handlerId = skill.safeHandler(definition.id());
                AstralCharacterSkills.get(handlerId).ifPresent(skillSet ->
                        skillSet.serverTick(new CharacterSkillContext(player, player, state, definition, skill, skillState)));
            }
        } else {
            AstralCharacterSkillEffects.clearStatusEffects(player);
        }
        if (changed) {
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }
    }

    public static boolean addStatusEffect(ServerPlayer player, Identifier statusId, int durationTicks, int amplifier) {
        return AstralCharacterSkillEffects.add(player, statusId, durationTicks, amplifier);
    }

    public static boolean hasStatusEffect(ServerPlayer player, String id) {
        Identifier statusId = CharacterSkillDefinition.parseIdentifier(id, null);
        return hasStatusEffect(player, statusId);
    }

    public static boolean hasStatusEffect(ServerPlayer player, Identifier id) {
        return AstralCharacterSkillEffects.hasStatusEffect(player, id);
    }

    public static void clearStatusEffects(ServerPlayer player) {
        AstralCharacterSkillEffects.clearStatusEffects(player);
    }

    public static int durationTicks(CharacterSkillDefinition skill) {
        int seconds = skill.durationSeconds();
        if (seconds <= 0) {
            seconds = DEFAULT_STATUS_DURATION_SECONDS;
        }

        seconds = Math.clamp(seconds, 1, AstralGameplayConfig.skillMaximumCooldownSeconds());
        return Math.max(1, seconds * 20);
    }

    public static int durationRounds(CharacterSkillDefinition skill) {
        int seconds = skill.durationSeconds();
        if (seconds <= 0) seconds = DEFAULT_STATUS_DURATION_SECONDS;
        int secondsPerRound = Math.max(1, AstralGameplayConfig.skillCooldownSecondsPerRound());
        return Math.max(1, (seconds + secondsPerRound - 1) / secondsPerRound);
    }

    protected static Optional<CharacterSkillDefinition> activeSkill(CharacterDefinition definition) {
        if (definition == null || definition.skills().isEmpty()) return Optional.empty();
        for (CharacterSkillDefinition skill : definition.skills()) {
            if (skill.id().isActive()) {
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


    protected static Optional<CharacterSkillDefinition> firstSkill(CharacterDefinition definition) {
        Optional<CharacterSkillDefinition> active = activeSkill(definition);
        if (active.isPresent()) return active;
        if (definition == null || definition.skills().isEmpty()) return Optional.empty();
        return Optional.of(definition.skills().getFirst());
    }

    protected static String cooldownKey(CharacterDefinition definition, CharacterSkillDefinition skill, Identifier handlerId) {
        return definition.id() + ":" + handlerId + ":" + skill.serializedId();
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
        Identifier animation = skill.safeAnimation(skillSet == null ? null : skillSet.fallbackAnimation());
        if (animation == null) return;
        CharacterSkillCutinPayload payload = new CharacterSkillCutinPayload(definition.id(), state.skinId(), skill.serializedId(), animation, duration);
        CharacterSkillCutinAudience audience = AstralGameplayConfig.skillCutinAudience();
        if (audience == CharacterSkillCutinAudience.NONE) return;
        if (audience.sendsToNearbyPlayers()) {
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