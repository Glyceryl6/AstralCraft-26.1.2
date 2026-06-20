package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record DropItemEventEffect(Identifier id, int count) implements AstralEventEffect {

    public static final MapCodec<DropItemEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(DropItemEventEffect::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(DropItemEventEffect::count)
    ).apply(instance, DropItemEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("drop_item").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        if (context.level() == null) return;
        Item item = BuiltInRegistries.ITEM.getValue(this.id);
        ItemStack stack = new ItemStack(item, Math.max(1, this.count));
        ItemEntity entity = new ItemEntity(context.level(),
                context.origin().getX() + 0.5D,
                context.origin().getY() + 0.5D,
                context.origin().getZ() + 0.5D, stack);
        context.level().addFreshEntity(entity);
    }

}