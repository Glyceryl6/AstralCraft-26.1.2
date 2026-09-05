package com.astral_craft.client.render;

import com.astral_craft.client.gui.board.BoardRouteWorldRenderer;
import com.astral_craft.common.blocks.BasePlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlatformTooltipWorldRenderer {

    private static final int MAX_LINE_WIDTH = 160;
    private static final float LINE_SPACING = 9.0F * 1.15F * 0.025F;
    private static final double HEIGHT_ABOVE_BLOCK = 0.5D;
    private static @Nullable BasePlatform cachedPlatform;
    private static @Nullable Language cachedLanguage;
    private static @Nullable Font cachedFont;
    private static List<Component> cachedLines = List.of();

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) return;
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        submitTutorialBranchLabels(event, cameraPos);
        if (minecraft.screen != null) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockPos blockPos = hitResult.getBlockPos();
        if (!(minecraft.level.getBlockState(blockPos).getBlock() instanceof BasePlatform platform)) return;
        Vec3 anchor = Vec3.atCenterOf(blockPos).add(0.0D, HEIGHT_ABOVE_BLOCK, 0.0D);
        double distanceToCameraSq = anchor.distanceToSqr(cameraPos);
        List<Component> lines = tooltipLines(minecraft.font, platform);
        if (lines.isEmpty()) return;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
        for (int index = lines.size() - 1; index >= 0; index--) {
            event.getSubmitNodeCollector().order(0).submitNameTag(
                    poseStack, Vec3.ZERO, 0, lines.get(index), Boolean.TRUE,
                    LightCoordsUtil.FULL_BRIGHT, distanceToCameraSq,
                    event.getLevelRenderState().cameraRenderState);
            poseStack.translate(0.0F, LINE_SPACING, 0.0F);
        }

        poseStack.popPose();
    }

    private static void submitTutorialBranchLabels(SubmitCustomGeometryEvent event, Vec3 cameraPos) {
        List<BlockPos> branches = BoardRouteWorldRenderer.tutorialBranchPositions();
        if (branches.isEmpty()) return;
        Component text = Component.translatable("gui.astral_craft.board.tutorial.branch_world");
        for (BlockPos blockPos : branches) {
            Vec3 anchor = Vec3.atCenterOf(blockPos).add(0.0D, 0.72D, 0.0D);
            double distanceToCameraSq = anchor.distanceToSqr(cameraPos);
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y, anchor.z - cameraPos.z);
            event.getSubmitNodeCollector().order(0).submitNameTag(
                    poseStack, Vec3.ZERO, 0, text, Boolean.TRUE,
                    LightCoordsUtil.FULL_BRIGHT, distanceToCameraSq,
                    event.getLevelRenderState().cameraRenderState);
            poseStack.popPose();
        }
    }

    private static List<Component> tooltipLines(Font font, BasePlatform platform) {
        Language language = Language.getInstance();
        if (cachedPlatform == platform && cachedLanguage == language && cachedFont == font) {
            return cachedLines;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(platform.getDescriptionId()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        for (FormattedText line : font.getSplitter().splitLines(platform.tooltip(), MAX_LINE_WIDTH, Style.EMPTY)) {
            lines.add(toComponent(line));
        }

        cachedPlatform = platform;
        cachedLanguage = language;
        cachedFont = font;
        cachedLines = List.copyOf(lines);
        return cachedLines;
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