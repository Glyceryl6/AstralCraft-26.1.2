package com.astral_craft.client.gui.board;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.gameplay.board.BoardTemplateData;
import com.astral_craft.common.gameplay.board.BoardTemplatePlacement;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Ground footprint preview for a saved one-use board projector. */
public class BoardTemplatePreviewRenderer {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int VALID_COLOR = 0xB050F07A;
    private static final int INVALID_COLOR = 0xB0F05A5A;
    private static final double HALF_WIDTH = 0.025D;

    public static void submit(SubmitCustomGeometryEvent event) {
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
        corner = new Vec3(corner.x, origin.getY() + 0.025D, corner.z);
        boolean valid = BoardTemplatePlacement.canPlace(minecraft.level, groundPos, facing, template)
                && !BoardProtectionWorldRenderer.intersects(BoardTemplatePlacement.boardArea(groundPos, facing, template));
        int color = valid ? VALID_COLOR : INVALID_COLOR;
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        for (int x = 0; x <= template.width(); x++) {
            Vec3 start = corner.add(axisX.scale(x));
            submitLine(event, poseStack, cameraPos, start, start.add(axisZ.scale(template.depth())), color);
        }

        for (int z = 0; z <= template.depth(); z++) {
            Vec3 start = corner.add(axisZ.scale(z));
            submitLine(event, poseStack, cameraPos, start, start.add(axisX.scale(template.width())), color);
        }
    }

    private static ItemStack projectorStack(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(AstralItems.BOARD_PROJECTOR.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(AstralItems.BOARD_PROJECTOR.get()) ? offhand : ItemStack.EMPTY;
    }

    private static void submitLine(SubmitCustomGeometryEvent event, PoseStack poseStack, Vec3 cameraPos, Vec3 start, Vec3 end, int color) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) return;
        direction = direction.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).scale(HALF_WIDTH);
        Vec3 a = start.subtract(side).subtract(cameraPos);
        Vec3 b = end.subtract(side).subtract(cameraPos);
        Vec3 c = end.add(side).subtract(cameraPos);
        Vec3 d = start.add(side).subtract(cameraPos);
        event.getSubmitNodeCollector().order(2).submitCustomGeometry(
                poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, consumer) -> {
                    Vec3 normal = new Vec3(0.0D, 1.0D, 0.0D);
                    EffectRenderGeometry.vertex(consumer, pose, a, color, 0.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, b, color, 1.0F, 0.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, c, color, 1.0F, 1.0F, normal);
                    EffectRenderGeometry.vertex(consumer, pose, d, color, 0.0F, 1.0F, normal);
                });
    }

}