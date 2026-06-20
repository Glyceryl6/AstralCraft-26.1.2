package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

public record SummonEntityEventEffect(HolderSet<EntityType<?>> entityTypes, int count, double spread) implements AstralEventEffect {

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
        BlockPos blockPos = context.origin();
        for (int i = 0; i < safeCount; i++) {
            Optional<Holder<EntityType<?>>> entityType = this.entityTypes().getRandomElement(context.random());
            if (entityType.isEmpty()) continue;
            Entity entity = entityType.get().value().spawn(level, blockPos, EntitySpawnReason.TRIGGERED);
            if (entity != null) {
                double offsetX = safeSpread <= 0.0D ? 0.0D : (context.random().nextDouble() - 0.5D) * safeSpread * 2.0D;
                double offsetZ = safeSpread <= 0.0D ? 0.0D : (context.random().nextDouble() - 0.5D) * safeSpread * 2.0D;
                entity.snapTo(blockPos.getX() + 0.5D + offsetX,
                        blockPos.getY(), blockPos.getZ() + 0.5D + offsetZ,
                        context.random().nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(entity);
            }
        }
    }

}