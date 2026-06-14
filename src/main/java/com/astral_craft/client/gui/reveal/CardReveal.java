package com.astral_craft.client.gui.reveal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record CardReveal(
        String cardId,
        String title,
        String body,
        ItemStack stack,
        Identifier frontTexture,
        Identifier backTexture,
        String animation,
        long startedAtNanos,
        int durationTicks) { }