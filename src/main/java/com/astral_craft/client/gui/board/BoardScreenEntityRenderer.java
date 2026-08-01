package com.astral_craft.client.gui.board;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Shared model preview renderer used by board selection, encounter and combat panels. */
public class BoardScreenEntityRenderer {

    public static void render(GuiGraphicsExtractor graphics, Entity entity, int x0, int y0, int x1, int y1, float yaw) {
        render(graphics, entity, x0, y0, x1, y1, yaw, 1.0F, 0.0F, 0.0F, 0.0F);
    }

    public static void render(GuiGraphicsExtractor graphics, Entity entity, int x0, int y0, int x1, int y1,
                              float yaw, float scaleMultiplier, int offsetX, int offsetY, float rollDegrees) {
        render(graphics, entity, x0, y0, x1, y1, yaw, scaleMultiplier,
                (float) offsetX, (float) offsetY, rollDegrees);
    }

    public static void render(GuiGraphicsExtractor graphics, Entity entity, int x0, int y0, int x1, int y1,
                              float yaw, float scaleMultiplier, float offsetX, float offsetY, float rollDegrees) {
        if (entity == null) {
            int left = x0 + Math.round(offsetX);
            int top = y0 + Math.round(offsetY);
            graphics.fill(left, top, x1 + Math.round(offsetX), y1 + Math.round(offsetY), 0x55000000);
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super Entity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 0.0F;
            living.yRot = 0.0F;
            living.xRot = 0.0F;
            living.scale = 1.0F;
        }
        float boxWidth = Math.max(0.35F, state.boundingBoxWidth);
        float boxHeight = Math.max(1.0F, state.boundingBoxHeight);
        float scale = Math.min((x1 - x0) / (boxWidth * 1.45F), (y1 - y0) / (boxHeight * 1.12F));
        scale = Mth.clamp(scale * Math.max(0.01F, scaleMultiplier), 10.0F, 112.0F);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI + (float) Math.toRadians(rollDegrees))
                .rotateY((float) Math.toRadians(yaw));
        Vector3f translation = new Vector3f(0.0F, boxHeight * 0.48F, 0.0F);
        int wholeOffsetX = (int) Math.floor(offsetX);
        int wholeOffsetY = (int) Math.floor(offsetY);
        float fractionalOffsetX = offsetX - wholeOffsetX;
        float fractionalOffsetY = offsetY - wholeOffsetY;
        graphics.pose().pushMatrix();
        graphics.pose().translate(fractionalOffsetX, fractionalOffsetY);
        graphics.entity(state, scale, translation, rotation, null,
                x0 + wholeOffsetX, y0 + wholeOffsetY, x1 + wholeOffsetX, y1 + wholeOffsetY);
        graphics.pose().popMatrix();
    }
}
