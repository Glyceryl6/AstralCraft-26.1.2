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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count, double spread) implements AstralEventEffect {

    public static final int MAX_POSITION_ATTEMPTS = 48;
    public static final int VERTICAL_SEARCH_RADIUS = 4;

    public static final MapCodec<SummonEntityEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity").forGetter(SummonEntityEventEffect::entityTypes),
            Codec.INT.optionalFieldOf("count", 1).forGetter(SummonEntityEventEffect::count),
            Codec.DOUBLE.optionalFieldOf("spread", 2.0D).forGetter(SummonEntityEventEffect::spread)
    ).apply(instance, SummonEntityEventEffect::new));

    public SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count) {
        this(entityTypes, count, 2.0D);
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
        for (int i = 0; i < safeCount; i++) {
            Optional<Holder<EntityType<?>>> entityType = this.entityTypes.getRandomElement(level.getRandom());
            if (entityType.isEmpty()) return;
            Entity entity = entityType.get().value().spawn(level, context.origin(), EntitySpawnReason.TRIGGERED);
            if (entity == null) continue;
            BlockPos spawnPos = this.findSpawnPos(context, level, entity, safeSpread);
            if (spawnPos == null) continue;
            entity.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    context.random().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(entity);
        }
    }

    private BlockPos findSpawnPos(AstralEventContext context, ServerLevel level, Entity entity, double safeSpread) {
        BlockPos origin = context.origin();
        BlockPos direct = this.findVerticalSpawnPos(level, entity, origin);
        if (direct != null) return direct;
        int attempts = Math.max(MAX_POSITION_ATTEMPTS, this.count * 16);
        int horizontalRange = Math.max(1, (int) Math.ceil(Math.max(1.0D, safeSpread)));
        for (int attempt = 0; attempt < attempts; attempt++) {
            int offsetX = safeSpread <= 0.0D ? 0 : context.random().nextInt(horizontalRange * 2 + 1) - horizontalRange;
            int offsetZ = safeSpread <= 0.0D ? 0 : context.random().nextInt(horizontalRange * 2 + 1) - horizontalRange;
            if (offsetX == 0 && offsetZ == 0 && attempt > 0) continue;
            BlockPos candidate = origin.offset(offsetX, 0, offsetZ);
            BlockPos result = this.findVerticalSpawnPos(level, entity, candidate);
            if (result != null) return result;
        }

        return null;
    }

    private BlockPos findVerticalSpawnPos(ServerLevel level, Entity entity, BlockPos base) {
        for (int dy = -VERTICAL_SEARCH_RADIUS; dy <= VERTICAL_SEARCH_RADIUS; dy++) {
            BlockPos candidate = base.offset(0, dy, 0);
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