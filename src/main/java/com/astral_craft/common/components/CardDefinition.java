package com.astral_craft.common.components;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.handcard.CardRangeResolver;
import com.astral_craft.common.gameplay.handcard.CardTargetMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record CardDefinition(
        Optional<Identifier> largeFrontTextureOverride,
        Optional<Identifier> largeBackTextureOverride,
        CardType type,
        CardTargetMode targetMode,
        int range,
        int minTargets,
        int maxTargets,
        CardUseRestriction restrictions) {

    public CardDefinition {
        minTargets = Math.max(0, minTargets);
        maxTargets = Math.max(minTargets, maxTargets);
    }

    public static final Codec<CardDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("large_front_texture").forGetter(CardDefinition::largeFrontTextureOverride),
            Identifier.CODEC.optionalFieldOf("large_back_texture").forGetter(CardDefinition::largeBackTextureOverride),
            CardType.CODEC.fieldOf("type").forGetter(CardDefinition::type),
            CardTargetMode.CODEC.fieldOf("target_mode").forGetter(CardDefinition::targetMode),
            Codec.INT.fieldOf("range").forGetter(CardDefinition::range),
            Codec.INT.fieldOf("min_targets").forGetter(CardDefinition::minTargets),
            Codec.INT.fieldOf("max_targets").forGetter(CardDefinition::maxTargets),
            CardUseRestriction.CODEC.optionalFieldOf("restrictions", CardUseRestriction.NONE).forGetter(CardDefinition::restrictions)
    ).apply(instance, CardDefinition::new));

    public static final StreamCodec<ByteBuf, CardDefinition> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public Identifier itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public MutableComponent displayName(ItemStack stack) {
        return stack.getHoverName().copy();
    }

    public MutableComponent effectText(ItemStack stack, Object... arguments) {
        return Component.translatable(this.effectKey(stack), arguments);
    }

    public String effectKey(ItemStack stack) {
        Identifier itemId = this.itemId(stack);
        return "tooltips." + itemId.getNamespace() + "." + itemId.getPath();
    }

    public Identifier largeFrontTexture(ItemStack stack) {
        Identifier itemId = this.itemId(stack);
        return this.largeFrontTextureOverride.orElseGet(() -> Identifier.fromNamespaceAndPath(
                itemId.getNamespace(), "textures/gui/cards/front/" + itemId.getPath() + ".jpg"));
    }

    public Identifier largeBackTexture() {
        return this.largeBackTextureOverride.orElseGet(CardDefinition::defaultBackTexture);
    }

    public boolean needsTarget() {
        return this.maxTargets > 0;
    }

    public boolean shouldRevealOnUse() {
        return this.type == CardType.EFFECT || this.type == CardType.JINX;
    }

    public boolean isCombatOnly() {
        return this.type == CardType.ATTACK || this.type == CardType.DEFENSE;
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

    public CardDefinition withType(CardType cardType) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, cardType,
                this.targetMode, this.range, this.minTargets, this.maxTargets, this.restrictions);
    }

    public CardDefinition withFrontTexture(@Nullable Identifier texture) {
        return new CardDefinition(Optional.ofNullable(texture), this.largeBackTextureOverride, this.type,
                this.targetMode, this.range, this.minTargets, this.maxTargets, this.restrictions);
    }

    public CardDefinition withBackTexture(@Nullable Identifier texture) {
        return new CardDefinition(this.largeFrontTextureOverride, Optional.ofNullable(texture), this.type,
                this.targetMode, this.range, this.minTargets, this.maxTargets, this.restrictions);
    }

    public CardDefinition withRestrictions(CardUseRestriction restrictions) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                this.targetMode, this.range, this.minTargets, this.maxTargets, restrictions);
    }

    public CardDefinition withRange(int range) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                this.targetMode, range, this.minTargets, this.maxTargets, this.restrictions);
    }

    public static CardDefinition create(CardType type, CardTargetMode targetMode, int range) {
        return new CardDefinition(Optional.empty(), Optional.empty(), type, targetMode, range,
                minTargets(targetMode), maxTargets(targetMode), CardUseRestriction.NONE);
    }

    public static CardDefinition fallback() {
        return create(CardType.EFFECT, CardTargetMode.NONE, -1);
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

    public static Identifier defaultBackTexture() {
        return AstralCraft.prefix("textures/gui/cards/card_back.png");
    }
}
