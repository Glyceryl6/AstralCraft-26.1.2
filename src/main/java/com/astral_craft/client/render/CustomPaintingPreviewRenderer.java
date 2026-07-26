package com.astral_craft.client.render;

import com.astral_craft.client.gui.CustomPaintingConfigScreen;
import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.astral_craft.common.gameplay.CustomPaintingPlacement;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
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
    private static int editingEntityId = -1;
    private static CustomPaintingData editingData = CustomPaintingData.EMPTY;

    public static void beginEditing(int entityId, CustomPaintingData data) {
        editingEntityId = entityId;
        editingData = data == null ? CustomPaintingData.EMPTY : data;
    }

    public static void updateEditing(CustomPaintingData data) {
        if (editingEntityId >= 0 && data != null) editingData = data;
    }

    public static void endEditing() {
        editingEntityId = -1;
        editingData = CustomPaintingData.EMPTY;
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
        Preview preview = editingPreview(minecraft);
        if (preview == null) preview = heldPreview(minecraft);
        if (preview == null) return;
        boolean valid = CustomPaintingPlacement.canPlace(minecraft.level, preview.support(), preview.facing(),
                preview.data(), preview.ignored());
        renderGrid(event, preview.support(), preview.facing(), preview.data(), valid);
    }

    private static Preview editingPreview(Minecraft minecraft) {
        if (!(minecraft.screen instanceof CustomPaintingConfigScreen) || editingEntityId < 0) return null;
        Entity entity = minecraft.level.getEntity(editingEntityId);
        if (!(entity instanceof CustomPaintingEntity painting)) return null;
        return new Preview(painting.supportPos(), painting.facing(), editingData, painting);
    }

    private static Preview heldPreview(Minecraft minecraft) {
        if (minecraft.screen != null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getDirection().getAxis().isHorizontal()) return null;
        ItemStack stack = paintingStack(minecraft);
        if (stack.isEmpty()) return null;
        CustomPaintingData data = stack.getOrDefault(AstralDataComponents.CUSTOM_PAINTING.get(), CustomPaintingData.EMPTY);
        return new Preview(hit.getBlockPos(), hit.getDirection(), data, minecraft.player);
    }

    private static void renderGrid(SubmitCustomGeometryEvent event, BlockPos support, Direction facing,
                                   CustomPaintingData data, boolean valid) {
        Direction right = CustomPaintingPlacement.right(facing);
        Vec3 horizontal = new Vec3(right.getStepX(), 0.0D, right.getStepZ());
        Vec3 vertical = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        int glow = valid ? VALID_GLOW : INVALID_GLOW;
        int core = valid ? VALID_CORE : INVALID_CORE;
        Vec3 bottomLeft = CustomPaintingPlacement.bottomLeft(support, facing)
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
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(AstralItems.CUSTOM_PAINTING.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(AstralItems.CUSTOM_PAINTING.get()) ? offhand : ItemStack.EMPTY;
    }

    private static void submitLine(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 start, Vec3 end,
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

    private record Preview(BlockPos support, Direction facing, CustomPaintingData data, Entity ignored) {}
}
