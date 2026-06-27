package com.astral_craft.common.components;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.CardTargetMode;
import com.astral_craft.common.gameplay.CardRangeResolver;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
        int maxTargets,
        CardUseRestriction restrictions) {

    public static final Codec<CardDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(CardDefinition::id),
            Codec.STRING.fieldOf("name_key").forGetter(CardDefinition::nameKey),
            Codec.STRING.fieldOf("effect_key").forGetter(CardDefinition::effectKey),
            Codec.STRING.fieldOf("large_front_texture").forGetter(CardDefinition::largeFrontTexture),
            Codec.STRING.fieldOf("large_back_texture").forGetter(CardDefinition::largeBackTexture),
            CardType.CODEC.fieldOf("type").forGetter(CardDefinition::type),
            CardTargetMode.CODEC.fieldOf("target_mode").forGetter(CardDefinition::targetMode),
            Codec.INT.fieldOf("range").forGetter(CardDefinition::range),
            Codec.BOOL.fieldOf("combat_only").forGetter(CardDefinition::combatOnly),
            Codec.INT.fieldOf("min_targets").forGetter(CardDefinition::minTargets),
            Codec.INT.fieldOf("max_targets").forGetter(CardDefinition::maxTargets),
            CardUseRestriction.CODEC.optionalFieldOf("restrictions", CardUseRestriction.NONE).forGetter(CardDefinition::restrictions)
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

    public int effectiveRange(Player player, ItemStack stack) {
        return CardRangeResolver.effectiveRange(player, stack, this);
    }

    public int targetingRange(Player player, ItemStack stack) {
        return CardRangeResolver.targetingRange(player, stack, this);
    }

    public CardDefinition resolveDynamic(Player player, ItemStack stack) {
        return CardRangeResolver.effectiveDefinition(player, stack, this);
    }

    public boolean isAstralItemPath(String path) {
        return this.id.equals(path);
    }

    public CardDefinition withId(String id) {
        return new CardDefinition(id, nameKey(id), effectKey(id), largeFrontTexture(id),
                this.largeBackTexture, this.type, this.targetMode, this.range,
                this.combatOnly, this.minTargets, this.maxTargets, this.restrictions);
    }

    public CardDefinition withType(CardType cardType) {
        return new CardDefinition(this.id, this.nameKey, this.effectKey,
                this.largeFrontTexture, this.largeBackTexture, cardType,
                this.targetMode, this.range, this.combatOnly, this.minTargets, this.maxTargets,
                this.restrictions);
    }

    public CardDefinition withBackTexture(String texture) {
        return new CardDefinition(this.id, this.nameKey, this.effectKey,
                this.largeFrontTexture, texture, this.type, this.targetMode,
                this.range, this.combatOnly, this.minTargets, this.maxTargets,
                this.restrictions);
    }

    public CardDefinition withRestrictions(CardUseRestriction restrictions) {
        return new CardDefinition(this.id, this.nameKey, this.effectKey,
                this.largeFrontTexture, this.largeBackTexture, this.type, this.targetMode,
                this.range, this.combatOnly, this.minTargets, this.maxTargets, restrictions);
    }

    public CardDefinition withRange(int range) {
        return new CardDefinition(this.id, this.nameKey, this.effectKey,
                this.largeFrontTexture, this.largeBackTexture, this.type, this.targetMode,
                range, this.combatOnly, this.minTargets, this.maxTargets,
                this.restrictions);
    }

    /** Preferred factory for hand card classes. The final id is derived from the item registry id in AstralItems#registerCard. */
    public static CardDefinition create(CardType type, CardTargetMode targetMode, int range, boolean combatOnly) {
        return create("", type, targetMode, range, combatOnly);
    }

    /** Legacy overload. Kept so old card classes or external mods do not have to migrate immediately. */
    public static CardDefinition create(String id, CardType type, CardTargetMode targetMode, int range, boolean combatOnly) {
        return new CardDefinition(id, nameKey(id), effectKey(id), largeFrontTexture(id), defaultBackTexture(),
                type, targetMode, range, combatOnly, minTargets(targetMode), maxTargets(targetMode), CardUseRestriction.NONE);
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
