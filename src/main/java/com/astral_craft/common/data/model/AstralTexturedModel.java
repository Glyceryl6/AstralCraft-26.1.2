package com.astral_craft.common.data.model;

import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;

public class AstralTexturedModel {

    public static final TexturedModel.Provider PLATFORM = TexturedModel.createDefault(TextureMapping::defaultTexture, AstralModelTemplates.PLATFORM);

}