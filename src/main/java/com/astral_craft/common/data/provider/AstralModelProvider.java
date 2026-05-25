package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.data.model.AstralTexturedModel;
import com.astral_craft.common.registry.AstralBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.DeferredHolder;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class AstralModelProvider extends ModelProvider {

    public AstralModelProvider(PackOutput output) {
        super(output, AstralCraft.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        AstralBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).filter(block -> block instanceof BasePlatform).forEach(block -> {
            MultiVariant model = plainVariant(AstralTexturedModel.PLATFORM.get(block).create(block, blockModels.modelOutput));
            blockModels.registerSimpleItemModel(block, blockModels.createFlatItemModelWithBlockTexture(block.asItem(), block));
            blockModels.blockStateOutput.accept(createSimpleBlock(block, model).with(ROTATION_HORIZONTAL_FACING_ALT));
        });
    }

}