package com.astral_craft.client.gui.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ContractDetailPage implements CharacterDetailPage {

    protected final CharacterSettingsScreen screen;

    public ContractDetailPage(CharacterSettingsScreen screen) {
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
        this.screen.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.main.contract"), headerX, headerY, 0xFF8CFF20, maxWidth);
        graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
        y = this.screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.friendship_value", this.screen.friendship, this.screen.selectedCharacter().maxFriendshipLevel()), contentX + 8, y + 2, 0xFFFFC0E8, maxWidth - 16);
        y += 5;
        this.screen.renderProgressCards(graphics, this.screen.selectedCharacter(), contentX + 8, y, maxWidth - 16, 1, this.screen.selectedCharacter().maxFriendshipLevel(), this.screen.friendship, "gui.astral_craft.character_settings.friendship_card", 0xFFFF4FAE);
        graphics.disableScissor();
        this.screen.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, contentTop, contentBottom - contentTop, this.screen.bodyScroll, this.maxScroll(layout));
    }

    @Override
    public float maxScroll(CharacterLayout layout) {
        int visible = Math.max(10, layout.bodyH - 50);
        int content = this.estimatedHeight(layout);
        return content > visible + 14 ? content - visible : 0.0F;
    }

    protected int estimatedHeight(CharacterLayout layout) {
        CharacterDefinition definition = this.screen.selectedCharacter();
        return 48 + this.screen.progressCardsHeight(definition, Math.max(40, layout.bodyW - 60), 1, definition.maxFriendshipLevel(), this.screen.friendship, "gui.astral_craft.character_settings.friendship_card", 0xFFFF4FAE);
    }

}
