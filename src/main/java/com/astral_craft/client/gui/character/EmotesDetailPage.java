package com.astral_craft.client.gui.character;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class EmotesDetailPage implements CharacterDetailPage {

    protected final CharacterSettingsScreen screen;

    public EmotesDetailPage(CharacterSettingsScreen screen) {
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        this.screen.renderBodyContainer(graphics, layout);
        int headerX = layout.bodyX + 14;
        int headerY = layout.bodyY + 14;
        int contentX = layout.bodyX + 18;
        int contentTop = layout.bodyY + 38;
        int contentBottom = layout.bodyY + layout.bodyH - 12;
        int maxWidth = layout.bodyW - 36;
        int y = contentTop - Math.round(this.screen.bodyScroll);
        this.screen.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.main.emotes"), headerX, headerY, 0xFFE83CA8, maxWidth);
        graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
        this.screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.emotes_placeholder"), contentX + 8, y + 4, 0xFFE7E7E7, maxWidth - 16);
        graphics.disableScissor();
        this.screen.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, contentTop, contentBottom - contentTop, this.screen.bodyScroll, this.maxScroll(layout));
    }

    @Override
    public float maxScroll(CharacterLayout layout) {
        int visible = Math.max(10, layout.bodyH - 50);
        int content = this.screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.emotes_placeholder"), Math.max(40, layout.bodyW - 60)) + 30;
        return content > visible + 14 ? content - visible : 0.0F;
    }

}