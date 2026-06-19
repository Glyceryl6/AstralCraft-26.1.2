package com.astral_craft.client.gui.character;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ProgressLevelCardComponent {

    public static final int MIN_HEIGHT = 42;
    public static final int GAP = 6;

    protected final CharacterSettingsScreen screen;
    protected final CharacterDefinition definition;
    protected final int level;
    protected final int currentLevel;
    protected final String translationPrefix;
    protected final int accentColor;

    public ProgressLevelCardComponent(CharacterSettingsScreen screen, CharacterDefinition definition, int level, int currentLevel, String translationPrefix, int accentColor) {
        this.screen = screen;
        this.definition = definition;
        this.level = level;
        this.currentLevel = currentLevel;
        this.translationPrefix = translationPrefix;
        this.accentColor = accentColor;
    }

    public int height(int maxWidth) {
        int badgeW = this.badgeWidth(maxWidth);
        int textWidth = Math.max(40, maxWidth - badgeW - 24);
        MutableComponent description = this.description();
        int textHeight = Math.max(10, this.screen.wrap(description.getString(), textWidth).size() * 11);
        return Math.max(MIN_HEIGHT, textHeight + 18);
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int maxWidth) {
        int cardHeight = this.height(maxWidth);
        boolean reached = this.currentLevel >= this.level;
        int fill = reached ? 0xDD20202A : 0xAA101018;
        int border = reached ? this.accentColor : 0xFF55555F;
        AstralFancyButton.renderFlatBox(graphics, x, y, maxWidth, cardHeight, fill, border, 0xEE000000, 0x44000000, 2, 2);

        int badgeW = this.badgeWidth(maxWidth);
        int badgeH = 18;
        int badgeX = x + 6;
        int badgeY = y + (cardHeight - badgeH) / 2;
        int badgeFill = reached ? this.accentColor : 0xFF55555F;
        AstralFancyButton.renderFlatBox(graphics, badgeX, badgeY, badgeW, badgeH, badgeFill, 0xFFFFFFFF, 0xFF101018, 0x33000000, 1, 1);
        this.screen.drawCenteredText(graphics, Component.translatable(this.translationPrefix + ".title", this.level), badgeX, badgeY + 5, badgeW, reached ? 0xFF101018 : 0xFFE7E7E7);

        MutableComponent description = this.description();
        int textX = x + badgeW + 14;
        int textW = Math.max(40, maxWidth - badgeW - 20);
        int textH = Math.max(10, this.screen.wrap(description.getString(), textW).size() * 11);
        int textY = y + Math.max(7, (cardHeight - textH) / 2);
        this.screen.drawWrapped(graphics, description, textX, textY, reached ? 0xFFEFEFFF : 0xFF999999, textW);
    }

    protected int badgeWidth(int maxWidth) {
        return Math.clamp(maxWidth / 5, 48, 64);
    }

    protected MutableComponent description() {
        return Component.translatable(this.translationPrefix + ".desc", Component.translatable(this.definition.nameKey()), this.level);
    }

}
