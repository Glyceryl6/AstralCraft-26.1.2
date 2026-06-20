package com.astral_craft.common.gameplay.event.conditions;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventCondition;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record HasItemEventCondition(Identifier id, int count, boolean inverted) implements AstralEventCondition {

    public static final MapCodec<HasItemEventCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(HasItemEventCondition::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(HasItemEventCondition::count),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(HasItemEventCondition::inverted)
    ).apply(instance, HasItemEventCondition::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("has_item").toString();
    }

    @Override
    public MapCodec<? extends AstralEventCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(AstralEventContext context) {
        ServerPlayer player = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (player == null) return this.inverted;
        Item item = BuiltInRegistries.ITEM.getValue(this.id);
        int found = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= Math.max(1, this.count)) {
                    return !this.inverted;
                }
            }
        }

        return this.inverted;
    }

}