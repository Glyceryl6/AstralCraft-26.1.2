package com.astral_craft.client.render.character;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class BoardKnockoutLayer extends RenderLayer<AstralCharacterRenderState, PlayerModel> {

    public BoardKnockoutLayer(RenderLayerParent<AstralCharacterRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       AstralCharacterRenderState state, float yRot, float xRot) {
        if (!state.knockedDown) return;
        renderColoredCutoutModel(this.getParentModel(), state.texture, poseStack, collector,
                lightCoords, state, 0xB8D8D8D8, state.outlineColor);
    }

}