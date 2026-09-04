package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardMatchmakingService;
import com.astral_craft.common.gameplay.board.BoardPhase;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.network.chat.Component;
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
        BoardSession session = BoardSessionManager.findAt(player.level(), context.getClickedPos()).orElse(null);
        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_registered"), true);
            return InteractionResult.FAIL;
        }
        if (session.phase() == BoardPhase.PLAYING) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.already_playing"), true);
            return InteractionResult.FAIL;
        }
        if (session.phase() == BoardPhase.FINISHED) BoardSessionManager.resetForLobby(player.level(), session);
        if (session.phase() != BoardPhase.READY) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.matchmaking.busy"), true);
            return InteractionResult.FAIL;
        }

        BoardMatchmakingService.openModeSelection(player, session);
        return InteractionResult.SUCCESS;
    }

}