package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterSkinAddition;
import com.astral_craft.common.gameplay.character.SkinRarityDefinition;
import com.astral_craft.common.gameplay.event.AstralEventDefinition;
import com.astral_craft.common.network.*;
import com.astral_craft.common.registry.AstralDataPackRegistryKeys;
import com.astral_craft.common.registry.AstralEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = AstralCraft.MOD_ID)
public class ModBusEventSubscriber {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(AstralEntities.ASTRAL_CHARACTER.get(), AstralCharacterEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(AstralDataPackRegistryKeys.CHARACTERS, CharacterDefinition.CODEC, CharacterDefinition.CODEC);
        event.dataPackRegistry(AstralDataPackRegistryKeys.CHARACTER_SKINS, CharacterSkinAddition.CODEC, CharacterSkinAddition.CODEC);
        event.dataPackRegistry(AstralDataPackRegistryKeys.SKIN_RARITIES, SkinRarityDefinition.CODEC, SkinRarityDefinition.CODEC);
        event.dataPackRegistry(AstralDataPackRegistryKeys.CARD_BACKS, CardBackDefinition.CODEC, CardBackDefinition.CODEC);
        event.dataPackRegistry(AstralDataPackRegistryKeys.EVENTS, AstralEventDefinition.CODEC, AstralEventDefinition.CODEC);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(CardRevealPayload.TYPE, CardRevealPayload.STREAM_CODEC);
        registrar.playToClient(OpenTargetSelectionPayload.TYPE, OpenTargetSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenBattleScenePayload.TYPE, OpenBattleScenePayload.STREAM_CODEC);
        registrar.playToClient(OpenChipSelectionPayload.TYPE, OpenChipSelectionPayload.STREAM_CODEC);
        registrar.playToClient(BoardHudSnapshotPayload.TYPE, BoardHudSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(OpenCardBackSelectionPayload.TYPE, OpenCardBackSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenCharacterSettingsPayload.TYPE, OpenCharacterSettingsPayload.STREAM_CODEC);
        registrar.playToServer(CardTargetSelectionPayload.TYPE, CardTargetSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardTargets);
        registrar.playToServer(ChipSelectionPayload.TYPE, ChipSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleChipSelection);
        registrar.playToServer(RequestCardBackSelectionPayload.TYPE, RequestCardBackSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCardBackSelection);
        registrar.playToServer(CardBackSelectionPayload.TYPE, CardBackSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardBackSelection);
        registrar.playToServer(RequestCharacterSettingsPayload.TYPE, RequestCharacterSettingsPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCharacterSettings);
        registrar.playToServer(CharacterSelectionPayload.TYPE, CharacterSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCharacterSelection);
        registrar.playToServer(CharacterSkinSelectionPayload.TYPE, CharacterSkinSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCharacterSkinSelection);
        registrar.playToServer(UseHandCardFromDeckPayload.TYPE, UseHandCardFromDeckPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUseHandCardFromDeck);
    }

}