package com.astral_craft.client.render;

import com.astral_craft.client.gui.CustomPaintingConfigScreen;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.astral_craft.common.gameplay.CustomPaintingPlacement;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CustomPaintingPreviewRenderer {

    private static final int VALID_GLOW = 0xC02CFF70;
    private static final int VALID_CORE = 0xFFC8FFD8;
    private static final int INVALID_GLOW = 0xC0FF3648;
    private static final int INVALID_CORE = 0xFFFFD5D9;
    private static final float GLOW_WIDTH = 7.0F;
    private static final float CORE_WIDTH = 3.5F;
    private static int editingEntityId = -1;
    private static CustomPaintingData editingData = CustomPaintingData.EMPTY;

    public static void beginEditing(int entityId, @Nullable CustomPaintingData data) {
        editingEntityId = entityId;
        editingData = data == null ? CustomPaintingData.EMPTY : data;
    }

    public static void updateEditing(@Nullable CustomPaintingData data) {
        if (editingEntityId >= 0 && data != null) editingData = data;
    }

    public static void endEditing() {
        editingEntityId = -1;
        editingData = CustomPaintingData.EMPTY;
    }

    public static void submit() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        Preview preview = editingPreview(minecraft);
        if (preview == null) preview = heldPreview(minecraft);
        if (preview == null) return;
        boolean valid = CustomPaintingPlacement.canPlace(
                minecraft.level, preview.support(), preview.facing(),
                preview.data(), preview.ignored());
        renderGrid(preview.support(), preview.facing(), preview.data(), valid);
    }

    @Nullable
    private static Preview editingPreview(Minecraft minecraft) {
        if (!(minecraft.screen instanceof CustomPaintingConfigScreen)
                || minecraft.level == null || editingEntityId < 0) return null;
        Entity entity = minecraft.level.getEntity(editingEntityId);
        if (!(entity instanceof CustomPaintingEntity painting)) return null;
        return new Preview(painting.supportPos(), painting.facing(), editingData, painting);
    }

    @Nullable
    private static Preview heldPreview(Minecraft minecraft) {
        if (minecraft.screen != null || minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !hit.getDirection().getAxis().isHorizontal()) return null;
        ItemStack stack = paintingStack(minecraft);
        if (stack.isEmpty()) return null;
        CustomPaintingData data = stack.getOrDefault(AstralDataComponents.CUSTOM_PAINTING.get(), CustomPaintingData.EMPTY);
        return new Preview(hit.getBlockPos(), hit.getDirection(), data, minecraft.player);
    }

    private static void renderGrid(BlockPos support, Direction facing, CustomPaintingData data, boolean valid) {
        Direction right = CustomPaintingPlacement.right(facing);
        Vec3 horizontal = new Vec3(right.getStepX(), 0.0D, right.getStepZ());
        Vec3 vertical = new Vec3(0.0D, 1.0D, 0.0D);
        int glow = valid ? VALID_GLOW : INVALID_GLOW;
        int core = valid ? VALID_CORE : INVALID_CORE;
        Vec3 bottomLeft = CustomPaintingPlacement.bottomLeft(support, facing)
                .add(facing.getStepX() * 0.025D, 0.0D, facing.getStepZ() * 0.025D);
        for (int x = 0; x <= data.width(); x++) {
            Vec3 start = bottomLeft.add(horizontal.scale(x));
            submitLine(start, start.add(vertical.scale(data.height())), glow, core);
        }

        for (int y = 0; y <= data.height(); y++) {
            Vec3 start = bottomLeft.add(vertical.scale(y));
            submitLine(start, start.add(horizontal.scale(data.width())), glow, core);
        }
    }

    private static ItemStack paintingStack(Minecraft minecraft) {
        if (minecraft.player == null) return ItemStack.EMPTY;
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(AstralItems.CUSTOM_PAINTING.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(AstralItems.CUSTOM_PAINTING.get()) ? offhand : ItemStack.EMPTY;
    }

    private static void submitLine(Vec3 start, Vec3 end, int glow, int core) {
        Gizmos.line(start, end, glow, GLOW_WIDTH).setAlwaysOnTop();
        Gizmos.line(start, end, core, CORE_WIDTH).setAlwaysOnTop();
    }

    private record Preview(BlockPos support, Direction facing, CustomPaintingData data, Entity ignored) {}

}