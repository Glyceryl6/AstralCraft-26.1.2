package com.astral_craft.client.gui.reveal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record CardReveal(
        String cardId,
        String cardType,
        Component title,
        Component body,
        ItemStack stack,
        Identifier frontTexture,
        Identifier backTexture,
        Identifier animation,
        double startedAtTick,
        int durationTicks,
        int sourceEntityId,
        List<Integer> targetEntityIds,
        UUID revealId,
        boolean held) {

    public CardReveal withStartedAt(double startedAtTick) {
        return new CardReveal(this.cardId, this.cardType, this.title, this.body, this.stack,
                this.frontTexture, this.backTexture, this.animation, startedAtTick, this.durationTicks,
                this.sourceEntityId, this.targetEntityIds, this.revealId, this.held);
    }

    public CardReveal withHeld(boolean held) {
        return new CardReveal(this.cardId, this.cardType, this.title, this.body, this.stack,
                this.frontTexture, this.backTexture, this.animation, this.startedAtTick, this.durationTicks,
                this.sourceEntityId, this.targetEntityIds, this.revealId, held);
    }

}