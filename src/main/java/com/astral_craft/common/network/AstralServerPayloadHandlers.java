package com.astral_craft.common.network;

import com.astral_craft.common.gameplay.CardUseService;
import com.astral_craft.common.gameplay.ChipSelectionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class AstralServerPayloadHandlers {

    public static void handleCardTargets(CardTargetSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CardUseService.applyTargetSelection(player, payload);
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

}