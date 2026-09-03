package com.astral_craft.common.items;

import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class ExhibitionCharacterToolItem extends Item {

    public ExhibitionCharacterToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.FAIL;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;
        Vec3 location = context.getClickLocation();
        ExhibitionCharacterEntity entity = new ExhibitionCharacterEntity(AstralEntities.EXHIBITION_CHARACTER.get(), level);
        entity.setPos(location.x, location.y, location.z);
        entity.setExhibitionYaw(player.getYRot() + 180.0F);
        level.addFreshEntity(entity);
        entity.openConfiguration(player);
        return InteractionResult.SUCCESS;
    }
}
