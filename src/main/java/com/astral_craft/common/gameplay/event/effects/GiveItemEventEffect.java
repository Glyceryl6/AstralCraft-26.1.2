package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record GiveItemEventEffect(Identifier id, int count) implements AstralEventEffect {

    public static final MapCodec<GiveItemEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(GiveItemEventEffect::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(GiveItemEventEffect::count)
    ).apply(instance, GiveItemEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("give_item").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        ServerPlayer receiver = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (receiver == null) return;
        Item item = BuiltInRegistries.ITEM.getValue(this.id);
        ItemStack stack = new ItemStack(item, Math.max(1, this.count));
        if (!receiver.addItem(stack) && !stack.isEmpty()) {
            receiver.drop(stack, false);
        }
    }

}