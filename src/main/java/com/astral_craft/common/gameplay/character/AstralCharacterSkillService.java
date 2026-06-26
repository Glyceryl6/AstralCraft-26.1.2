package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.config.AstralGameplayConfig;
import com.astral_craft.common.network.CharacterSkillCutinPayload;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralCharacterSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public class AstralCharacterSkillService {

    public static final double PUBLIC_CUTIN_RANGE = 96.0D;

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

        String key = thisCooldownKey(definition, skill, handlerId);
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        int cooldown = skillState.cooldown(key);
        if (cooldown > 0 && !canBypassCooldown(player)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.cooldown", Component.translatable(displayNameKey(skill)), seconds(cooldown)), true);
            return;
        }

        CharacterSkillContext context = new CharacterSkillContext(player, state, definition, skill, skillState);
        if (!skillSet.useActive(context)) {
            return;
        }

        int nextCooldown = cooldownTicks(skill);
        if (nextCooldown > 0 && !canBypassCooldown(player)) {
            skillState.setCooldown(key, nextCooldown);
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }
        sendCutin(player, state, definition, skill, skillSet);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used", Component.translatable(displayNameKey(skill))).withStyle(ChatFormatting.AQUA), true);
    }

    public static void serverTick(ServerPlayer player) {
        if (player == null) return;
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        boolean changed = skillState.tick();
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (state.active()) {
            CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId());
            Optional<CharacterSkillDefinition> maybeSkill = activeSkill(definition);
            CharacterSkillDefinition skill = maybeSkill.orElse(new CharacterSkillDefinition("passive", "", "", 0));
            Identifier handlerId = skill.safeHandler(definition.id());
            AstralCharacterSkillSet skillSet = AstralCharacterSkills.get(handlerId).orElseGet(() -> AstralCharacterSkills.getOrDefault(definition.id()));
            skillSet.serverTick(new CharacterSkillContext(player, state, definition, skill, skillState));
        }
        if (changed) {
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }
    }

    protected static Optional<CharacterSkillDefinition> activeSkill(CharacterDefinition definition) {
        if (definition == null || definition.skills().isEmpty()) return Optional.empty();
        for (CharacterSkillDefinition skill : definition.skills()) {
            if ("active".equalsIgnoreCase(skill.id())) {
                return Optional.of(skill);
            }
        }
        for (CharacterSkillDefinition skill : definition.skills()) {
            if (skill.cooldown() > 0 || skill.pvpCooldown() > 0 || skill.pveCooldown() > 0 || skill.cooldownSeconds() > 0 || skill.pvpCooldownSeconds() > 0 || skill.pveCooldownSeconds() > 0) {
                return Optional.of(skill);
            }
        }
        return Optional.empty();
    }

    protected static String thisCooldownKey(CharacterDefinition definition, CharacterSkillDefinition skill, Identifier handlerId) {
        return definition.id() + ":" + handlerId + ":" + skill.id();
    }

    protected static int cooldownTicks(CharacterSkillDefinition skill) {
        int seconds = skill.cooldownSeconds(CharacterSkillDefinition.SkillMode.PVP);
        if (seconds <= 0) {
            int rounds = skill.cooldown(CharacterSkillDefinition.SkillMode.PVP);
            if (rounds <= 0) return 0;
            seconds = rounds * AstralGameplayConfig.skillCooldownSecondsPerRound();
        }
        seconds = Math.clamp(seconds, AstralGameplayConfig.skillMinimumCooldownSeconds(), AstralGameplayConfig.skillMaximumCooldownSeconds());
        return Math.max(0, seconds * 20);
    }

    protected static int seconds(int ticks) {
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    protected static boolean canBypassCooldown(ServerPlayer player) {
        return player.getAbilities().instabuild;
    }

    protected static String displayNameKey(CharacterSkillDefinition skill) {
        if (!skill.nameKey().isBlank()) return skill.nameKey();
        if (!skill.pvpNameKey().isBlank()) return skill.pvpNameKey();
        if (!skill.pveNameKey().isBlank()) return skill.pveNameKey();
        return "message.astral_craft.skill.default_name";
    }

    protected static String displayDescriptionKey(CharacterSkillDefinition skill) {
        if (!skill.descriptionKey().isBlank()) return skill.descriptionKey();
        if (!skill.pvpDescriptionKey().isBlank()) return skill.pvpDescriptionKey();
        if (!skill.pveDescriptionKey().isBlank()) return skill.pveDescriptionKey();
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
