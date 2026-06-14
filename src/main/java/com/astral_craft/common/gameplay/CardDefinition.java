package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import net.minecraft.network.chat.Component;

public record CardDefinition(
        String id,
        String nameKey,
        String effectKey,
        String largeFrontTexture,
        String largeBackTexture,
        CardType type,
        CardTargetMode targetMode,
        int range,
        boolean combatOnly,
        int minTargets,
        int maxTargets) {

    public Component displayName() {
        return Component.translatable(this.nameKey);
    }

    public Component effectText() {
        return Component.translatable(this.effectKey);
    }

    public String registryPath() {
        return this.id;
    }

    public boolean needsTarget() {
        return this.maxTargets > 0;
    }

    public boolean shouldRevealOnUse() {
        return !this.combatOnly && this.type == CardType.EFFECT;
    }

    public boolean isAstralItemPath(String path) {
        return this.id.equals(path);
    }

    public static CardDefinition create(String id, CardType type, CardTargetMode targetMode, int range, boolean combatOnly) {
        return new CardDefinition(
                id,
                nameKey(id),
                effectKey(id),
                largeFrontTexture(id),
                defaultBackTexture(),
                type,
                targetMode,
                range,
                combatOnly,
                minTargets(targetMode),
                maxTargets(targetMode));
    }

    private static int minTargets(CardTargetMode targetMode) {
        return switch (targetMode) {
            case TWO_PLAYERS -> 2;
            case ALLY, ENEMY_PLAYER, ANY_PLAYER, MONSTER -> 1;
            default -> 0;
        };
    }

    private static int maxTargets(CardTargetMode targetMode) {
        return switch (targetMode) {
            case TWO_PLAYERS -> 2;
            case ALLY, ENEMY_PLAYER, ANY_PLAYER, MONSTER -> 1;
            default -> 0;
        };
    }

    public static String nameKey(String id) {
        return "card." + AstralCraft.MOD_ID + "." + id;
    }

    public static String effectKey(String id) {
        return "tooltips." + AstralCraft.MOD_ID + "." + id;
    }

    public static String largeFrontTexture(String id) {
        return AstralCraft.MOD_ID + ":textures/gui/cards/front/" + id + ".png";
    }

    public static String defaultBackTexture() {
        return AstralCraft.MOD_ID + ":textures/gui/cards/card_back.png";
    }

}