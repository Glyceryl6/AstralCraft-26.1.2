package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count, double spread, boolean silent,
                                      boolean respectDifficulty, boolean allowPeacefulMonsters) implements AstralEventEffect {

    public static final int MAX_POSITION_ATTEMPTS = 48;
    public static final int VERTICAL_SEARCH_RADIUS = 4;

    public static final MapCodec<SummonEntityEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity").forGetter(SummonEntityEventEffect::entityTypes),
            Codec.INT.optionalFieldOf("count", 1).forGetter(SummonEntityEventEffect::count),
            Codec.DOUBLE.optionalFieldOf("spread", 2.0D).forGetter(SummonEntityEventEffect::spread),
            Codec.BOOL.optionalFieldOf("silent", true).forGetter(SummonEntityEventEffect::silent),
            Codec.BOOL.optionalFieldOf("respect_difficulty", true).forGetter(SummonEntityEventEffect::respectDifficulty),
            Codec.BOOL.optionalFieldOf("allow_peaceful_monsters", false).forGetter(SummonEntityEventEffect::allowPeacefulMonsters)
    ).apply(instance, SummonEntityEventEffect::new));

    public SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count, double spread, boolean silent) {
        this(entityTypes, count, spread, silent, true, false);
    }

    public SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count, double spread) {
        this(entityTypes, count, spread, true, true, false);
    }

    public SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count) {
        this(entityTypes, count, 2.0D, true, true, false);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("summon_entity").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        ServerLevel level = context.level();
        if (level == null) return;
        int safeCount = Math.max(1, this.count);
        double safeSpread = Math.max(0.0D, this.spread);
        Set<BlockPos> usedPositions = new HashSet<>();
        for (int i = 0; i < safeCount; i++) {
            Optional<Holder<EntityType<?>>> entityType = this.entityTypes.getRandomElement(level.getRandom());
            if (entityType.isEmpty()) return;
            EntityType<?> type = entityType.get().value();
            if (!this.canSpawnInCurrentDifficulty(level, type, null)) continue;
            Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
            if (entity == null) continue;
            if (!this.canSpawnInCurrentDifficulty(level, type, entity)) {
                entity.discard();
                continue;
            }
            BlockPos spawnPos = this.findSpawnPos(context, level, entity, safeSpread, usedPositions);
            if (spawnPos == null) {
                entity.discard();
                continue;
            }
            entity.setUUID(UUID.randomUUID());
            entity.setSilent(this.silent);
            entity.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    context.random().nextFloat() * 360.0F, 0.0F);
            if (level.addFreshEntity(entity)) {
                usedPositions.add(spawnPos.immutable());
            } else {
                entity.discard();
            }
        }
    }

    private boolean canSpawnInCurrentDifficulty(ServerLevel level, EntityType<?> type, Entity entity) {
        if (!this.respectDifficulty || this.allowPeacefulMonsters || level.getDifficulty() != Difficulty.PEACEFUL) {
            return true;
        }
        return type.getCategory() != MobCategory.MONSTER && !(entity instanceof Monster);
    }

    private BlockPos findSpawnPos(AstralEventContext context, ServerLevel level, Entity entity, double safeSpread, Set<BlockPos> usedPositions) {
        BlockPos origin = context.origin();
        BlockPos direct = this.findVerticalSpawnPos(level, entity, origin, usedPositions);
        if (direct != null) return direct;
        int attempts = Math.max(MAX_POSITION_ATTEMPTS, this.count * 16);
        int horizontalRange = Math.max(1, (int) Math.ceil(Math.max(1.0D, safeSpread)));
        for (int attempt = 0; attempt < attempts; attempt++) {
            int offsetX = safeSpread <= 0.0D ? 0 : context.random().nextInt(horizontalRange * 2 + 1) - horizontalRange;
            int offsetZ = safeSpread <= 0.0D ? 0 : context.random().nextInt(horizontalRange * 2 + 1) - horizontalRange;
            if (offsetX == 0 && offsetZ == 0 && attempt > 0) continue;
            BlockPos candidate = origin.offset(offsetX, 0, offsetZ);
            BlockPos result = this.findVerticalSpawnPos(level, entity, candidate, usedPositions);
            if (result != null) return result;
        }

        return null;
    }

    private BlockPos findVerticalSpawnPos(ServerLevel level, Entity entity, BlockPos base, Set<BlockPos> usedPositions) {
        for (int dy = -VERTICAL_SEARCH_RADIUS; dy <= VERTICAL_SEARCH_RADIUS; dy++) {
            BlockPos candidate = base.offset(0, dy, 0);
            if (usedPositions.contains(candidate)) continue;
            if (this.isValidSpawnPos(level, entity, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isValidSpawnPos(ServerLevel level, Entity entity, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)) return false;
        if (!this.hasEmptyCollision(level, feet)) return false;
        if (!this.hasEmptyCollision(level, feet.above())) return false;
        entity.snapTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, 0.0F, 0.0F);
        return level.noCollision(entity);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasEmptyCollision(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

}
