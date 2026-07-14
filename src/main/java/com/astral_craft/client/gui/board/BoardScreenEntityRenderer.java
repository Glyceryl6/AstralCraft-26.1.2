package com.astral_craft.client.gui.board;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Shared model preview renderer used by board selection, encounter and combat panels. */
public class BoardScreenEntityRenderer {

    public static void render(GuiGraphicsExtractor graphics, LivingEntity entity,
                              int x0, int y0, int x1, int y1, float yaw) {
        if (entity == null) {
            graphics.fill(x0, y0, x1, y1, 0x55000000);
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            // The preview quaternion owns the horizontal rotation. Applying yaw to both the
            // render state and quaternion rotates models twice and makes combatants face away.
            living.bodyRot = 0.0F;
            living.yRot = 0.0F;
            living.xRot = 0.0F;
            living.scale = 1.0F;
        }
        float boxWidth = Math.max(0.35F, state.boundingBoxWidth);
        float boxHeight = Math.max(1.0F, state.boundingBoxHeight);
        float scale = Math.min((x1 - x0) / (boxWidth * 1.45F), (y1 - y0) / (boxHeight * 1.12F));
        scale = Mth.clamp(scale, 10.0F, 86.0F);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(yaw));
        Vector3f translation = new Vector3f(0.0F, boxHeight * 0.48F, 0.0F);
        graphics.entity(state, scale, translation, rotation, null, x0, y0, x1, y1);
    }

}
