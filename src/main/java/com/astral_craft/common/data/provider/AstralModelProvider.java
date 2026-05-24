package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.data.model.AstralTexturedModel;
import com.astral_craft.common.registry.AstralBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AstralModelProvider extends ModelProvider {

    public AstralModelProvider(PackOutput output) {
        super(output, AstralCraft.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        AstralBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).filter(block -> block instanceof BasePlatform).forEach(block -> {
            Variant blockModel = BlockModelGenerators.plainModel(AstralTexturedModel.PLATFORM.get(block).create(block, blockModels.modelOutput));
            blockModels.registerSimpleItemModel(block, blockModels.createFlatItemModelWithBlockTexture(block.asItem(), block));
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.createRotatedVariants(blockModel)));
        });
    }

}