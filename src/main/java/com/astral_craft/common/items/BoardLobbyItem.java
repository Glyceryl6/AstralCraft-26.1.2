package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class BoardLobbyItem extends Item {

    public BoardLobbyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        return BoardSessionManager.openCharacterSelection(player, context.getClickedPos())
                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
