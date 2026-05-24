package com.astral_craft.common.data.model;

import com.astral_craft.AstralCraft;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class AstralModelTemplates {

    public static final ModelTemplate PLATFORM = create("template_platform", TextureSlot.TEXTURE);

    public static ModelTemplate create(String id, TextureSlot... slots) {
        return new ModelTemplate(Optional.of(decorateBlockModelLocation(id)), Optional.empty(), slots);
    }

    public static Identifier decorateBlockModelLocation(String id) {
        return AstralCraft.prefix(id).withPrefix("block/");
    }

}