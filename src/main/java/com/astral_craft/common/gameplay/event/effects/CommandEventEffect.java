package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CommandEventEffect(String command) implements AstralEventEffect {

    public static final MapCodec<CommandEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("command").forGetter(CommandEventEffect::command)
    ).apply(instance, CommandEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("command").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        EventEffectCommands.run(context, this.command);
    }

}