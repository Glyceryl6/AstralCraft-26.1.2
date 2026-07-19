package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardMechanicsState;
import com.astral_craft.common.gameplay.board.BoardPhase;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.Map;

/** Selects the four ordered character spawn panels from all public start panels on a board. */
public class BoardStartMarkerItem extends Item {

    public BoardStartMarkerItem(Properties properties) {
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
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.playing"), true);
            return InteractionResult.FAIL;
        }

        BoardMechanicsState mechanics = session.mechanics();
        if (player.isShiftKeyDown()) {
            mechanics.undoCharacterStart().ifPresentOrElse(_ ->
                    player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.undo",
                            mechanics.characterStartNodes().size()).withStyle(ChatFormatting.YELLOW), true), () ->
                    player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.empty"), true));
            BoardSessionManager.markChanged(player.level());
            BoardSessionManager.syncBoardSnapshot(player.level(), session);
            return InteractionResult.SUCCESS;
        }

        String nodeId = session.positions().entrySet().stream()
                .filter(entry -> entry.getValue().equals(context.getClickedPos()))
                .map(Map.Entry::getKey).findFirst().orElse("");
        if (!session.startNodes().contains(nodeId)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.start_marker.not_start"), true);
            return InteractionResult.FAIL;
        }

        BoardMechanicsState.MarkerResult result = mechanics.toggleCharacterStart(nodeId);
        switch (result) {
            case ADDED -> player.sendSystemMessage(Component.translatable(
                    "message.astral_craft.board.start_marker.added",
                            mechanics.characterStartNodes().size(),
                    mechanics.characterStartNodes().size(), 4)
                    .withStyle(ChatFormatting.GREEN), true);
            case REMOVED -> player.sendSystemMessage(Component.translatable(
                    "message.astral_craft.board.start_marker.removed",
                            mechanics.characterStartNodes().size(), 4)
                    .withStyle(ChatFormatting.YELLOW), true);
            case FULL -> player.sendSystemMessage(Component.translatable(
                    "message.astral_craft.board.start_marker.full")
                    .withStyle(ChatFormatting.RED), true);
            default -> {
                return InteractionResult.FAIL;
            }
        }

        BoardSessionManager.markChanged(player.level());
        BoardSessionManager.syncBoardSnapshot(player.level(), session);
        return InteractionResult.SUCCESS;
    }

}