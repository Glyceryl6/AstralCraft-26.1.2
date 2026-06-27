package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, AstralCraft.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> HAND_CARD_RANGE = ATTRIBUTES.register("hand_card_range",
            () -> new RangedAttribute("attribute.name.astral_craft.hand_card_range", 0.0D, -1024.0D, 1024.0D).setSyncable(true));

}