package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

public record AddHungerEventEffect(int nutrition, float saturation) implements AstralEventEffect {

    public static final MapCodec<AddHungerEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("nutrition", 2).forGetter(AddHungerEventEffect::nutrition),
            Codec.FLOAT.optionalFieldOf("saturation", 0.2F).forGetter(AddHungerEventEffect::saturation)
    ).apply(instance, AddHungerEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("add_hunger").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        ServerPlayer target = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (target != null && this.nutrition != 0) {
            target.getFoodData().eat(this.nutrition, this.saturation);
        }
    }
}
