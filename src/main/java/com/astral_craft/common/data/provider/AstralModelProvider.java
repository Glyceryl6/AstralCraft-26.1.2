package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.data.model.AstralModelTemplates;
import com.astral_craft.common.data.model.AstralTextureSlot;
import com.astral_craft.common.data.model.AstralTexturedModel;
import com.astral_craft.common.registry.AstralBlocks;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class AstralModelProvider extends ModelProvider {

    public AstralModelProvider(PackOutput output) {
        super(output, AstralCraft.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(AstralItems.BOARD_SCANNER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AstralItems.BOARD_LOBBY.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AstralItems.BOARD_SPECTATOR.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AstralItems.BOARD_DISMANTLER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(AstralItems.BOARD_PROJECTOR.get(), ModelTemplates.FLAT_ITEM);
        AstralItems.MODELLED_CARD_ITEMS.forEach(modelledCardItem -> {
            Item item = modelledCardItem.item().get();
            String name = modelledCardItem.cardType().name;
            Material frame = new Material(AstralCraft.prefix("item/template_handcard_").withSuffix(name));
            TextureMapping mapping = (new TextureMapping()).put(AstralTextureSlot.FRAME, frame).put(TextureSlot.PARTICLE, frame)
                    .put(AstralTextureSlot.ICON, new Material(ModelLocationUtils.getModelLocation(item)));
            Identifier identifier = AstralModelTemplates.HANDCARD.create(item, mapping, itemModels.modelOutput);
            itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(identifier));
        });

        AstralBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).filter(block -> block instanceof BasePlatform).forEach(block -> {
            MultiVariant model = plainVariant(AstralTexturedModel.PLATFORM.get(block).create(block, blockModels.modelOutput));
            blockModels.registerSimpleItemModel(block, blockModels.createFlatItemModelWithBlockTexture(block.asItem(), block));
            blockModels.blockStateOutput.accept(createSimpleBlock(block, model).with(ROTATION_HORIZONTAL_FACING_ALT));
        });
    }

}