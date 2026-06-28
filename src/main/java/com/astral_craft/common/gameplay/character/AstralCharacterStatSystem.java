package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillEffect;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillEffectState;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class AstralCharacterStatSystem {

    public static final Identifier PROPERTY_ATTACK_BONUS = AstralCraft.prefix("attack_bonus");
    public static final Identifier PROPERTY_DEFENSE_BONUS = AstralCraft.prefix("defense_bonus");
    public static final Identifier PROPERTY_SPEED_BONUS_PERCENT = AstralCraft.prefix("speed_bonus_percent");
    public static final Identifier PROPERTY_VISIBILITY_MODE = AstralCraft.prefix("visibility_mode");

    public static int attack(ServerPlayer player) {
        ActiveCharacterState state = activeState(player);
        if (!state.active()) return 0;
        return Math.max(0, state.attack() + effectInt(player, PROPERTY_ATTACK_BONUS));
    }

    public static int defense(ServerPlayer player) {
        ActiveCharacterState state = activeState(player);
        if (!state.active()) return 0;
        return Math.max(0, state.defense() + effectInt(player, PROPERTY_DEFENSE_BONUS));
    }

    public static int characterHealth(ServerPlayer player) {
        ActiveCharacterState state = activeState(player);
        return state.active() ? state.health() : 0;
    }

    public static int speedBonusPercent(ServerPlayer player) {
        return effectInt(player, PROPERTY_SPEED_BONUS_PERCENT);
    }

    public static String visibilityMode(ServerPlayer player) {
        if (player == null) return "";
        CharacterSkillEffectState effects = player.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        for (CharacterSkillEffect effect : effects.activeEffects()) {
            String value = effect.property(PROPERTY_VISIBILITY_MODE);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    public static boolean hasSkillEffect(ServerPlayer player, Identifier id) {
        return player != null && player.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS).contains(id);
    }

    public static int effectInt(ServerPlayer player, Identifier propertyKey) {
        if (player == null || propertyKey == null) return 0;
        int total = 0;
        CharacterSkillEffectState effects = player.getData(AstralAttachments.CHARACTER_SKILL_EFFECTS);
        for (CharacterSkillEffect effect : effects.activeEffects()) {
            total += effect.propertyInt(propertyKey, 0);
        }
        return total;
    }

    protected static ActiveCharacterState activeState(ServerPlayer player) {
        return player == null ? ActiveCharacterState.NONE : player.getData(AstralAttachments.ACTIVE_CHARACTER);
    }

}
