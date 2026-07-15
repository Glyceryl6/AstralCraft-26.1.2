package com.astral_craft.client.gui.reveal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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
        List<Integer> targetEntityIds) { }