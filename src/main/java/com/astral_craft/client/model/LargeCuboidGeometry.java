package com.astral_craft.client.model;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;

import java.util.List;
import java.util.Map;

public final class LargeCuboidGeometry implements ExtendedUnbakedGeometry {

    private final List<CuboidModelElement> elements;

    public LargeCuboidGeometry(List<CuboidModelElement> elements) {
        this.elements = List.copyOf(elements);
    }

    @Override
    public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState state, ModelDebugName debugName, ContextMap additionalProperties) {
        if (this.elements.isEmpty()) {
            return QuadCollection.EMPTY;
        }

        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (CuboidModelElement element : this.elements) {
            for (Map.Entry<Direction, CuboidFace> entry : element.faces().entrySet()) {
                Direction side = entry.getKey();
                CuboidFace face = entry.getValue();
                Material.Baked material = baker.materials().resolveSlot(textureSlots, face.texture(), debugName);
                BakedQuad quad = FaceBakery.bakeQuad(
                        baker, element.from(), element.to(), face, material, side, state,
                        element.rotation(), element.shade(), element.lightEmission());
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                } else {
                    builder.addCulledFace(Direction.rotate(state.transformation().getMatrix(), face.cullForDirection()), quad);
                }
            }
        }

        return builder.build();
    }

}