package com.astral_craft.client.gui.reveal;

import net.minecraft.resources.Identifier;

public interface CardRevealAnimation {

    Identifier id();

    int defaultDuration(CardRevealSettings settings);

    void render(CardRevealRenderContext context, CardRevealRenderer renderer);

}