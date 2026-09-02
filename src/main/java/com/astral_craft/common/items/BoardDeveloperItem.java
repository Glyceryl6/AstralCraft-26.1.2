package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardDeveloperService;
import com.astral_craft.common.gameplay.board.BoardPhase;
import com.astral_craft.common.gameplay.board.BoardProtectionService;
import com.astral_craft.common.gameplay.board.BoardSavedData;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class BoardDeveloperItem extends Item {

    public BoardDeveloperItem(Properties properties) {
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
        if (BoardDeveloperService.active(session.id()) && !BoardDeveloperService.ownedBy(session.id(), player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.other_players"), true);
            return InteractionResult.FAIL;
        }
        if (BoardDeveloperService.hasOtherHuman(session, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.other_players"), true);
            return InteractionResult.FAIL;
        }

        ServerLevel level = player.level();
        if (session.phase() == BoardPhase.PLAYING) {
            if (session.participantByController(player.getUUID()).isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.other_players"), true);
                return InteractionResult.FAIL;
            }
            if (!BoardDeveloperService.canEditLive(player, session)) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.busy"), true);
                return InteractionResult.FAIL;
            }
            BoardDeveloperService.begin(player, session);
            if (!BoardDeveloperService.openConfiguration(player, session)) BoardDeveloperService.resume(level, session);
            return InteractionResult.SUCCESS;
        }

        if (session.phase() == BoardPhase.FINISHED) BoardSessionManager.resetForLobby(level, session);
        if (session.phase() == BoardPhase.CHARACTER_SELECTION && !BoardDeveloperService.active(session.id())) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.developer.busy"), true);
            return InteractionResult.FAIL;
        }
        if (session.phase() == BoardPhase.READY) {
            session.setProtectionEnabled(true);
            session.setPhase(BoardPhase.CHARACTER_SELECTION);
            session.setLobbyDeadlineTick(AstralServerTickClock.now(level) + BoardSessionManager.LOBBY_TIMEOUT_TICKS);
            BoardSessionManager.markChanged(level);
            BoardProtectionService.refreshProtectedAreas(level, BoardSavedData.get(level));
        }
        if (session.phase() != BoardPhase.CHARACTER_SELECTION) return InteractionResult.FAIL;

        BoardDeveloperService.begin(player, session);
        if (!BoardDeveloperService.openConfiguration(player, session)) {
            BoardDeveloperService.clear(session.id());
            BoardSessionManager.resetForLobby(level, session);
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }
}
