package com.astral_craft.client.gui.character;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface CharacterDetailPage {

    void render(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY);

    default boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
        return false;
    }

    default boolean usesSkinScroll() {
        return false;
    }

    default float maxScroll(CharacterLayout layout) {
        return 0.0F;
    }

}