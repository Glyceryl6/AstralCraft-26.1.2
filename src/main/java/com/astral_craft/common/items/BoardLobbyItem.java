package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        if (!session.mechanics().hasCompleteCharacterStarts()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.incomplete",
                    session.mechanics().characterStartNodes().size(), 4), true);
            return InteractionResult.FAIL;
        }

        ServerLevel level = player.level();
        if (session.phase() == BoardPhase.FINISHED) BoardSessionManager.resetForLobby(level, session);
        if (session.phase() == BoardPhase.READY) {
            session.setProtectionEnabled(true);
            session.setPhase(BoardPhase.CHARACTER_SELECTION);
            session.setLobbyDeadlineTick(level.getGameTime() + BoardSessionManager.LOBBY_TIMEOUT_TICKS);
            BoardSessionManager.markChanged(level);
            BoardProtectionService.refreshProtectedAreas(level, BoardSavedData.get(level));
        } else if (session.lobbyDeadlineTick() <= 0L) {
            session.setLobbyDeadlineTick(level.getGameTime() + BoardSessionManager.LOBBY_TIMEOUT_TICKS);
            BoardSessionManager.markChanged(level);
        }
        BoardLobbyService.registerViewer(player, session);
        return InteractionResult.SUCCESS;
    }

}