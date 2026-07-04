package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.gameplay.event.type.AstralEventEntityCategories;
import com.astral_craft.common.gameplay.event.type.AstralEventIdentifiers;
import com.astral_craft.common.gameplay.event.type.AstralEventTargetScopes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public record AstralEventTargetDefinition(Identifier scope, double radius, List<Identifier> entityCategories, List<Identifier> entityTypes, boolean includeTriggerPlayer, boolean livingOnly) {

    public static final AstralEventTargetDefinition DEFAULT = new AstralEventTargetDefinition(AstralEventTargetScopes.TRIGGER_PLAYER, 16.0D, List.of(), List.of(), true, true);

    public static final Codec<AstralEventTargetDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AstralEventIdentifiers.CODEC.optionalFieldOf("scope", AstralEventTargetScopes.TRIGGER_PLAYER).forGetter(AstralEventTargetDefinition::scope),
            Codec.DOUBLE.optionalFieldOf("radius", 16.0D).forGetter(AstralEventTargetDefinition::radius),
            AstralEventIdentifiers.CODEC.listOf().optionalFieldOf("entity_categories", List.of()).forGetter(AstralEventTargetDefinition::entityCategories),
            Identifier.CODEC.listOf().optionalFieldOf("entity_types", List.of()).forGetter(AstralEventTargetDefinition::entityTypes),
            Codec.BOOL.optionalFieldOf("include_trigger_player", true).forGetter(AstralEventTargetDefinition::includeTriggerPlayer),
            Codec.BOOL.optionalFieldOf("living_only", true).forGetter(AstralEventTargetDefinition::livingOnly)
    ).apply(instance, AstralEventTargetDefinition::new));

    public List<Entity> resolve(ServerPlayer triggerPlayer) {
        List<Entity> result = new ArrayList<>();
        if (triggerPlayer == null) return result;
        Identifier normalized = this.scope == null ? AstralEventTargetScopes.TRIGGER_PLAYER : this.scope;
        if (AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.ALL_PLAYERS) || AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.WORLD_PLAYERS)) {
            result.addAll(triggerPlayer.server.getPlayerList().getPlayers());
        } else if (AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.DIMENSION_PLAYERS)) {
            triggerPlayer.server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.level() == triggerPlayer.level()).forEach(result::add);
        } else if (AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.NEARBY_PLAYERS)) {
            double safeRadius = Math.max(1.0D, this.radius);
            AABB bounds = triggerPlayer.getBoundingBox().inflate(safeRadius);
            for (ServerPlayer player : triggerPlayer.server.getPlayerList().getPlayers()) {
                if (player.level() == triggerPlayer.level()
                        && (this.includeTriggerPlayer || !player.getUUID().equals(triggerPlayer.getUUID()))
                        && bounds.contains(player.position())) {
                    result.add(player);
                }
            }
        } else if (AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.NEARBY_ENTITIES)
                || AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.NEARBY_MOBS)
                || AstralEventIdentifiers.equals(normalized, AstralEventTargetScopes.NEARBY_ANIMALS)) {
            double safeRadius = Math.max(1.0D, this.radius);
            AABB bounds = triggerPlayer.getBoundingBox().inflate(safeRadius);
            result.addAll(triggerPlayer.level().getEntities(triggerPlayer, bounds, this::matchesEntity));
            if (this.includeTriggerPlayer && this.matchesEntity(triggerPlayer)) {
                result.add(triggerPlayer);
            }
        } else {
            result.add(triggerPlayer);
        }

        return result.stream().filter(this::matchesEntity).distinct().toList();
    }

    public boolean matchesEntity(Entity entity) {
        if (entity == null) return false;
        if (this.livingOnly && !(entity instanceof LivingEntity)) return false;
        if (AstralEventIdentifiers.equals(this.scope, AstralEventTargetScopes.NEARBY_MOBS) && !(entity instanceof Mob)) return false;
        if (AstralEventIdentifiers.equals(this.scope, AstralEventTargetScopes.NEARBY_ANIMALS) && !(entity instanceof Animal)) return false;
        if (!this.entityTypes.isEmpty()) {
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (!this.entityTypes.contains(typeId)) return false;
        }

        if (this.entityCategories.isEmpty()) return true;
        for (Identifier category : this.entityCategories) {
            if (AstralEventEntityCategories.matches(entity, category)) return true;
        }

        return false;
    }

}
