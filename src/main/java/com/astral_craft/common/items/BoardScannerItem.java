package com.astral_craft.common.items;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.network.s2c.OpenBoardModeSelectionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class BoardScannerItem extends Item {

    public BoardScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof BasePlatform)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_panel"), true);
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = player.level();
        ScannedBoard scanned = BoardScanner.scan(level, context.getClickedPos());
        if (!scanned.isValid()) {
            MutableComponent reasons = Component.empty();
            for (int index = 0; index < scanned.errors().size(); index++) {
                if (index > 0) reasons.append(Component.translatable("text.astral_craft.list_separator"));
                reasons.append(Component.translatable("message.astral_craft.board.scan_error." + scanned.errors().get(index)));
            }

            player.sendSystemMessage(Component.translatable("message.astral_craft.board.invalid", reasons)
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.FAIL;
        }

        if (!scanned.mode().decided()) {
            PacketDistributor.sendToPlayer(player, new OpenBoardModeSelectionPayload(context.getClickedPos()));
            return InteractionResult.SUCCESS;
        }
        return createBoard(player, context.getClickedPos(), scanned.mode()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    public static void chooseMode(ServerPlayer player, BlockPos origin, BoardMode mode) {
        if (player == null || origin == null || mode == null || !mode.decided()) return;
        if (player.distanceToSqr(Vec3.atCenterOf(origin)) > 64.0D) return;
        createBoard(player, origin, mode);
    }

    private static boolean createBoard(ServerPlayer player, BlockPos origin, BoardMode requestedMode) {
        ServerLevel level = player.level();
        ScannedBoard scanned = BoardScanner.scan(level, origin);
        if (!scanned.isValid()) {
            MutableComponent reasons = Component.empty();
            for (int index = 0; index < scanned.errors().size(); index++) {
                if (index > 0) reasons.append(Component.translatable("text.astral_craft.list_separator"));
                reasons.append(Component.translatable("message.astral_craft.board.scan_error." + scanned.errors().get(index)));
            }
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.invalid", reasons)
                    .withStyle(ChatFormatting.RED), false);
            return false;
        }
        if (scanned.mode().decided() && scanned.mode() != requestedMode) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.mode_changed")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        BoardSavedData data = BoardSavedData.get(level);
        if (data.sessions().stream().anyMatch(existing -> existing.protectedArea().intersects(scanned.area()))) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.overlap")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        BoardSession session = new BoardSession(UUID.randomUUID(), level.dimension(), scanned.withMode(requestedMode));
        data.put(session);
        BoardProtectionService.refreshProtectedAreas(level, data);
        BoardSessionManager.syncBoardSnapshot(level, session);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.started",
                        scanned.nodes().size(), scanned.startNodes().size(),
                        session.hologramCenter().getX() + ", " + session.hologramCenter().getY() + ", "
                                + session.hologramCenter().getZ())
                .append(Component.translatable("message.astral_craft.board.mode_suffix." + requestedMode.getSerializedName()))
                .withStyle(ChatFormatting.GREEN), false);
        return true;
    }

}