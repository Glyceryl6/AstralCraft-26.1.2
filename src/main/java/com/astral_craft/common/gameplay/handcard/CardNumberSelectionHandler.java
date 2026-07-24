package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.network.c2s.CardNumberSelectionPayload;
import net.minecraft.server.level.ServerPlayer;

/** Lets the selected card own validation and application of its number-selection response. */
public interface CardNumberSelectionHandler {

    void applyNumberSelection(ServerPlayer user, CardNumberSelectionPayload payload);
}
