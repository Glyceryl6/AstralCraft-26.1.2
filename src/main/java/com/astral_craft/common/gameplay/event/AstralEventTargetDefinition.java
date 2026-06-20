package com.astral_craft.common.gameplay.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public record AstralEventTargetDefinition(String scope, double radius, List<String> entityCategories, List<Identifier> entityTypes, boolean includeTriggerPlayer, boolean livingOnly) {

    public static final AstralEventTargetDefinition DEFAULT = new AstralEventTargetDefinition("trigger_player", 16.0D, List.of(), List.of(), true, true);

    public static final Codec<AstralEventTargetDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("scope", "trigger_player").forGetter(AstralEventTargetDefinition::scope),
            Codec.DOUBLE.optionalFieldOf("radius", 16.0D).forGetter(AstralEventTargetDefinition::radius),
            Codec.STRING.listOf().optionalFieldOf("entity_categories", List.of()).forGetter(AstralEventTargetDefinition::entityCategories),
            Identifier.CODEC.listOf().optionalFieldOf("entity_types", List.of()).forGetter(AstralEventTargetDefinition::entityTypes),
            Codec.BOOL.optionalFieldOf("include_trigger_player", true).forGetter(AstralEventTargetDefinition::includeTriggerPlayer),
            Codec.BOOL.optionalFieldOf("living_only", true).forGetter(AstralEventTargetDefinition::livingOnly)
    ).apply(instance, AstralEventTargetDefinition::new));

    public List<Entity> resolve(ServerPlayer triggerPlayer) {
        List<Entity> result = new ArrayList<>();
        if (triggerPlayer == null) return result;
        String normalized = this.scope == null ? "trigger_player" : this.scope;
        switch (normalized) {
            case "all_players", "world_players" -> result.addAll(triggerPlayer.server.getPlayerList().getPlayers());
            case "dimension_players" -> triggerPlayer.server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.level() == triggerPlayer.level()).forEach(result::add);
            case "nearby_players" -> {
                double safeRadius = Math.max(1.0D, this.radius);
                AABB bounds = triggerPlayer.getBoundingBox().inflate(safeRadius);
                for (ServerPlayer player : triggerPlayer.server.getPlayerList().getPlayers()) {
                    if (player.level() == triggerPlayer.level()
                            && (this.includeTriggerPlayer || !player.getUUID().equals(triggerPlayer.getUUID()))
                            && bounds.contains(player.position())) {
                        result.add(player);
                    }
                }
            }

            case "nearby_entities", "nearby_mobs", "nearby_animals" -> {
                double safeRadius = Math.max(1.0D, this.radius);
                AABB bounds = triggerPlayer.getBoundingBox().inflate(safeRadius);
                result.addAll(triggerPlayer.level().getEntities(triggerPlayer, bounds, this::matchesEntity));
                if (this.includeTriggerPlayer && this.matchesEntity(triggerPlayer)) {
                    result.add(triggerPlayer);
                }
            }

            default -> result.add(triggerPlayer);
        }

        return result.stream().filter(this::matchesEntity).distinct().toList();
    }

    public boolean matchesEntity(Entity entity) {
        if (entity == null) return false;
        if (this.livingOnly && !(entity instanceof LivingEntity)) return false;
        if ("nearby_mobs".equalsIgnoreCase(this.scope) && !(entity instanceof Mob)) return false;
        if ("nearby_animals".equalsIgnoreCase(this.scope) && !(entity instanceof Animal)) return false;
        if (!this.entityTypes.isEmpty()) {
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (!this.entityTypes.contains(typeId)) return false;
        }

        if (this.entityCategories.isEmpty()) return true;
        for (String category : this.entityCategories) {
            if (matchesCategory(entity, category)) return true;
        }

        return false;
    }

    private static boolean matchesCategory(Entity entity, String category) {
        if (category == null || category.isBlank() || "any".equalsIgnoreCase(category)) return true;
        String normalized = category.toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "player", "players" -> entity instanceof ServerPlayer;
            case "living" -> entity instanceof LivingEntity;
            case "mob", "mobs" -> entity instanceof Mob;
            case "monster", "monsters", "hostile" -> entity instanceof Monster;
            case "animal", "animals", "passive" -> entity instanceof Animal;
            default -> false;
        };
    }

}