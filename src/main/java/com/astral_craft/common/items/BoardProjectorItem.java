package com.astral_craft.common.items;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardSavedData;
import com.astral_craft.common.gameplay.board.BoardScanner;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardProtectionService;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.ScannedBoard;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.UUID;

public class BoardProjectorItem extends Item {

    public BoardProjectorItem(Properties properties) {
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

        BoardSavedData data = BoardSavedData.get(level);
        for (BoardSession existing : data.sessions()) {
            if (existing.protectedArea().intersects(scanned.area())) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.board.overlap")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }

        BoardSession session = new BoardSession(UUID.randomUUID(), level.dimension(), scanned);
        data.put(session);
        BoardProtectionService.refreshProtectedAreas(level, data);
        BoardSessionManager.syncBoardSnapshot(level, session);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.started",
                        scanned.nodes().size(), scanned.startNodes().size(),
                        session.hologramCenter().getX() + ", " + session.hologramCenter().getY() + ", "
                                + session.hologramCenter().getZ())
                .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.SUCCESS;
    }

}