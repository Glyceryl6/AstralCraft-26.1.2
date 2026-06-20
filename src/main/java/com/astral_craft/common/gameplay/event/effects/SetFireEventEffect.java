package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;

public record SetFireEventEffect(int seconds) implements AstralEventEffect {

    public static final MapCodec<SetFireEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("seconds", 4).forGetter(SetFireEventEffect::seconds)
    ).apply(instance, SetFireEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("set_fire").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        Entity target = context.target();
        if (target != null && this.seconds > 0) {
            target.setRemainingFireTicks(this.seconds * 20);
        }
    }

}