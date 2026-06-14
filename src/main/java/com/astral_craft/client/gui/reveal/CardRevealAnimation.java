package com.astral_craft.client.gui.reveal;

public interface CardRevealAnimation {

    String id();

    int defaultDuration(CardRevealSettings settings);

    void render(CardRevealRenderContext context, CardRevealRenderer renderer);

}