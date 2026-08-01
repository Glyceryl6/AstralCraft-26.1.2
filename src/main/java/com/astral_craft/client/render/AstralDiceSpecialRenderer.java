package com.astral_craft.client.render;

import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import com.astral_craft.common.registry.AstralDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** Item renderer that reads the synchronized six-face dice texture from the stack. */
public class AstralDiceSpecialRenderer implements SpecialModelRenderer<Identifier> {

    @Override
    public @Nullable Identifier extractArgument(ItemStack stack) {
        return stack.getOrDefault(AstralDataComponents.DICE_TEXTURE, DiceSkinPreferenceManager.DEFAULT_TEXTURE);
    }

    @Override
    public void submit(@Nullable Identifier texture, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        AstralDiceRenderer.renderItemCube(poseStack, collector,
                texture == null ? DiceSkinPreferenceManager.DEFAULT_TEXTURE : texture, lightCoords, overlayCoords);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float min = 0.5F - AstralDiceRenderer.HALF_SIZE;
        float max = 0.5F + AstralDiceRenderer.HALF_SIZE;
        for (float x : new float[]{min, max}) {
            for (float y : new float[]{min, max}) {
                for (float z : new float[]{min, max}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Identifier> {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public AstralDiceSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new AstralDiceSpecialRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

    }

}