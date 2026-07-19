package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventGeneralCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;
import java.util.Locale;

public record EntityCategoryEventCondition(List<String> categories, boolean inverted) implements AstralEventGeneralCondition {

    public static final MapCodec<EntityCategoryEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("categories", List.of()).forGetter(EntityCategoryEventCondition::categories),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(EntityCategoryEventCondition::inverted)
    ).apply(instance, EntityCategoryEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("entity_category").toString();
    }

    @Override
    public MapCodec<? extends AstralEventGeneralCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        if (context == null || context.target() == null || this.categories.isEmpty()) return !this.inverted;
        boolean matches = false;
        for (String category : this.categories) {
            if (matches(context.target(), category)) {
                matches = true;
                break;
            }
        }

        return this.inverted != matches;
    }

    private static boolean matches(Entity entity, String category) {
        String normalized = category == null ? "" : category.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "any" -> true;
            case "player", "players" -> entity instanceof ServerPlayer;
            case "living" -> entity instanceof LivingEntity;
            case "mob", "mobs" -> entity instanceof Mob;
            case "monster", "monsters", "hostile" -> entity instanceof Monster;
            case "animal", "animals", "passive" -> entity instanceof Animal;
            default -> false;
        };
    }

}