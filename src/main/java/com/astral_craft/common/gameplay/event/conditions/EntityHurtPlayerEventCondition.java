package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralActiveEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record EntityHurtPlayerEventCondition(
        List<Identifier> damageTypes,
        List<Identifier> damageTypeTags,
        List<Identifier> entityTypes,
        List<Identifier> entityTypeTags,
        boolean inverted) implements AstralActiveEventCondition {

    public static final MapCodec<EntityHurtPlayerEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("damage_types", List.of()).forGetter(EntityHurtPlayerEventCondition::damageTypes),
            Identifier.CODEC.listOf().optionalFieldOf("damage_type_tags", List.of()).forGetter(EntityHurtPlayerEventCondition::damageTypeTags),
            Identifier.CODEC.listOf().optionalFieldOf("entity_types", List.of()).forGetter(EntityHurtPlayerEventCondition::entityTypes),
            Identifier.CODEC.listOf().optionalFieldOf("entity_type_tags", List.of()).forGetter(EntityHurtPlayerEventCondition::entityTypeTags),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EntityHurtPlayerEventCondition::inverted)
    ).apply(instance, EntityHurtPlayerEventCondition::new));

    public EntityHurtPlayerEventCondition() {
        this(List.of(), List.of(), List.of(), List.of(), false);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("entity_hurt_player").toString();
    }

    @Override
    public MapCodec<? extends AstralActiveEventCondition> activeCodec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || !context.isEntityHurtPlayer()) return false;
        return ActiveEventConditionFilters.matchesDamageAndEntity(context.damageSource(), context.damageSourceEntity(),
                this.damageTypes, this.damageTypeTags, this.entityTypes, this.entityTypeTags, this.inverted);
    }
}
