package com.astral_craft.client.render;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.entity.BoardWorldObjectEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class BoardWorldObjectRenderer extends EntityRenderer<BoardWorldObjectEntity, BoardWorldObjectRenderState> {

    private static final Identifier GOLD_INGOT = Identifier.withDefaultNamespace("textures/item/gold_ingot.png");

    public BoardWorldObjectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.14F;
    }

    @Override
    public BoardWorldObjectRenderState createRenderState() {
        return new BoardWorldObjectRenderState();
    }

    @Override
    public void extractRenderState(BoardWorldObjectEntity entity, BoardWorldObjectRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.kind();
        state.age = entity.visualAge(partialTick);
        state.stackIndex = entity.stackIndex();
        state.stackCount = entity.stackCount();
        state.amount = entity.amount();
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = entity.blockPosition();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = entity.blockState();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(BoardWorldObjectRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.kind == BoardWorldObjectEntity.Kind.COIN_PILE) {
            this.submitCoinPile(state, poseStack, collector);
        } else if (state.kind.coin()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, Math.sin(state.age * 0.16F) * 0.045D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 7.0F));
            submitItem(poseStack, collector, GOLD_INGOT, size(state));
            poseStack.popPose();
        } else {
            this.submitBlockModel(state, poseStack, collector);
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void submitBlockModel(BoardWorldObjectRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        BlockState blockState = state.movingBlockRenderState.blockState;
        if (blockState.getRenderShape() != RenderShape.MODEL) return;
        float modelScale = size(state) * 2.0F;
        poseStack.pushPose();
        if (state.kind == BoardWorldObjectEntity.Kind.TIME_BOMB) {
            poseStack.translate(0.0D, Math.sin(state.age * 0.13F) * 0.08D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 4.5F));
        } else {
            float landing = Mth.clamp(state.age / 10.0F, 0.0F, 1.0F);
            float height = (1.0F - landing) * (1.0F - landing) * 0.72F
                    + Mth.sin(landing * (float) Math.PI) * 0.045F;
            poseStack.translate(0.0D, height, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.stackIndex * 31.0F));
        }
        poseStack.scale(modelScale, modelScale, modelScale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState);
        poseStack.popPose();
    }

    private void submitCoinPile(BoardWorldObjectRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        int layers = Math.clamp(state.amount, 1, 6);
        float size = size(state);
        for (int layer = 0; layer < layers; layer++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, layer * 0.035D
                    + Math.sin((state.age + layer) * 0.12F) * 0.006D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 2.0F + layer * 23.0F));
            submitItem(poseStack, collector, GOLD_INGOT, size);
            poseStack.popPose();
        }
    }

    private static void submitItem(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, float halfSize) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, consumer) -> {
                    EffectRenderGeometry.quad(consumer, pose,
                            -halfSize, -halfSize, 0.006F, halfSize, -halfSize, 0.006F,
                            halfSize, halfSize, 0.006F, -halfSize, halfSize, 0.006F,
                            0xFFFFFFFF, 0.0F, 0.0F, 1.0F);
                    EffectRenderGeometry.quad(consumer, pose,
                            halfSize, -halfSize, -0.006F, -halfSize, -halfSize, -0.006F,
                            -halfSize, halfSize, -0.006F, halfSize, halfSize, -0.006F,
                            0xFFFFFFFF, 0.0F, 0.0F, -1.0F);
                });
    }

    private static float size(BoardWorldObjectRenderState state) {
        return switch (state.kind) {
            case BARRICADE, ENHANCED_BARRICADE -> 0.28F;
            case TIME_BOMB -> 0.20F;
            case COIN_PILE -> Math.min(0.18F, 0.10F + Math.max(0, state.amount - 1) * 0.008F);
            case COIN_PICKUP, COIN_AWARD -> 0.11F;
            default -> state.stackCount > 4 ? 0.12F : 0.15F;
        };
    }

}