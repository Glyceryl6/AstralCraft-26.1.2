package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardSavedData;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardRouteService;
import com.astral_craft.common.gameplay.board.BoardProtectionService;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        BoardSession session = BoardSessionManager.findAt(player.level(), context.getClickedPos()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_registered"), true);
            return InteractionResult.FAIL;
        }

        ServerLevel level = player.level();
        boolean deleteDefinition = player.isShiftKeyDown();
        BoardRouteService.broadcastState(session, false, "", "", "");
        BoardSessionManager.resetForLobby(level, session);
        session.setProtectionEnabled(false);
        BoardSavedData data = BoardSavedData.get(level);
        if (deleteDefinition) {
            BoardSessionManager.syncBoardSnapshot(level, session);
            data.remove(session.id());
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.deleted")
                    .withStyle(ChatFormatting.YELLOW), true);
        } else {
            BoardSessionManager.markChanged(level);
            BoardSessionManager.syncBoardSnapshot(level, session);
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.protection_disabled")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        BoardProtectionService.refreshProtectedAreas(level, data);
        return InteractionResult.SUCCESS;
    }

}