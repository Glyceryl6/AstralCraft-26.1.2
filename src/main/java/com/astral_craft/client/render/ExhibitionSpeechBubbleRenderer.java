package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExhibitionSpeechBubbleRenderer {

    private static final Identifier BUBBLE_TEXTURE = AstralCraft.prefix("textures/entity/dice/white.png");
    private static final int MAX_LINE_WIDTH = 180;
    private static final int BORDER_COLOR = 0xFF3A3040;
    private static final int BACKGROUND_COLOR = 0xFFFFF9FF;
    private static final int TEXT_COLOR = 0xFF28202C;
    private static final double MAX_DISTANCE = 64.0D;
    private static final float WORLD_SCALE = 0.025F;
    private static final float LINE_HEIGHT = 10.0F;
    private static final float PADDING_X = 7.0F;
    private static final float PADDING_Y = 5.0F;
    private static final float BORDER = 2.0F;
    private static final float TAIL_HALF_WIDTH = 6.0F;
    private static final float TAIL_HEIGHT = 7.0F;

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        List<ExhibitionCharacterEntity> entities = minecraft.level.getEntitiesOfClass(ExhibitionCharacterEntity.class,
                minecraft.player.getBoundingBox().inflate(MAX_DISTANCE), entity -> !entity.speechText().isBlank());
        for (ExhibitionCharacterEntity entity : entities) {
            Vec3 anchor = entity.position().add(0.0D, entity.getBbHeight() + Math.max(0.62D, entity.displayScale() * 0.34D), 0.0D);
            double distanceToCameraSq = anchor.distanceToSqr(cameraPos);
            if (distanceToCameraSq > MAX_DISTANCE * MAX_DISTANCE) continue;
            List<Component> lines = split(minecraft, entity.speechText());
            if (lines.isEmpty()) continue;
            submitBubble(event, minecraft, anchor, cameraPos, lines);
        }
    }

    private static void submitBubble(SubmitCustomGeometryEvent event, Minecraft minecraft, Vec3 anchor, Vec3 cameraPos, List<Component> lines) {
        Font font = minecraft.font;
        float textWidth = 0.0F;
        for (Component line : lines) textWidth = Math.max(textWidth, font.width(line.getVisualOrderText()));
        float bubbleWidth = Math.max(24.0F, textWidth + PADDING_X * 2.0F);
        float bubbleHeight = lines.size() * LINE_HEIGHT + PADDING_Y * 2.0F;
        float left = -bubbleWidth / 2.0F;
        float right = bubbleWidth / 2.0F;
        float top = -bubbleHeight / 2.0F;
        float bottom = bubbleHeight / 2.0F;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        poseStack.pushPose();
        poseStack.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.scale(WORLD_SCALE, -WORLD_SCALE, WORLD_SCALE);
        submitBubbleGeometry(collector, poseStack, left, top, right, bottom);
        float textY = top + PADDING_Y + 1.0F;
        for (Component line : lines) {
            float width = font.width(line.getVisualOrderText());
            collector.order(1).submitText(poseStack, -width / 2.0F, textY, line.getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET,
                    LightCoordsUtil.FULL_BRIGHT, TEXT_COLOR, 0x00000000, 0);
            textY += LINE_HEIGHT;
        }
        poseStack.popPose();
    }

    private static void submitBubbleGeometry(SubmitNodeCollector collector, PoseStack poseStack, float left, float top, float right, float bottom) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(BUBBLE_TEXTURE), (pose, consumer) -> {
            quad(consumer, pose, left, top, right, bottom, BACKGROUND_COLOR);
            quad(consumer, pose, left - BORDER, top - BORDER, right + BORDER, top, BORDER_COLOR);
            quad(consumer, pose, left - BORDER, bottom, right + BORDER, bottom + BORDER, BORDER_COLOR);
            quad(consumer, pose, left - BORDER, top, left, bottom, BORDER_COLOR);
            quad(consumer, pose, right, top, right + BORDER, bottom, BORDER_COLOR);
            tail(consumer, pose, bottom);
        });
    }

    private static void tail(VertexConsumer consumer, PoseStack.Pose pose, float top) {
        float outerHalfWidth = TAIL_HALF_WIDTH + BORDER;
        float outerTipY = top + TAIL_HEIGHT + BORDER;
        float innerTop = top + BORDER;
        float innerTipY = top + TAIL_HEIGHT;
        quad(consumer, pose, -outerHalfWidth, top, outerHalfWidth, innerTop, BORDER_COLOR);
        freeQuad(consumer, pose, -outerHalfWidth, innerTop, -TAIL_HALF_WIDTH, innerTop, 0.0F, innerTipY, 0.0F, outerTipY, BORDER_COLOR);
        freeQuad(consumer, pose, TAIL_HALF_WIDTH, innerTop, outerHalfWidth, innerTop, 0.0F, outerTipY, 0.0F, innerTipY, BORDER_COLOR);
        triangle(consumer, pose, -TAIL_HALF_WIDTH, innerTop, TAIL_HALF_WIDTH, innerTop, 0.0F, innerTipY, BACKGROUND_COLOR);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float left, float top, float right, float bottom, int color) {
        freeQuad(consumer, pose, left, bottom, right, bottom, right, top, left, top, color);
    }

    private static void freeQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                                 float x2, float y2, float x3, float y3, int color) {
        vertex(consumer, pose, x0, y0, color, 0.0F, 1.0F);
        vertex(consumer, pose, x1, y1, color, 1.0F, 1.0F);
        vertex(consumer, pose, x2, y2, color, 1.0F, 0.0F);
        vertex(consumer, pose, x3, y3, color, 0.0F, 0.0F);
    }

    private static void triangle(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float x2, float y2, int color) {
        vertex(consumer, pose, x0, y0, color, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y1, color, 1.0F, 0.0F);
        vertex(consumer, pose, x2, y2, color, 0.5F, 1.0F);
        vertex(consumer, pose, x2, y2, color, 0.5F, 1.0F);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, int color, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static List<Component> split(Minecraft minecraft, String text) {
        List<Component> lines = new ArrayList<>();
        for (FormattedText line : minecraft.font.getSplitter().splitLines(Component.literal(text), MAX_LINE_WIDTH, Style.EMPTY)) {
            lines.add(toComponent(line));
        }
        return lines;
    }

    private static Component toComponent(FormattedText text) {
        MutableComponent component = Component.empty();
        text.visit((style, contents) -> {
            component.append(Component.literal(contents).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return component;
    }
}
