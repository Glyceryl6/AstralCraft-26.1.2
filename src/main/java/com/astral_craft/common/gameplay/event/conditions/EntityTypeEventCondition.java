package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

public record EntityTypeEventCondition(List<Identifier> entityTypes, boolean inverted) implements AstralEventGeneralCondition {

    public static final MapCodec<EntityTypeEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("entity_types", List.of()).forGetter(EntityTypeEventCondition::entityTypes),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EntityTypeEventCondition::inverted)
    ).apply(instance, EntityTypeEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("entity_type").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.target() == null || this.entityTypes.isEmpty()) return !this.inverted;
        Identifier current = BuiltInRegistries.ENTITY_TYPE.getKey(context.target().getType());
        return this.inverted != this.entityTypes.contains(current);
    }

}