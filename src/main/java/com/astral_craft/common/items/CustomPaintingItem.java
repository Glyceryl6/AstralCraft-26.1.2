package com.astral_craft.common.items;

import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class CustomPaintingItem extends Item {

    public CustomPaintingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();
        if (!direction.getAxis().isHorizontal()) return InteractionResult.FAIL;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        CustomPaintingData data = stack.getOrDefault(AstralDataComponents.CUSTOM_PAINTING.get(), CustomPaintingData.EMPTY);
        CustomPaintingEntity painting = new CustomPaintingEntity(level, context.getClickedPos(), direction, data);
        if (!painting.survives()) return InteractionResult.FAIL;
        level.addFreshEntity(painting);
        painting.playPlacementSound();
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

}