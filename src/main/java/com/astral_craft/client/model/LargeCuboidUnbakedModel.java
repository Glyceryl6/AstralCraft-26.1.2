package com.astral_craft.client.model;

import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import org.jspecify.annotations.Nullable;

public final class LargeCuboidUnbakedModel extends AbstractUnbakedModel {

    private final @Nullable LargeCuboidGeometry geometry;

    public LargeCuboidUnbakedModel(StandardModelParameters parameters, @Nullable LargeCuboidGeometry geometry) {
        super(parameters);
        this.geometry = geometry;
    }

    @Override
    public @Nullable UnbakedGeometry geometry() {
        return this.geometry;
    }

}