package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CombatBonusDefinition;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.gameplay.board.BoardTemplateData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class AstralDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPE = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AstralCraft.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardType>> CARD_TYPE = DATA_COMPONENT_TYPE.register("card_type",
            () -> DataComponentType.<CardType>builder().persistent(CardType.CODEC).networkSynchronized(CardType.STREAM_CODEC).cacheEncoding().build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardDefinition>> CARD_DEFINITION = DATA_COMPONENT_TYPE.register("card_definition",
            () -> DataComponentType.<CardDefinition>builder().persistent(CardDefinition.CODEC).networkSynchronized(CardDefinition.STREAM_CODEC).cacheEncoding().build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CombatBonusDefinition>> COMBAT_BONUS = DATA_COMPONENT_TYPE.register("combat_bonus",
            () -> DataComponentType.<CombatBonusDefinition>builder().persistent(CombatBonusDefinition.CODEC).networkSynchronized(CombatBonusDefinition.STREAM_CODEC).cacheEncoding().build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BOARD_SPECTATOR_BINDING = DATA_COMPONENT_TYPE.register("board_spectator_binding",
            () -> DataComponentType.<UUID>builder().networkSynchronized(BoardNetworkCodecs.UUID_STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomPaintingData>> CUSTOM_PAINTING = DATA_COMPONENT_TYPE.register("custom_painting",
            () -> DataComponentType.<CustomPaintingData>builder().persistent(CustomPaintingData.CODEC).networkSynchronized(CustomPaintingData.STREAM_CODEC).cacheEncoding().build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoardTemplateData>> BOARD_TEMPLATE = DATA_COMPONENT_TYPE.register("board_template",
            () -> DataComponentType.<BoardTemplateData>builder().persistent(BoardTemplateData.CODEC).networkSynchronized(BoardTemplateData.STREAM_CODEC).cacheEncoding().build());

}