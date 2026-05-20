package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

public class AstralModelProvider extends ModelProvider {

    public AstralModelProvider(PackOutput output) {
        super(output, AstralCraft.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

    }

}