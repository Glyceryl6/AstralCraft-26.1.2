package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ConditionalEventEffect(List<AstralEventCondition> conditions, List<AstralEventEffect> thenEffects, List<AstralEventEffect> elseEffects) implements AstralEventEffect {

    public static final MapCodec<ConditionalEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AstralEventCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(ConditionalEventEffect::conditions),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("then", List.of()).forGetter(ConditionalEventEffect::thenEffects),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("else", List.of()).forGetter(ConditionalEventEffect::elseEffects)
    ).apply(instance, ConditionalEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("conditional").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        boolean pass = true;
        for (AstralEventCondition condition : this.conditions) {
            if (condition != null && !condition.test(context)) {
                pass = false;
                break;
            }
        }

        applyAll(context, pass ? this.thenEffects : this.elseEffects);
    }

    private static void applyAll(AstralEventContext context, List<AstralEventEffect> effects) {
        for (AstralEventEffect effect : effects) {
            if (effect != null) {
                effect.apply(context);
            }
        }
    }

}