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

    private static final Identifier LODESTONE = Identifier.withDefaultNamespace("textures/block/lodestone_side.png");
    private static final Identifier TNT = Identifier.withDefaultNamespace("textures/block/tnt_side.png");
    private static final Identifier BARRICADE = Identifier.withDefaultNamespace("textures/block/yellow_concrete.png");
    private static final Identifier ENHANCED_BARRICADE = Identifier.withDefaultNamespace("textures/block/orange_concrete.png");
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
        } else {
            Identifier texture = texture(state.kind);
            float size = size(state);
            poseStack.pushPose();
            if (state.kind == BoardWorldObjectEntity.Kind.TIME_BOMB) {
                poseStack.translate(0.0D, Math.sin(state.age * 0.13F) * 0.08D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 4.5F));
            } else if (coin(state.kind)) {
                poseStack.translate(0.0D, Math.sin(state.age * 0.16F) * 0.045D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 7.0F));
                poseStack.scale(1.0F, 0.18F, 1.0F);
            } else {
                float landing = Mth.clamp(state.age / 10.0F, 0.0F, 1.0F);
                float height = (1.0F - landing) * (1.0F - landing) * 0.72F
                        + Mth.sin(landing * (float) Math.PI) * 0.045F;
                poseStack.translate(0.0D, height, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.stackIndex * 31.0F));
            }

            submitCube(poseStack, collector, texture, size);
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, cameraState);
    }

    private void submitCoinPile(BoardWorldObjectRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        int layers = Math.min(6, Math.max(1, state.amount));
        float size = size(state);
        for (int layer = 0; layer < layers; layer++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, layer * 0.025D + Math.sin((state.age + layer) * 0.12F) * 0.006D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 2.0F + layer * 23.0F));
            poseStack.scale(1.0F, 0.18F, 1.0F);
            submitCube(poseStack, collector, GOLD_INGOT, size);
            poseStack.popPose();
        }
    }

    private static void submitCube(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, float size) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture),
                (pose, consumer) -> EffectRenderGeometry.cube(pose, consumer, size, 0xFFFFFFFF));
    }

    private static Identifier texture(BoardWorldObjectEntity.Kind kind) {
        return switch (kind) {
            case ENTRAPMENT -> LODESTONE;
            case DEMOLITION, TIME_BOMB -> TNT;
            case BARRICADE -> BARRICADE;
            case ENHANCED_BARRICADE -> ENHANCED_BARRICADE;
            case COIN_PILE, COIN_PICKUP, COIN_AWARD -> GOLD_INGOT;
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

}