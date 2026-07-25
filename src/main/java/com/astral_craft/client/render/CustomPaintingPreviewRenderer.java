package com.astral_craft.client.render;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.gameplay.CustomPaintingPlacement;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

public class CustomPaintingPreviewRenderer {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int VALID_GLOW = 0xB050FF90;
    private static final int VALID_CORE = 0xFFFFFFFF;
    private static final int INVALID_GLOW = 0xB0FF4B55;
    private static final int INVALID_CORE = 0xFFFFE0E2;

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !hit.getDirection().getAxis().isHorizontal()) return;
        ItemStack stack = paintingStack(minecraft);
        if (stack.isEmpty()) return;
        CustomPaintingData data = stack.getOrDefault(AstralDataComponents.CUSTOM_PAINTING.get(), CustomPaintingData.EMPTY);
        Direction facing = hit.getDirection();
        BlockPos support = hit.getBlockPos();
        boolean valid = CustomPaintingPlacement.canPlace(minecraft.level, support, facing, data, minecraft.player);
        Vec3 center = CustomPaintingPlacement.center(support, facing, data);
        Direction side = facing.getCounterClockWise();
        Vec3 horizontal = new Vec3(side.getStepX(), 0.0D, side.getStepZ());
        Vec3 vertical = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        int glow = valid ? VALID_GLOW : INVALID_GLOW;
        int core = valid ? VALID_CORE : INVALID_CORE;
        double halfWidth = data.width() * 0.5D;
        double halfHeight = data.height() * 0.5D;
        Vec3 bottomLeft = center.subtract(horizontal.scale(halfWidth)).subtract(vertical.scale(halfHeight))
                .add(facing.getStepX() * 0.02D, 0.0D, facing.getStepZ() * 0.02D);
        for (int x = 0; x <= data.width(); x++) {
            Vec3 start = bottomLeft.add(horizontal.scale(x));
            submitLine(event, camera, start, start.add(vertical.scale(data.height())), facing, glow, 0.045D);
            submitLine(event, camera, start, start.add(vertical.scale(data.height())), facing, core, 0.012D);
        }

        for (int y = 0; y <= data.height(); y++) {
            Vec3 start = bottomLeft.add(vertical.scale(y));
            submitLine(event, camera, start, start.add(horizontal.scale(data.width())), facing, glow, 0.045D);
            submitLine(event, camera, start, start.add(horizontal.scale(data.width())), facing, core, 0.012D);
        }
    }

    private static ItemStack paintingStack(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player != null) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.is(AstralItems.CUSTOM_PAINTING.get())) return mainHand;
            ItemStack offhand = player.getOffhandItem();
            return offhand.is(AstralItems.CUSTOM_PAINTING.get()) ? offhand : ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
    }

    private static void submitLine(
            SubmitCustomGeometryEvent event, Vec3 camera, Vec3 start, Vec3 end,
            Direction facing, int color, double halfWidth) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 normal = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());
        Vec3 side = direction.cross(normal).normalize().scale(halfWidth);
        Vec3 a = start.subtract(side).subtract(camera);
        Vec3 b = end.subtract(side).subtract(camera);
        Vec3 c = end.add(side).subtract(camera);
        Vec3 d = start.add(side).subtract(camera);
        PoseStack poseStack = event.getPoseStack();
        event.getSubmitNodeCollector().order(8).submitCustomGeometry(poseStack, RenderTypes.textSeeThrough(TEXTURE), (pose, consumer) -> {
            EffectRenderGeometry.vertex(consumer, pose, a, color, 0.0F, 0.0F, normal);
            EffectRenderGeometry.vertex(consumer, pose, b, color, 1.0F, 0.0F, normal);
            EffectRenderGeometry.vertex(consumer, pose, c, color, 1.0F, 1.0F, normal);
            EffectRenderGeometry.vertex(consumer, pose, d, color, 0.0F, 1.0F, normal);
        });
    }

}