package com.astral_craft.common.items;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.network.c2s.BoardDismantleConfirmPayload;
import com.astral_craft.common.network.s2c.OpenBoardDismantleConfirmPayload;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

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
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToPlayer(player, new OpenBoardDismantleConfirmPayload(session.id(), session.positions().size()));
            return InteractionResult.SUCCESS;
        }

        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        BoardSessionManager.resetForLobby(level, session);
        session.setProtectionEnabled(false);
        BoardSavedData data = BoardSavedData.get(level);
        BoardSessionManager.markChanged(level);
        BoardSessionManager.syncBoardSnapshot(level, session);
        BoardProtectionService.refreshProtectedAreas(level, data);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.protection_disabled")
                .withStyle(ChatFormatting.YELLOW), true);
        return InteractionResult.SUCCESS;
    }

    public static void confirmDelete(ServerPlayer player, UUID boardId, BoardDismantleConfirmPayload.@Nullable Action action) {
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (session == null || action == null || !holdsDismantler(player)) return;
        BlockPos center = session.protectedArea().center();
        if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) > 64.0D * 64.0D) return;
        ServerLevel level = player.level();
        BoardRouteService.broadcastState(session, false, List.of(), List.of(), List.of());
        BoardSessionManager.resetForLobby(level, session);
        session.setProtectionEnabled(false);
        if (action == BoardDismantleConfirmPayload.Action.REMOVE_DATA_AND_PANELS) {
            for (BlockPos pos : session.positions().values()) {
                if (level.getBlockState(pos).getBlock() instanceof BasePlatform) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        BoardSavedData data = BoardSavedData.get(level);
        BoardSessionManager.syncBoardSnapshot(level, session);
        data.remove(session.id());
        BoardProtectionService.refreshProtectedAreas(level, data);
        String messageKey = action == BoardDismantleConfirmPayload.Action.REMOVE_DATA_AND_PANELS
                ? "message.astral_craft.board.deleted_with_panels"
                : "message.astral_craft.board.deleted_data_only";
        player.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.YELLOW), true);
    }

    private static boolean holdsDismantler(ServerPlayer player) {
        return player.getMainHandItem().is(AstralItems.BOARD_DISMANTLER.get())
                || player.getOffhandItem().is(AstralItems.BOARD_DISMANTLER.get());
    }

}