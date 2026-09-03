package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
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
    private static final int BORDER_COLOR = 0xF03A3040;
    private static final int BACKGROUND_COLOR = 0xF8FFF9FF;
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
        submitPanel(collector, poseStack, left - BORDER, top - BORDER, right + BORDER, bottom + BORDER, BORDER_COLOR, 0.0F);
        submitTail(collector, poseStack, bottom + BORDER, TAIL_HALF_WIDTH + BORDER, TAIL_HEIGHT + BORDER, BORDER_COLOR, 0.0F);
        submitPanel(collector.order(1), poseStack, left, top, right, bottom, BACKGROUND_COLOR, -0.02F);
        submitTail(collector.order(1), poseStack, bottom, TAIL_HALF_WIDTH, TAIL_HEIGHT, BACKGROUND_COLOR, -0.02F);
        float textY = top + PADDING_Y + 1.0F;
        for (Component line : lines) {
            float width = font.width(line.getVisualOrderText());
            collector.order(2).submitText(poseStack, -width / 2.0F, textY, line.getVisualOrderText(), false,
                    Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, TEXT_COLOR, 0x00000000, 0);
            textY += LINE_HEIGHT;
        }
        poseStack.popPose();
    }

    private static void submitPanel(OrderedSubmitNodeCollector collector, PoseStack poseStack, float left, float top, float right, float bottom, int color, float z) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(BUBBLE_TEXTURE), (pose, consumer) -> {
            vertex(consumer, pose, left, bottom, z, color, 0.0F, 1.0F);
            vertex(consumer, pose, right, bottom, z, color, 1.0F, 1.0F);
            vertex(consumer, pose, right, top, z, color, 1.0F, 0.0F);
            vertex(consumer, pose, left, top, z, color, 0.0F, 0.0F);
        });
    }

    private static void submitTail(OrderedSubmitNodeCollector collector, PoseStack poseStack, float top, float halfWidth, float height, int color, float z) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(BUBBLE_TEXTURE), (pose, consumer) -> {
            vertex(consumer, pose, -halfWidth, top, z, color, 0.0F, 0.0F);
            vertex(consumer, pose, halfWidth, top, z, color, 1.0F, 0.0F);
            vertex(consumer, pose, 0.0F, top + height, z, color, 0.5F, 1.0F);
            vertex(consumer, pose, 0.0F, top + height, z, color, 0.5F, 1.0F);
        });
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int color, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
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