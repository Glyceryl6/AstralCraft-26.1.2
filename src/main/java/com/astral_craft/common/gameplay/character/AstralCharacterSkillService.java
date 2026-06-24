package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.handcard.AstralHandCardManager;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AstralCharacterSkillService {

    public static final double PUBLIC_CUTIN_RANGE = 96.0D;

    protected static final Map<String, AstralCharacterSkill> SKILLS = new LinkedHashMap<>();

    static {
        register("mimi", AstralCharacterSkillService::useMimiSkill);
        register("fen", AstralCharacterSkillService::useHealSkill);
        register("dorothy", AstralCharacterSkillService::useHealSkill);
        register("lulu", AstralCharacterSkillService::useHealSkill);
        register("ame", AstralCharacterSkillService::useHealSkill);
        register("nardis", AstralCharacterSkillService::useNardisSkill);
        register("pandaman", AstralCharacterSkillService::usePandamanSkill);
        register("jill", AstralCharacterSkillService::useDrawOneSkill);
        register("megas", AstralCharacterSkillService::useDrawOneSkill);
    }

    public static void register(String characterPath, AstralCharacterSkill skill) {
        if (characterPath == null || characterPath.isBlank() || skill == null) return;
        SKILLS.put(characterPath, skill);
    }

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
        String key = cooldownKey(definition, skill);
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        int cooldown = skillState.cooldown(key);
        if (cooldown > 0 && !canBypassCooldown(player)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.skill.cooldown", Component.translatable(displayNameKey(skill)), seconds(cooldown)), true);
            return;
        }

        AstralCharacterSkill handler = SKILLS.getOrDefault(definition.id().getPath(), AstralCharacterSkillService::useFallbackSkill);
        if (!handler.use(player, state, definition, skill)) {
            return;
        }

        int nextCooldown = cooldownTicks(skill);
        if (nextCooldown > 0 && !canBypassCooldown(player)) {
            skillState.setCooldown(key, nextCooldown);
            player.setData(AstralAttachments.CHARACTER_SKILLS, skillState);
        }
//        sendCutin(player, definition, skill);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.used", Component.translatable(displayNameKey(skill))).withStyle(ChatFormatting.AQUA), true);
    }

    public static void serverTick(ServerPlayer player) {
        if (player == null) return;
        CharacterSkillState skillState = player.getData(AstralAttachments.CHARACTER_SKILLS);
        if (skillState.tick()) {
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
            if (skill.cooldown() > 0 || skill.pvpCooldown() > 0 || skill.pveCooldown() > 0) {
                return Optional.of(skill);
            }
        }

        return Optional.empty();
    }

    protected static String cooldownKey(CharacterDefinition definition, CharacterSkillDefinition skill) {
        return definition.id() + ":" + skill.id();
    }

    protected static int cooldownTicks(CharacterSkillDefinition skill) {
        int rounds = skill.cooldown();
        if (rounds <= 0 && skill.pvpCooldown() >= 0) rounds = skill.pvpCooldown();
        if (rounds <= 0 && skill.pveCooldown() >= 0) rounds = skill.pveCooldown();
        if (rounds <= 0) return 0;
        return Math.clamp(rounds * 20 * 10, 20, 20 * 60 * 10);
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

    protected static boolean useMimiSkill(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill) {
        int cleared = AstralHandCardManager.clear(player);
        AstralHandCardManager.addRandomEffectCards(player, cleared + 1);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.mimi", cleared + 1), true);
        return true;
    }

    protected static boolean useHealSkill(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill) {
        float amount = Math.clamp(state.friendship(), 2.0F, 6.0F);
        player.heal(amount);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.heal", (int) amount), true);
        return true;
    }

    protected static boolean useNardisSkill(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill) {
        AstralHandCardManager.addRandomEffectCards(player, 3);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.draw", 3), true);
        return true;
    }

    protected static boolean usePandamanSkill(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill) {
        List<Identifier> foods = List.of(AstralCraft.prefix("handcard_hamburger"), AstralCraft.prefix("handcard_chocolate_cake"));
        Identifier card = foods.get(player.getRandom().nextInt(foods.size()));
        AstralHandCardManager.add(player, card, 1);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.food"), true);
        return true;
    }

    protected static boolean useDrawOneSkill(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill) {
        AstralHandCardManager.addRandomEffectCards(player, 1);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.draw", 1), true);
        return true;
    }

    protected static boolean useFallbackSkill(ServerPlayer player, ActiveCharacterState state, CharacterDefinition definition, CharacterSkillDefinition skill) {
        player.heal(1.0F);
        AstralHandCardManager.addRandomEffectCards(player, 1);
        player.sendSystemMessage(Component.translatable("message.astral_craft.skill.fallback"), true);
        return true;
    }

}