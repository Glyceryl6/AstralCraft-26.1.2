package com.astral_craft.common.network;

import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.chip.ChipSelectionService;
import com.astral_craft.common.gameplay.handcard.AstralHandCardManager;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.items.cards.HandcardSmartDice;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("unused")
public class AstralServerPayloadHandlers {

    public static void handleCardTargets(CardTargetSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CardUseService.applyTargetSelection(player, payload);
            }
        });
    }

    public static void handleCardNumberSelection(CardNumberSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HandcardSmartDice.applyNumberSelection(player, payload);
            }
        });
    }

    public static void handleChipSelection(ChipSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ChipSelectionService.choose(player, payload.chipId());
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
                CardBackPreferenceManager.select(player, CardBackPreferenceManager.safeParse(payload.selectedId()));
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
                CharacterProgressManager.selectSkin(player, payload.characterId(), payload.skinId());
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

}