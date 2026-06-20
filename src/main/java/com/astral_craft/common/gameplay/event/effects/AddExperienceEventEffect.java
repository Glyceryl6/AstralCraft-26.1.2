package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

public record AddExperienceEventEffect(int amount) implements AstralEventEffect {

    public static final MapCodec<AddExperienceEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("amount", 5).forGetter(AddExperienceEventEffect::amount)
    ).apply(instance, AddExperienceEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("add_experience").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        ServerPlayer target = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (target != null && this.amount != 0) {
            target.giveExperiencePoints(this.amount);
        }
    }
}
