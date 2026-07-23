package com.astral_craft.common.items;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.network.c2s.BoardProjectorConfirmPayload;
import com.astral_craft.common.network.s2c.OpenBoardProjectorConfirmPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Saves an existing scanned board and later projects a one-use copy onto suitable terrain. */
public class BoardProjectorItem extends Item {

    public BoardProjectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        ServerLevel level = player.level();
        BoardSession existing = BoardSessionManager.findAt(level, context.getClickedPos()).orElse(null);
        if (existing != null && existing.positions().values().stream()
                .anyMatch(pos -> level.getBlockState(pos).getBlock() instanceof BasePlatform)) {
            return this.saveBoard(player, context.getItemInHand(), existing);
        }

        BoardTemplateData template = context.getItemInHand().get(AstralDataComponents.BOARD_TEMPLATE.get());
        if (template == null || !template.valid()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.empty")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }

        Direction facing = BoardTemplatePlacement.horizontal(player.getDirection());
        if (context.getClickedFace() != Direction.UP || !canPlace(level, context.getClickedPos(), facing, template)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.invalid_terrain")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        PacketDistributor.sendToPlayer(player, new OpenBoardProjectorConfirmPayload(context.getClickedPos(), facing,
                context.getHand() == InteractionHand.OFF_HAND, template.width(), template.depth(), template.panelCount()));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult saveBoard(ServerPlayer player, ItemStack stack, BoardSession session) {
        List<BlockPos> positions = session.positions().values().stream()
                .filter(pos -> player.level().getBlockState(pos).getBlock() instanceof BasePlatform)
                .sorted(Comparator.comparingInt(Vec3i::getX).thenComparingInt(Vec3i::getZ)).toList();
        if (positions.isEmpty()) return InteractionResult.FAIL;
        int minX = positions.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxX = positions.stream().mapToInt(BlockPos::getX).max().orElse(minX);
        int maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElse(minZ);
        int panelY = positions.getFirst().getY();
        List<BoardTemplateData.TemplateBlock> blocks = positions.stream()
                .map(pos -> new BoardTemplateData.TemplateBlock(
                        new BlockPos(pos.getX() - minX, pos.getY() - panelY, pos.getZ() - minZ),
                        player.level().getBlockState(pos)))
                .toList();
        BoardTemplateData template = new BoardTemplateData(maxX - minX + 1, maxZ - minZ + 1,
                blocks.size(), blocks);
        if (!template.valid()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.too_large")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        stack.set(AstralDataComponents.BOARD_TEMPLATE.get(), template);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.saved",
                template.panelCount(), template.width(), template.depth()).withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    public static void confirmPlacement(ServerPlayer player, BoardProjectorConfirmPayload payload) {
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BoardProjectorItem)) return;
        BoardTemplateData template = stack.get(AstralDataComponents.BOARD_TEMPLATE.get());
        if (template == null || !template.valid()) return;
        ServerLevel level = player.level();
        if (player.distanceToSqr(Vec3.atCenterOf(payload.groundPos())) > 64.0D) return;
        Direction facing = BoardTemplatePlacement.horizontal(payload.facing());
        if (!canPlace(level, payload.groundPos(), facing, template)) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.invalid_terrain")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        List<BoardTemplatePlacement.PlacedBlock> blocks = BoardTemplatePlacement.transformedBlocks(
                payload.groundPos(), facing, template);
        if (blocks.isEmpty()) return;
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        for (BoardTemplatePlacement.PlacedBlock block : blocks) {
            previous.put(block.pos(), level.getBlockState(block.pos()));
            level.setBlock(block.pos(), block.state(), 3);
        }

        ScannedBoard scanned = BoardScanner.scan(level, blocks.getFirst().pos());
        if (!scanned.isValid()) {
            previous.forEach((pos, state) -> level.setBlock(pos, state, 3));
            player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.scan_failed")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        BoardSavedData data = BoardSavedData.get(level);
        BoardSession session = new BoardSession(UUID.randomUUID(), level.dimension(), scanned);
        data.put(session);
        BoardProtectionService.refreshProtectedAreas(level, data);
        BoardSessionManager.syncBoardSnapshot(level, session);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board_projector.created",
                scanned.nodes().size(), scanned.area().width(), scanned.area().depth())
                .withStyle(ChatFormatting.GREEN), false);
    }

    private static boolean canPlace(ServerLevel level, BlockPos groundPos, Direction facing, BoardTemplateData template) {
        if (!BoardTemplatePlacement.canPlace(level, groundPos, facing, template)) return false;
        BoardArea targetArea = BoardTemplatePlacement.boardArea(groundPos, facing, template);
        return BoardSavedData.get(level).sessions().stream()
                .noneMatch(session -> session.protectedArea().intersects(targetArea));
    }
}
