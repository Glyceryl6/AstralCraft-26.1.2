package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class BoardDismantlerItem extends Item {

    public BoardDismantlerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        boolean delete = player.isShiftKeyDown();
        return BoardSessionManager.dismantle(player, context.getClickedPos(), delete)
                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
