package com.astral_craft.client.gui.board;

import com.astral_craft.common.gameplay.board.BoardTemplateData;
import com.astral_craft.common.gameplay.board.BoardTemplatePlacement;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ground footprint preview for a saved one-use board projector. */
public class BoardTemplatePreviewRenderer {

    private static final int VALID_GLOW_COLOR = 0x8040FF72;
    private static final int VALID_CORE_COLOR = 0xFFB8FFC9;
    private static final int INVALID_GLOW_COLOR = 0x80FF3D4F;
    private static final int INVALID_CORE_COLOR = 0xFFFFB8C0;
    private static final float GLOW_WIDTH = 5.0F;
    private static final float CORE_WIDTH = 2.25F;

    public static void submit() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen != null || !(minecraft.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK || hitResult.getDirection() != Direction.UP) return;
        ItemStack stack = projectorStack(minecraft);
        if (stack.isEmpty()) return;
        BoardTemplateData template = stack.get(AstralDataComponents.BOARD_TEMPLATE.get());
        if (template == null || !template.valid()) return;
        BlockPos groundPos = hitResult.getBlockPos();
        Direction facing = BoardTemplatePlacement.horizontal(minecraft.player.getDirection());
        BlockPos origin = BoardTemplatePlacement.origin(groundPos, facing, template);
        Direction xDirection = BoardTemplatePlacement.xDirection(facing);
        Vec3 axisX = new Vec3(xDirection.getStepX(), 0.0D, xDirection.getStepZ());
        Vec3 axisZ = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());
        Vec3 firstCenter = Vec3.atCenterOf(origin);
        Vec3 corner = firstCenter.subtract(axisX.scale(0.5D)).subtract(axisZ.scale(0.5D));
        corner = new Vec3(corner.x, origin.getY() + 0.145D, corner.z);
        boolean valid = BoardTemplatePlacement.canPlace(minecraft.level, groundPos, facing, template)
                && !BoardProtectionWorldRenderer.intersects(BoardTemplatePlacement.boardArea(groundPos, facing, template));
        int glowColor = valid ? VALID_GLOW_COLOR : INVALID_GLOW_COLOR;
        int coreColor = valid ? VALID_CORE_COLOR : INVALID_CORE_COLOR;
        for (int x = 0; x <= template.width(); x++) {
            Vec3 start = corner.add(axisX.scale(x));
            Vec3 end = start.add(axisZ.scale(template.depth()));
            submitLine(start, end, glowColor, GLOW_WIDTH);
            submitLine(start, end, coreColor, CORE_WIDTH);
        }

        for (int z = 0; z <= template.depth(); z++) {
            Vec3 start = corner.add(axisZ.scale(z));
            Vec3 end = start.add(axisX.scale(template.width()));
            submitLine(start, end, glowColor, GLOW_WIDTH);
            submitLine(start, end, coreColor, CORE_WIDTH);
        }
    }

    private static ItemStack projectorStack(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(AstralItems.BOARD_PROJECTOR.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(AstralItems.BOARD_PROJECTOR.get()) ? offhand : ItemStack.EMPTY;
    }

    private static void submitLine(Vec3 start, Vec3 end, int color, float width) {
        if (start.distanceToSqr(end) < 1.0E-8D) return;
        Gizmos.line(start, end, color, width).setAlwaysOnTop();
    }

}