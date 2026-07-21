package com.astral_craft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Legacy compatibility item. Start points are now assigned automatically from the scanned board graph. */
public class BoardStartMarkerItem extends Item {

    public BoardStartMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.automatic"), true);
        }
        return InteractionResult.SUCCESS;
    }

}
