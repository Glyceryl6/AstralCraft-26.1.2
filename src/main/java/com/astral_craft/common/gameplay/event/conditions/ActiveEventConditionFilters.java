package com.astral_craft.common.gameplay.event.conditions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ActiveEventConditionFilters {

    public static boolean matchesBlock(BlockState blockState, List<Identifier> blocks, List<Identifier> blockTags) {
        if (blockState == null) return false;
        if (empty(blocks) && empty(blockTags)) return true;
        Identifier current = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (!empty(blocks) && blocks.contains(current)) return true;
        if (!empty(blockTags)) {
            for (Identifier tag : blockTags) {
                if (tag != null && blockState.is(TagKey.create(Registries.BLOCK, tag))) return true;
            }
        }
        return false;
    }

    public static boolean matchesDamage(DamageSource damageSource, List<Identifier> damageTypes, List<Identifier> damageTypeTags) {
        if (damageSource == null) return false;
        if (empty(damageTypes) && empty(damageTypeTags)) return true;
        if (!empty(damageTypes)) {
            for (Identifier damageType : damageTypes) {
                if (damageType != null && damageSource.is(ResourceKey.create(Registries.DAMAGE_TYPE, damageType))) return true;
            }
        }
        if (!empty(damageTypeTags)) {
            for (Identifier damageTag : damageTypeTags) {
                if (damageTag != null && damageSource.is(TagKey.create(Registries.DAMAGE_TYPE, damageTag))) return true;
            }
        }
        return false;
    }

    public static boolean matchesEntity(Entity entity, List<Identifier> entityTypes, List<Identifier> entityTypeTags) {
        if (entity == null) return empty(entityTypes) && empty(entityTypeTags);
        if (empty(entityTypes) && empty(entityTypeTags)) return true;
        Identifier current = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!empty(entityTypes) && entityTypes.contains(current)) return true;
        if (!empty(entityTypeTags)) {
            for (Identifier tag : entityTypeTags) {
                if (tag != null && entity.is(TagKey.create(Registries.ENTITY_TYPE, tag))) return true;
            }
        }
        return false;
    }

    public static boolean matchesDamageAndEntity(DamageSource damageSource,
                                                 Entity entity,
                                                 List<Identifier> damageTypes,
                                                 List<Identifier> damageTypeTags,
                                                 List<Identifier> entityTypes,
                                                 List<Identifier> entityTypeTags,
                                                 boolean inverted) {
        boolean matches = matchesDamage(damageSource, damageTypes, damageTypeTags) && matchesEntity(entity, entityTypes, entityTypeTags);
        return inverted != matches;
    }

    private static boolean empty(List<?> list) {
        return list == null || list.isEmpty();
    }

}
