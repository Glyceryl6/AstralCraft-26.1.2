package com.astral_craft.client.gui.reveal;

public record CardRevealFrame(
        boolean front,
        float cardScale,
        float textScale,
        float widthScale,
        float heightScale,
        float alpha,
        int centerYOffset,
        boolean renderText) { }