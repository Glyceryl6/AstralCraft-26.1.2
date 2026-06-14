package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.CardDefinition;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPE = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AstralCraft.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardType>> CARD_TYPE = DATA_COMPONENT_TYPE.register("card_type",
            () -> DataComponentType.<CardType>builder().persistent(CardType.CODEC).networkSynchronized(CardType.STREAM_CODEC).cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardDefinition>> CARD_DEFINITION = DATA_COMPONENT_TYPE.register("card_definition",
            () -> DataComponentType.<CardDefinition>builder().persistent(CardDefinition.CODEC).networkSynchronized(CardDefinition.STREAM_CODEC).cacheEncoding().build());

}