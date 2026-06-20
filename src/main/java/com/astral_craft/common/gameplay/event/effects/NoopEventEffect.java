package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.MapCodec;

public record NoopEventEffect() implements AstralEventEffect {

    public static final MapCodec<NoopEventEffect> CODEC = MapCodec.unit(new NoopEventEffect());

    @Override
    public String typeId() {
        return AstralCraft.prefix("noop").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {}

}