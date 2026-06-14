package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.PlatformPanelMapper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class BoardProjectorItem extends Item {

    public BoardProjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        if (!PlatformPanelMapper.isPlatform(context.getLevel().getBlockState(context.getClickedPos()).getBlock())) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_panel"), true);
            return InteractionResult.SUCCESS;
        }

        return BoardSessionManager.startFromPanel(player, context.getClickedPos()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

}