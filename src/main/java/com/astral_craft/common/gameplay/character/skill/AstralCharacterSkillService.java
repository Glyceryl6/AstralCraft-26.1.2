package com.astral_craft.common.gameplay.character.skill;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.config.AstralGameplayConfig;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardPhase;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardSpectatorService;
import com.astral_craft.common.gameplay.character.*;
import com.astral_craft.common.network.s2c.CharacterSkillCutinPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

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
        AstralCharacter character = CharacterManager.INSTANCE.character(state.characterId());
        ActiveCharacterSkillDefinition skill = character.activeSkill();
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        String key = cooldownKey(definition);
        int cooldown = skillState.cooldown(key);
        if (cooldown > 0 && !canBypassCooldown(player)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.cooldown",
                    Component.translatable(definition.skillNameKey(skill.view(), CharacterSkillView.SkillMode.PVP)), seconds(cooldown)), true);
            return;
        }
        CharacterSkillContext context = new CharacterSkillContext(player, player, state, definition, skill, skillState);
        if (!character.useActiveSkill(context)) return;
        character.onSkillUsed(context);
        int nextCooldown = cooldownTicks(skill);
        if (nextCooldown > 0 && !canBypassCooldown(player)) {
            skillState.setCooldown(key, nextCooldown);
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }
        sendCutin(player, state, definition, skill, character);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used",
                Component.translatable(definition.skillNameKey(skill.view(), CharacterSkillView.SkillMode.PVP)))
                .withStyle(ChatFormatting.AQUA), true);
    }

    public static int useActiveSkillForBoard(ServerPlayer player, AstralCharacterEntity actor, BoardSession session,
                                             BoardParticipant participant, List<ServerPlayer> viewers) {
        if (player == null || actor == null || session == null || participant == null) return -1;
        Identifier characterId = participant.characterId();
        if (!CharacterManager.INSTANCE.contains(characterId)) return -1;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        AstralCharacter character = CharacterManager.INSTANCE.character(characterId);
        BoardSkillDefinition skill = character.boardSkill();
        BoardSkillContext context = new BoardSkillContext(player, player.level(), actor, session, participant, definition, skill);
        if (!character.useBoardSkill(context)) return -1;
        character.onBoardSkillUsed(context);
        int duration = AstralGameplayConfig.skillCutinDurationTicks();
        Identifier animation = skill.safeAnimation(character.fallbackAnimation());
        if (duration > 0 && animation != null) {
            CharacterSkillCutinPayload payload = new CharacterSkillCutinPayload(definition.id(), participant.skinName(),
                    "active", animation, duration);
            for (ServerPlayer viewer : viewers == null ? List.<ServerPlayer>of() : viewers) PacketDistributor.sendToPlayer(viewer, payload);
        }
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used",
                Component.translatable(definition.skillNameKey(character.activeSkill().view(), CharacterSkillView.SkillMode.PVP)))
                .withStyle(ChatFormatting.AQUA), true);
        return Math.max(0, skill.pvpCooldown());
    }

    public static void serverTick(ServerPlayer player) {
        if (player == null) return;
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        boolean changed = skillState.tick();
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        if (state.active()) {
            AstralCharacterSkillEffects.removeStatusEffectsNotOwnedByActiveCharacter(player);
            CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId());
            AstralCharacter character = CharacterManager.INSTANCE.character(state.characterId());
            character.serverTick(new CharacterSkillContext(player, player, state, definition, character.activeSkill(), skillState));
            character.onPlayerTick(player);
        } else {
            AstralCharacterSkillEffects.clearStatusEffects(player);
        }
        if (changed) player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
    }

    public static boolean addStatusEffect(ServerPlayer player, Identifier statusId, int durationTicks, int amplifier) {
        return AstralCharacterSkillEffects.add(player, statusId, durationTicks, amplifier);
    }

    public static boolean hasStatusEffect(ServerPlayer player, String id) {
        return hasStatusEffect(player, parseIdentifier(id));
    }

    public static boolean hasStatusEffect(ServerPlayer player, Identifier id) {
        return AstralCharacterSkillEffects.hasStatusEffect(player, id);
    }

    public static void clearStatusEffects(ServerPlayer player) {
        AstralCharacterSkillEffects.clearStatusEffects(player);
    }

    public static int durationTicks(ActiveCharacterSkillDefinition skill) {
        int seconds = skill.durationSeconds() <= 0 ? DEFAULT_STATUS_DURATION_SECONDS : skill.durationSeconds();
        seconds = Math.clamp(seconds, 1, AstralGameplayConfig.skillMaximumCooldownSeconds());
        return Math.max(1, seconds * 20);
    }

    protected static String cooldownKey(CharacterDefinition definition) {
        return definition.id() + ":active";
    }

    protected static int cooldownTicks(ActiveCharacterSkillDefinition skill) {
        int rounds = skill.cooldown(CharacterSkillView.SkillMode.PVP);
        if (rounds <= 0) return 0;
        int seconds = rounds * AstralGameplayConfig.skillCooldownSecondsPerRound();
        seconds = Math.clamp(seconds, AstralGameplayConfig.skillMinimumCooldownSeconds(), AstralGameplayConfig.skillMaximumCooldownSeconds());
        return seconds * 20;
    }

    protected static int seconds(int ticks) {
        return Math.max(1, (int) Math.ceil(ticks / 20.0D));
    }

    protected static boolean canBypassCooldown(ServerPlayer player) {
        return player.getAbilities().instabuild;
    }

    protected static Identifier parseIdentifier(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return raw.contains(":") ? Identifier.parse(raw) : AstralCraft.prefix(raw);
        } catch (Exception exception) {
            return null;
        }
    }

    protected static void sendCutin(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition,
                                    ActiveCharacterSkillDefinition skill, AstralCharacter character) {
        int duration = AstralGameplayConfig.skillCutinDurationTicks();
        if (duration <= 0) return;
        Identifier animation = skill.safeAnimation(character.fallbackAnimation());
        if (animation == null) return;
        CharacterSkillCutinPayload payload = new CharacterSkillCutinPayload(definition.id(), state.skinId(),
                "active", animation, duration);
        BoardSession boardSession = BoardSessionManager.findByController(player)
                .filter(session -> session.phase() == BoardPhase.PLAYING).orElse(null);
        if (boardSession != null) {
            for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(player.level(), boardSession)) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
            return;
        }
        CharacterSkillCutinAudience audience = AstralGameplayConfig.skillCutinAudience();
        if (audience == CharacterSkillCutinAudience.NONE) return;
        if (audience.sendsToNearbyPlayers()) {
            double maxDistanceSqr = PUBLIC_CUTIN_RANGE * PUBLIC_CUTIN_RANGE;
            for (ServerPlayer viewer : player.level().players()) {
                if (viewer.distanceToSqr(player) <= maxDistanceSqr) PacketDistributor.sendToPlayer(viewer, payload);
            }
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }
}
