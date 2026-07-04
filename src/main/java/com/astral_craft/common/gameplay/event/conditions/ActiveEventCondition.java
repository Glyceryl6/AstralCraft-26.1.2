package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.registry.AstralAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ActiveEventCondition(String key, boolean inverted) implements AstralEventGeneralCondition {

    public static final MapCodec<ActiveEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(ActiveEventCondition::key),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(ActiveEventCondition::inverted)
    ).apply(instance, ActiveEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("active_event").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.target() == null) return this.inverted;
        boolean active = context.target().getData(AstralAttachments.EVENT_STATE).active(this.key);
        return this.inverted != active;
    }

}