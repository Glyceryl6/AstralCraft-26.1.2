package com.astral_craft.common.network;

import com.astral_craft.common.gameplay.CardUseService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class AstralServerPayloadHandlers {
    private AstralServerPayloadHandlers() {}

    public static void handleCardTargets(CardTargetSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CardUseService.applyTargetSelection(player, payload);
            }
        });
    }
}
