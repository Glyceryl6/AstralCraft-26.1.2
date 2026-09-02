package com.astral_craft.common.network;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.blocks.platform.GamblePlatform;
import com.astral_craft.common.blocks.platform.LotteryPlatform;
import com.astral_craft.common.blocks.platform.RelicPlatform;
import com.astral_craft.common.blocks.platform.ShopPlatform;
import com.astral_craft.common.blocks.platform.StartPlatform;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.astral_craft.common.network.c2s.*;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.board.BoardLobbyService;
import com.astral_craft.common.gameplay.board.BoardDeveloperService;
import com.astral_craft.common.gameplay.board.BoardEventService;
import com.astral_craft.common.blocks.platform.DivinePlatform;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardPanelSelectionService;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.chip.ChipSelectionService;
import com.astral_craft.common.gameplay.handcard.AstralHandCardManager;
import com.astral_craft.common.gameplay.handcard.CardNumberSelectionHandler;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.gameplay.handcard.PendingCounterEffectManager;
import com.astral_craft.common.items.BoardDismantlerItem;
import com.astral_craft.common.items.BoardProjectorItem;
import com.astral_craft.common.items.BoardScannerItem;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class AstralServerPayloadHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(AstralServerPayloadHandlers.class);

    public static void handleCustomPaintingConfig(CustomPaintingConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level().getEntity(payload.entityId()) instanceof CustomPaintingEntity painting)) return;
            if (player.distanceToSqr(painting) > 64.0D || !CustomPaintingData.validResource(payload.data().resource())) return;
            if (!painting.applyConfiguration(payload.data())) {
                player.sendSystemMessage(Component.translatable("message.astral_craft.custom_painting.no_space"), true);
            }
        });
    }

    public static void handleCardTargets(CardTargetSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CardUseService.applyTargetSelection(player, payload);
            }
        });
    }

    public static void handleCardNumberSelection(CardNumberSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && payload.cardStack().getItem() instanceof CardNumberSelectionHandler handler) {
                handler.applyNumberSelection(player, payload);
            }
        });
    }

    public static void handleChipSelection(ChipSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ChipSelectionService.choose(player, payload.boardId(), payload.chipId());
            }
        });
    }

    public static void handleRequestCardBackSelection(RequestCardBackSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CardBackPreferenceManager.openSelection(player);
            }
        });
    }

    public static void handleCardBackSelection(CardBackSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CardBackPreferenceManager.select(player, payload.selectedCardBackId(), payload.selectedDiceSkinId());
            }
        });
    }

    public static void handleRequestCharacterSettings(RequestCharacterSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CharacterProgressManager.open(player);
            }
        });
    }

    public static void handleCharacterSelection(CharacterSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CharacterProgressManager.selectCharacter(player, payload.characterId());
            }
        });
    }

    public static void handleActivateCharacterPotential(ActivateCharacterPotentialPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CharacterProgressManager.activatePotential(player, payload.characterId());
            }
        });
    }

    public static void handleUnlockAllCharacters(UnlockAllCharactersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CharacterProgressManager.unlockAllForTesting(player);
            }
        });
    }

    public static void handleRequestHandCardDeck(RequestHandCardDeckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AstralHandCardManager.open(player);
            }
        });
    }

    public static void handleCharacterSkinSelection(CharacterSkinSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CharacterProgressManager.selectSkin(player, payload.characterId(), payload.skinId().getPath());
            }
        });
    }

    public static void handleRequestCharacterSkill(RequestCharacterSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AstralCharacterSkillService.useActiveSkill(player);
            }
        });
    }

    public static void handleUseHandCardFromDeck(UseHandCardFromDeckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !CardUseService.useDeckCard(player, payload.cardId())) {
                AstralHandCardManager.open(player);
            }
        });
    }

    public static void handleBoardCharacterSelection(BoardCharacterSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BoardLobbyService.updateSelection(player, payload.boardId(), payload.characterId(), payload.skinId(), payload.confirmed());
            }
        });
    }

    public static void handleBoardDeveloperConfig(BoardDeveloperConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) BoardDeveloperService.apply(player, payload);
        });
    }

    public static void handleBoardProjectorConfirm(BoardProjectorConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BoardProjectorItem.confirmPlacement(player, payload);
            }
        });
    }

    public static void handleBoardModeSelection(BoardModeSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BoardScannerItem.chooseMode(player, payload.origin(), payload.mode());
            }
        });
    }

    public static void handleBoardDivinationChoice(BoardDivinationChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DivinePlatform.choose(player, payload.boardId(), payload.selectedIndex());
            }
        });
    }

    public static void handleBoardDismantleConfirm(BoardDismantleConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BoardDismantlerItem.confirmDelete(player, payload.boardId(), payload.action());
            }
        });
    }

    public static void handleUseBoardCard(UseBoardCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && !CardUseService.useBoardCard(player, payload.boardId(), payload.cardIndex())) {
                BoardSessionManager.reopenTurnScreen(player, payload.boardId());
            }
        });
    }

    public static void handleBoardCounterResponse(BoardCounterResponsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PendingCounterEffectManager.respondBoard(player, payload.boardId(), payload.handIndex());
            }
        });
    }

    public static void handleBoardMove(BoardMoveRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) BoardSessionManager.requestMove(player, payload.boardId());
        });
    }

    public static void handleBoardSkill(BoardSkillRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) BoardSessionManager.requestSkill(player, payload.boardId());
        });
    }

    public static void handleBoardDiscard(BoardDiscardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) BoardSessionManager.discard(player, payload.boardId(), payload.cardIndexes());
        });
    }

    public static void handleBoardEncounter(BoardEncounterChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) BoardSessionManager.chooseEncounter(player, payload.boardId(), payload.challenge());
        });
    }

    public static void handleBoardBattle(BoardBattleActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                try {
                    BoardBattleService.submit(player, payload.boardId(), payload.selectedCardIndexes(), payload.defenseMode());
                } catch (RuntimeException exception) {
                    LOGGER.error("Failed to process board battle action from {}", player.getGameProfile().name(), exception);
                }
            }
        });
    }

    public static void handleBoardStartChoice(BoardStartChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                StartPlatform.choose(player, payload.boardId(), payload.stop());
            }
        });
    }

    public static void handleBoardLotteryNumber(BoardLotteryNumberPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && !BoardEventService.chooseLotteryNumber(player, payload.boardId(), payload.number())) {
                LotteryPlatform.choose(player, payload.boardId(), payload.number());
            }
        });
    }

    public static void handleBoardGambleChoice(BoardGambleChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GamblePlatform.choose(player, payload.boardId(), payload.odd());
            }
        });
    }

    public static void handleBoardPlatformTarget(BoardPlatformTargetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BoardSessionManager.session(player.level(), payload.boardId()).ifPresent(session ->
                    BasePlatform.activeBoardEffect(payload.boardId()).ifPresent(platform ->
                            platform.handleBoardTargetSelection(player, session, payload.entityId())));
        });
    }

    public static void handleBoardShop(BoardShopActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShopPlatform.handleAction(player, payload.boardId(), payload.offerIndexes(), payload.leave());
            }
        });
    }

    public static void handleBoardRelicShop(BoardRelicShopActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                RelicPlatform.handleAction(player, payload.boardId(), payload.buy());
            }
        });
    }

    public static void handleBoardPanelSelection(BoardPanelSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BoardPanelSelectionService.submit(player, payload);
            }
        });
    }

    public static void handleBoardLeave(BoardLeavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BoardSessionManager.leaveGame(player, payload.boardId());
            }
        });
    }

}
