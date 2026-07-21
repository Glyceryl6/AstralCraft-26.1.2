package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.stats.AstralStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;

public record HealEventEffect(float amount) implements AstralEventEffect {

    public static final MapCodec<HealEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("amount", 2.0F).forGetter(HealEventEffect::amount)
    ).apply(instance, HealEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("heal").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        LivingEntity target = context.targetLiving();
        if (target == null || this.amount <= 0.0F) return;
        AstralCardEffects.update(target, AstralStats.getOrDefault(target).heal(Math.max(1, Math.round(this.amount))));
    }

}