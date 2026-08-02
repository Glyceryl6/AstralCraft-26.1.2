package com.astral_craft.common.components;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.handcard.CardRangeResolver;
import com.astral_craft.common.gameplay.handcard.CardTargetTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record CardDefinition(
        @Nullable Identifier largeFrontTextureOverride,
        @Nullable Identifier largeBackTextureOverride,
        CardType type,
        List<Class<? extends LivingEntity>> targetTypes,
        int range,
        int minTargets,
        int maxTargets,
        int combatCost,
        CardUseRestriction restrictions) {

    public CardDefinition {
        targetTypes = CardTargetTypes.copyOf(targetTypes);
        minTargets = Math.max(0, minTargets);
        maxTargets = Math.max(minTargets, maxTargets);
        combatCost = Math.clamp(combatCost, 0, 9);
    }

    public static final Codec<CardDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("large_front_texture")
                    .forGetter(definition -> Optional.ofNullable(definition.largeFrontTextureOverride)),
            Identifier.CODEC.optionalFieldOf("large_back_texture")
                    .forGetter(definition -> Optional.ofNullable(definition.largeBackTextureOverride)),
            CardType.CODEC.fieldOf("type").forGetter(CardDefinition::type),
            CardTargetTypes.CODEC.optionalFieldOf("target_types").forGetter(definition -> Optional.of(definition.targetTypes())),
            CardTargetTypes.LEGACY_CODEC.optionalFieldOf("target_mode").forGetter(_ -> Optional.empty()),
            Codec.INT.fieldOf("range").forGetter(CardDefinition::range),
            Codec.INT.fieldOf("min_targets").forGetter(CardDefinition::minTargets),
            Codec.INT.fieldOf("max_targets").forGetter(CardDefinition::maxTargets),
            Codec.INT.optionalFieldOf("combat_cost", 0).forGetter(CardDefinition::combatCost),
            CardUseRestriction.CODEC.optionalFieldOf("restrictions", CardUseRestriction.NONE).forGetter(CardDefinition::restrictions)
    ).apply(instance, (largeFrontTextureOverride, largeBackTextureOverride, type, targetTypes, legacyTargetTypes,
                       range, minTargets, maxTargets, combatCost, restrictions) -> new CardDefinition(
            largeFrontTextureOverride.orElse(null), largeBackTextureOverride.orElse(null), type,
            targetTypes.orElseGet(() -> legacyTargetTypes.orElse(CardTargetTypes.NONE)),
            range, minTargets, maxTargets, combatCost, restrictions)));

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
        return this.largeFrontTextureOverride != null ? this.largeFrontTextureOverride : Identifier.fromNamespaceAndPath(
                itemId.getNamespace(), "textures/gui/cards/front/" + itemId.getPath() + ".jpg");
    }

    public Identifier largeBackTexture() {
        return this.largeBackTextureOverride == null ? defaultBackTexture() : this.largeBackTextureOverride;
    }

    public boolean needsTarget() {
        return !this.targetTypes.isEmpty() && this.maxTargets > 0;
    }

    public boolean acceptsTarget(LivingEntity target) {
        return this.targetTypes.stream().anyMatch(targetType -> targetType.isInstance(target));
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
                this.targetTypes, this.range, this.minTargets, this.maxTargets, this.combatCost, this.restrictions);
    }

    public CardDefinition withFrontTexture(@Nullable Identifier texture) {
        return new CardDefinition(texture, this.largeBackTextureOverride, this.type,
                this.targetTypes, this.range, this.minTargets, this.maxTargets, this.combatCost, this.restrictions);
    }

    public CardDefinition withBackTexture(@Nullable Identifier texture) {
        return new CardDefinition(this.largeFrontTextureOverride, texture, this.type,
                this.targetTypes, this.range, this.minTargets, this.maxTargets, this.combatCost, this.restrictions);
    }

    public CardDefinition withRestrictions(CardUseRestriction restrictions) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                this.targetTypes, this.range, this.minTargets, this.maxTargets, this.combatCost, restrictions);
    }

    public CardDefinition withRange(int range) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                this.targetTypes, range, this.minTargets, this.maxTargets, this.combatCost, this.restrictions);
    }

    public CardDefinition withTargetTypes(List<Class<? extends LivingEntity>> targetTypes) {
        if (targetTypes.isEmpty()) {
            return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                    CardTargetTypes.NONE, this.range, 0, 0, this.combatCost, this.restrictions);
        }

        int minTargets = this.needsTarget() ? this.minTargets : 1;
        int maxTargets = this.needsTarget() ? this.maxTargets : 1;
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                targetTypes, this.range, minTargets, maxTargets, this.combatCost, this.restrictions);
    }

    public CardDefinition withTargetCount(int minTargets, int maxTargets) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                this.targetTypes, this.range, minTargets, maxTargets, this.combatCost, this.restrictions);
    }

    public CardDefinition withCombatCost(int combatCost) {
        return new CardDefinition(this.largeFrontTextureOverride, this.largeBackTextureOverride, this.type,
                this.targetTypes, this.range, this.minTargets, this.maxTargets, combatCost, this.restrictions);
    }

    public static CardDefinition create(CardType type, List<Class<? extends LivingEntity>> targetTypes, int range) {
        int targetCount = targetTypes.isEmpty() ? 0 : 1;
        return create(type, targetTypes, range, targetCount, targetCount);
    }

    public static CardDefinition create(CardType type, List<Class<? extends LivingEntity>> targetTypes, int range, int minTargets, int maxTargets) {
        return new CardDefinition(null, null, type, targetTypes, range, minTargets, maxTargets, 0, CardUseRestriction.NONE);
    }

    public static CardDefinition fallback() {
        return create(CardType.EFFECT, CardTargetTypes.NONE, -1);
    }

    public static Identifier defaultBackTexture() {
        return AstralCraft.prefix("textures/gui/cards/back/card_back_0.jpg");
    }

}