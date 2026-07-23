package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinAddition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinRarityDefinition;
import com.astral_craft.common.gameplay.event.AstralEventDefinition;
import com.astral_craft.common.network.AstralServerPayloadHandlers;
import com.astral_craft.common.network.c2s.*;
import com.astral_craft.common.network.s2c.*;
import com.astral_craft.common.registry.AstralAttributes;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.registry.bootstrap.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
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
    public static void modifyDefaultAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, AstralAttributes.HAND_CARD_RANGE, 0.0D);
    }

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(AstralCharacterBootstrap.CHARACTERS, CharacterDefinition.CODEC, CharacterDefinition.CODEC);
        event.dataPackRegistry(AstralCharacterSkinBootstrap.CHARACTER_SKINS, CharacterSkinAddition.CODEC, CharacterSkinAddition.CODEC);
        event.dataPackRegistry(AstralSkinRarityBootstrap.SKIN_RARITIES, CharacterSkinRarityDefinition.CODEC, CharacterSkinRarityDefinition.CODEC);
        event.dataPackRegistry(AstralCardBackBootstrap.CARD_BACKS, CardBackDefinition.CODEC, CardBackDefinition.CODEC);
        event.dataPackRegistry(AstralEventBootstrap.EVENTS, AstralEventDefinition.CODEC, AstralEventDefinition.CODEC);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("22");
        registrar.playToClient(CardRevealPayload.TYPE, CardRevealPayload.STREAM_CODEC);
        registrar.playToClient(CardRevealControlPayload.TYPE, CardRevealControlPayload.STREAM_CODEC);
        registrar.playToClient(CardRevealEntityPayload.TYPE, CardRevealEntityPayload.STREAM_CODEC);
        registrar.playToClient(OpenTargetSelectionPayload.TYPE, OpenTargetSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenCardNumberSelectionPayload.TYPE, OpenCardNumberSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenChipSelectionPayload.TYPE, OpenChipSelectionPayload.STREAM_CODEC);
        registrar.playToClient(BoardHudSnapshotPayload.TYPE, BoardHudSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(BoardAnnouncementPayload.TYPE, BoardAnnouncementPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardCharacterSelectionPayload.TYPE, OpenBoardCharacterSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardProjectorConfirmPayload.TYPE, OpenBoardProjectorConfirmPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardTurnPayload.TYPE, OpenBoardTurnPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardDiscardPayload.TYPE, OpenBoardDiscardPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardEncounterPayload.TYPE, OpenBoardEncounterPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardEncounterPayload.TYPE, CloseBoardEncounterPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardPresentationPayload.TYPE, CloseBoardPresentationPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardBattlePayload.TYPE, OpenBoardBattlePayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardStartChoicePayload.TYPE, OpenBoardStartChoicePayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardLotteryNumberPayload.TYPE, OpenBoardLotteryNumberPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardLotteryNumberPayload.TYPE, CloseBoardLotteryNumberPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardGamblePayload.TYPE, OpenBoardGamblePayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardGamblePayload.TYPE, CloseBoardGamblePayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardLotteryDrawPayload.TYPE, OpenBoardLotteryDrawPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardHospitalPayload.TYPE, OpenBoardHospitalPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardHospitalPayload.TYPE, CloseBoardHospitalPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardPlatformTargetPayload.TYPE, OpenBoardPlatformTargetPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardPlatformTargetPayload.TYPE, CloseBoardPlatformTargetPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardLotteryDrawPayload.TYPE, CloseBoardLotteryDrawPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardShopPayload.TYPE, OpenBoardShopPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardPanelSelectionPayload.TYPE, OpenBoardPanelSelectionPayload.STREAM_CODEC);
        registrar.playToClient(BoardRouteStatePayload.TYPE, BoardRouteStatePayload.STREAM_CODEC);
        registrar.playToClient(OpenCardBackSelectionPayload.TYPE, OpenCardBackSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenCharacterSettingsPayload.TYPE, OpenCharacterSettingsPayload.STREAM_CODEC);
        registrar.playToClient(OpenHandCardDeckPayload.TYPE, OpenHandCardDeckPayload.STREAM_CODEC);
        registrar.playToClient(CharacterSkillCutinPayload.TYPE, CharacterSkillCutinPayload.STREAM_CODEC);
        registrar.playToServer(CardTargetSelectionPayload.TYPE, CardTargetSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardTargets);
        registrar.playToServer(CardNumberSelectionPayload.TYPE, CardNumberSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardNumberSelection);
        registrar.playToServer(ChipSelectionPayload.TYPE, ChipSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleChipSelection);
        registrar.playToServer(RequestCardBackSelectionPayload.TYPE, RequestCardBackSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCardBackSelection);
        registrar.playToServer(CardBackSelectionPayload.TYPE, CardBackSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardBackSelection);
        registrar.playToServer(RequestCharacterSettingsPayload.TYPE, RequestCharacterSettingsPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCharacterSettings);
        registrar.playToServer(RequestHandCardDeckPayload.TYPE, RequestHandCardDeckPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestHandCardDeck);
        registrar.playToServer(RequestCharacterSkillPayload.TYPE, RequestCharacterSkillPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCharacterSkill);
        registrar.playToServer(CharacterSelectionPayload.TYPE, CharacterSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCharacterSelection);
        registrar.playToServer(UnlockAllCharactersPayload.TYPE, UnlockAllCharactersPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUnlockAllCharacters);
        registrar.playToServer(ActivateCharacterPotentialPayload.TYPE, ActivateCharacterPotentialPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleActivateCharacterPotential);
        registrar.playToServer(CharacterSkinSelectionPayload.TYPE, CharacterSkinSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCharacterSkinSelection);
        registrar.playToServer(UseHandCardFromDeckPayload.TYPE, UseHandCardFromDeckPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUseHandCardFromDeck);
        registrar.playToServer(BoardCharacterSelectionPayload.TYPE, BoardCharacterSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardCharacterSelection);
        registrar.playToServer(BoardProjectorConfirmPayload.TYPE, BoardProjectorConfirmPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardProjectorConfirm);
        registrar.playToServer(UseBoardCardPayload.TYPE, UseBoardCardPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUseBoardCard);
        registrar.playToServer(BoardCounterResponsePayload.TYPE, BoardCounterResponsePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardCounterResponse);
        registrar.playToServer(BoardMoveRequestPayload.TYPE, BoardMoveRequestPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardMove);
        registrar.playToServer(BoardSkillRequestPayload.TYPE, BoardSkillRequestPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardSkill);
        registrar.playToServer(BoardDiscardPayload.TYPE, BoardDiscardPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardDiscard);
        registrar.playToServer(BoardEncounterChoicePayload.TYPE, BoardEncounterChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardEncounter);
        registrar.playToServer(BoardBattleActionPayload.TYPE, BoardBattleActionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardBattle);
        registrar.playToServer(BoardStartChoicePayload.TYPE, BoardStartChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardStartChoice);
        registrar.playToServer(BoardLotteryNumberPayload.TYPE, BoardLotteryNumberPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardLotteryNumber);
        registrar.playToServer(BoardGambleChoicePayload.TYPE, BoardGambleChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardGambleChoice);
        registrar.playToServer(BoardPlatformTargetPayload.TYPE, BoardPlatformTargetPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardPlatformTarget);
        registrar.playToServer(BoardLeavePayload.TYPE, BoardLeavePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardLeave);
        registrar.playToServer(BoardShopActionPayload.TYPE, BoardShopActionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardShop);
        registrar.playToServer(BoardPanelSelectionPayload.TYPE, BoardPanelSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardPanelSelection);
    }

}