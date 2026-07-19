package com.astral_craft.client.render;

import com.astral_craft.client.render.effect.EffectRenderGeometry;
import com.astral_craft.common.entity.BoardWorldObjectEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BoardWorldObjectRenderer extends EntityRenderer<BoardWorldObjectEntity, BoardWorldObjectRenderState> {

    private static final Identifier LODESTONE_SIDE = Identifier.withDefaultNamespace("textures/block/lodestone_side.png");
    private static final Identifier LODESTONE_TOP = Identifier.withDefaultNamespace("textures/block/lodestone_top.png");
    private static final Identifier TNT_SIDE = Identifier.withDefaultNamespace("textures/block/tnt_side.png");
    private static final Identifier TNT_TOP = Identifier.withDefaultNamespace("textures/block/tnt_top.png");
    private static final Identifier TNT_BOTTOM = Identifier.withDefaultNamespace("textures/block/tnt_bottom.png");
    private static final Identifier YELLOW_CONCRETE = Identifier.withDefaultNamespace("textures/block/yellow_concrete.png");
    private static final Identifier ORANGE_CONCRETE = Identifier.withDefaultNamespace("textures/block/orange_concrete.png");
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
    }

    @Override
    public void submit(BoardWorldObjectRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.kind == BoardWorldObjectEntity.Kind.COIN_PILE) {
            this.submitCoinPile(state, poseStack, collector);
        } else if (coin(state.kind)) {
            poseStack.pushPose();
            poseStack.translate(0.0D, Math.sin(state.age * 0.16F) * 0.045D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 7.0F));
            submitItem(poseStack, collector, GOLD_INGOT, size(state));
            poseStack.popPose();
        } else {
            float size = size(state);
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
            submitBlock(poseStack, collector, textures(state.kind), size);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, cameraState);
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

    private static void submitBlock(PoseStack poseStack, SubmitNodeCollector collector, BlockTextures textures, float halfSize) {
        submitFace(poseStack, collector, textures.side(),
                -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize,
                halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, 0.0F, 0.0F, 1.0F);
        submitFace(poseStack, collector, textures.side(),
                halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize,
                -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, 0.0F, 0.0F, -1.0F);
        submitFace(poseStack, collector, textures.side(),
                halfSize, -halfSize, halfSize, halfSize, -halfSize, -halfSize,
                halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, 1.0F, 0.0F, 0.0F);
        submitFace(poseStack, collector, textures.side(),
                -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize,
                -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -1.0F, 0.0F, 0.0F);
        submitFace(poseStack, collector, textures.top(),
                -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize,
                halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, 0.0F, 1.0F, 0.0F);
        submitFace(poseStack, collector, textures.bottom(),
                -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize,
                halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, 0.0F, -1.0F, 0.0F);
    }

    private static void submitFace(PoseStack poseStack, SubmitNodeCollector collector,
                                   Identifier texture,
                                   float x0, float y0, float z0, float x1, float y1, float z1,
                                   float x2, float y2, float z2, float x3, float y3, float z3,
                                   float nx, float ny, float nz) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, consumer) -> EffectRenderGeometry.quad(consumer, pose,
                        x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
                        0xFFFFFFFF, nx, ny, nz));
    }

    private static void submitItem(PoseStack poseStack, SubmitNodeCollector collector,
                                   Identifier texture, float halfSize) {
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

    private static BlockTextures textures(BoardWorldObjectEntity.Kind kind) {
        return switch (kind) {
            case ENTRAPMENT -> new BlockTextures(LODESTONE_SIDE, LODESTONE_TOP, LODESTONE_TOP);
            case DEMOLITION, TIME_BOMB -> new BlockTextures(TNT_SIDE, TNT_TOP, TNT_BOTTOM);
            case BARRICADE -> BlockTextures.uniform(YELLOW_CONCRETE);
            case ENHANCED_BARRICADE -> BlockTextures.uniform(ORANGE_CONCRETE);
            case COIN_PILE, COIN_PICKUP, COIN_AWARD -> BlockTextures.uniform(GOLD_INGOT);
        };
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

    private static boolean coin(BoardWorldObjectEntity.Kind kind) {
        return kind == BoardWorldObjectEntity.Kind.COIN_PILE
                || kind == BoardWorldObjectEntity.Kind.COIN_PICKUP
                || kind == BoardWorldObjectEntity.Kind.COIN_AWARD;
    }

    private record BlockTextures(Identifier side, Identifier top, Identifier bottom) {
        private static BlockTextures uniform(Identifier texture) {
            return new BlockTextures(texture, texture, texture);
        }
    }

}