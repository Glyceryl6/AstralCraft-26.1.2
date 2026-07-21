package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardPhase;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardSpectatorService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class BoardSpectatorItem extends Item {

    public BoardSpectatorItem(Properties properties) {
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

        if (session.phase() != BoardPhase.PLAYING) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.spectator.not_playing"), true);
            return InteractionResult.FAIL;
        }

        boolean watching = BoardSpectatorService.toggle(player, session, context.getItemInHand());
        player.sendSystemMessage(Component.translatable(watching
                ? "message.astral_craft.board.spectator.bound"
                : "message.astral_craft.board.spectator.unbound"), true);
        return InteractionResult.SUCCESS;
    }

}