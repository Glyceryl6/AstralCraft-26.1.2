package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public record MobEffectEventCondition(Holder<MobEffect> effect, boolean inverted) implements AstralEventCondition {

    public static final MapCodec<MobEffectEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(MobEffectEventCondition::effect),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MobEffectEventCondition::inverted)
    ).apply(instance, MobEffectEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("has_effect").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        LivingEntity target = context.targetLiving();
        if (target == null) return this.inverted;
        return this.inverted != target.hasEffect(this.effect);
    }

}