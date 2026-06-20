package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public record DamageEventEffect(float amount) implements AstralEventEffect {

    public static final MapCodec<DamageEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("amount", 1.0F).forGetter(DamageEventEffect::amount)
    ).apply(instance, DamageEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("damage").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        LivingEntity target = context.targetLiving();
        if (target == null) return;
        ServerLevel level = context.level();
        target.hurtServer(level, target.damageSources().generic(), Math.max(0.0F, this.amount));
    }

}