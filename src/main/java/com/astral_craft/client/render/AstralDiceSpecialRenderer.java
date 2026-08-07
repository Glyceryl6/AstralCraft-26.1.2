package com.astral_craft.client.render;

import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import com.astral_craft.common.registry.AstralDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** Item renderer that keeps an opaque white cube below the player's synchronized dice skin. */
public class AstralDiceSpecialRenderer implements SpecialModelRenderer<AstralDiceSpecialRenderer.RenderData> {

    @Override
    public @Nullable RenderData extractArgument(ItemStack stack) {
        Identifier texture = stack.get(AstralDataComponents.DICE_TEXTURE);
        Player player = Minecraft.getInstance().player;
        if (texture == null && player != null) texture = DiceSkinPreferenceManager.selectedTexture(player);
        return new RenderData(texture == null ? DiceSkinPreferenceManager.DEFAULT_TEXTURE : texture, "10");
    }

    @Override
    public void submit(@Nullable RenderData data, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        RenderData resolved = data == null ? new RenderData(DiceSkinPreferenceManager.DEFAULT_TEXTURE, "10") : data;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        AstralDiceRenderer.renderItem(poseStack, collector, Minecraft.getInstance().font,
                resolved.texture(), resolved.text(), lightCoords, overlayCoords);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float min = 0.5F - AstralDiceRenderer.HALF_SIZE;
        float max = 0.5F + AstralDiceRenderer.HALF_SIZE;
        for (float x : new float[]{min, max}) {
            for (float y : new float[]{min, max}) {
                for (float z : new float[]{min, max}) output.accept(new Vector3f(x, y, z));
            }
        }
    }

    public record RenderData(Identifier texture, String text) {}

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderData> {

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