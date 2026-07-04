package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralActiveEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;

public record ForNearbyEntitiesEventEffect(double radius, int maxTargets, List<String> categories,
                                           List<AstralActiveEventCondition> conditions,
                                           List<AstralEventEffect> effects) implements AstralEventEffect {

    public static final MapCodec<ForNearbyEntitiesEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("radius", 6.0D).forGetter(ForNearbyEntitiesEventEffect::radius),
            Codec.INT.optionalFieldOf("max_targets", 16).forGetter(ForNearbyEntitiesEventEffect::maxTargets),
            Codec.STRING.listOf().optionalFieldOf("categories", List.of("living")).forGetter(ForNearbyEntitiesEventEffect::categories),
            AstralActiveEventCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(ForNearbyEntitiesEventEffect::conditions),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ForNearbyEntitiesEventEffect::effects)
    ).apply(instance, ForNearbyEntitiesEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("for_nearby_entities").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        if (context.level() == null || context.target() == null || this.effects.isEmpty()) return;
        double safeRadius = Math.clamp(this.radius, 0.0D, 128.0D);
        int safeMaxTargets = Math.clamp(this.maxTargets, 0, 256);
        if (safeRadius <= 0.0D || safeMaxTargets <= 0) return;
        AABB box = context.target().getBoundingBox().inflate(safeRadius);
        int applied = 0;
        for (Entity entity : context.level().getEntities(context.target(), box, entity -> !entity.isRemoved() && matchesAny(entity, this.categories))) {
            AstralEventContext scoped = AstralEventContext.of(context.triggerPlayer(), entity, context.definition());
            boolean pass = true;
            for (AstralActiveEventCondition condition : this.conditions) {
                if (condition != null && !condition.test(scoped)) {
                    pass = false;
                    break;
                }
            }

            if (!pass) continue;
            for (AstralEventEffect effect : this.effects) {
                if (effect != null) {
                    effect.apply(scoped);
                }
            }

            applied++;
            if (applied >= safeMaxTargets) return;
        }
    }

    private static boolean matchesAny(Entity entity, List<String> categories) {
        if (categories == null || categories.isEmpty()) return true;
        for (String category : categories) {
            if (matches(entity, category)) {
                return true;
            }
        }

        return false;
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