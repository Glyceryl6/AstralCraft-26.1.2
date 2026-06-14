package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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

    public static final Codec<CardDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CardDefinition::id),
            Codec.STRING.fieldOf("name_key").forGetter(CardDefinition::nameKey),
            Codec.STRING.fieldOf("effect_key").forGetter(CardDefinition::effectKey),
            Codec.STRING.fieldOf("large_front_texture").forGetter(CardDefinition::largeFrontTexture),
            Codec.STRING.fieldOf("large_back_texture").forGetter(CardDefinition::largeBackTexture),
            CardType.CODEC.fieldOf("type").forGetter(CardDefinition::type),
            CardTargetMode.CODEC.fieldOf("target_mode").forGetter(CardDefinition::targetMode),
            Codec.INT.fieldOf("range").forGetter(CardDefinition::range),
            Codec.BOOL.fieldOf("combat_only").forGetter(CardDefinition::combatOnly),
            Codec.INT.fieldOf("min_targets").forGetter(CardDefinition::minTargets),
            Codec.INT.fieldOf("max_targets").forGetter(CardDefinition::maxTargets)
    ).apply(instance, CardDefinition::new));

    public static final StreamCodec<ByteBuf, CardDefinition> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

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
        return new CardDefinition(id, nameKey(id), effectKey(id), largeFrontTexture(id),
                defaultBackTexture(), type, targetMode, range, combatOnly,
                minTargets(targetMode), maxTargets(targetMode));
    }

    public static CardDefinition fallback() {
        return create("unknown", CardType.EFFECT, CardTargetMode.NONE, -1, false);
    }

    public static int minTargets(CardTargetMode targetMode) {
        return switch (targetMode) {
            case TWO_PLAYERS -> 2;
            case ALLY, ENEMY_PLAYER, ANY_PLAYER, MONSTER -> 1;
            default -> 0;
        };
    }

    public static int maxTargets(CardTargetMode targetMode) {
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