package com.astral_craft.client.gui.reveal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record CardReveal(
        String cardId,
        String cardType,
        String title,
        String body,
        ItemStack stack,
        Identifier frontTexture,
        Identifier backTexture,
        Identifier animation,
        long startedAtNanos,
        int durationTicks) { }