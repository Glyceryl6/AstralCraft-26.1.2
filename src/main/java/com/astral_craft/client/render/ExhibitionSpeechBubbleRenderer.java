package com.astral_craft.client.render;

import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExhibitionSpeechBubbleRenderer {

    private static final int MAX_LINE_WIDTH = 180;
    private static final double MAX_DISTANCE = 64.0D;
    private static final float LINE_SPACING = 9.0F * 1.15F * 0.025F;

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        List<ExhibitionCharacterEntity> entities = minecraft.level.getEntitiesOfClass(ExhibitionCharacterEntity.class,
                minecraft.player.getBoundingBox().inflate(MAX_DISTANCE), entity -> !entity.speechText().isBlank());
        for (ExhibitionCharacterEntity entity : entities) {
            Vec3 anchor = entity.position().add(0.0D, entity.getBbHeight() + Math.max(0.55D, entity.displayScale() * 0.30D), 0.0D);
            double distanceToCameraSq = anchor.distanceToSqr(cameraPos);
            if (distanceToCameraSq > MAX_DISTANCE * MAX_DISTANCE) continue;
            List<Component> lines = split(minecraft, entity.speechText());
            if (lines.isEmpty()) continue;
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
            for (int index = lines.size() - 1; index >= 0; index--) {
                event.getSubmitNodeCollector().order(0).submitNameTag(poseStack, Vec3.ZERO, 0, lines.get(index), Boolean.TRUE,
                        LightCoordsUtil.FULL_BRIGHT, distanceToCameraSq, event.getLevelRenderState().cameraRenderState);
                poseStack.translate(0.0F, LINE_SPACING, 0.0F);
            }
            poseStack.popPose();
        }
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
