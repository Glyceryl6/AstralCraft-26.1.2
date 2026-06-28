package com.astral_craft.client.gui.character;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinRarityDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

public class SkinCardComponent {

    protected final CharacterSettingsScreen screen;
    protected final CharacterDefinition definition;
    protected final CharacterSkinDefinition skin;
    protected final LivingEntity entity;
    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;
    protected final boolean selected;
    protected final boolean equipped;
    protected final boolean unlocked;
    protected final boolean hovered;
    protected final float scale;

    public SkinCardComponent(CharacterSettingsScreen screen, CharacterDefinition definition, CharacterSkinDefinition skin, LivingEntity entity, int x, int y, int width, int height, boolean selected, boolean equipped, boolean unlocked, boolean hovered, float scale) {
        this.screen = screen;
        this.definition = definition;
        this.skin = skin;
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.selected = selected;
        this.equipped = equipped;
        this.unlocked = unlocked;
        this.hovered = hovered;
        this.scale = scale;
    }

    public void render(GuiGraphicsExtractor graphics) {
        this.screen.hoveredClickable |= this.hovered;
        AstralFancyButton.renderIconFrame(graphics, this.x, this.y, this.width, this.height, this.selected || this.equipped, this.hovered);
        if (this.entity != null) {
            this.screen.renderEntityModel(graphics, this.entity, this.x + 8, this.y + 15, this.x + this.width - 8, this.y + this.height - 35, -225.0F, -10.0F, 0.0F, this.scale);
        }

        if (!this.unlocked) {
            this.screen.renderLockedOverlay(graphics, this.x, this.y, this.width, this.height, Component.translatable(this.skin.unlockedByDefault() ? this.definition.unlockHintKey() : "gui.astral_craft.character_settings.skin_locked"));
        }

        this.renderRarityBadge(graphics);
        MutableComponent name = Component.translatable(this.skin.nameKey());
        this.screen.drawCenteredText(graphics, this.screen.ellipsize(name, this.width - 8), this.x, this.y + this.height - 17, this.width, 0xFFFFFFFF);
    }

    protected void renderRarityBadge(GuiGraphicsExtractor graphics) {
        String rarity = this.skin.rarityOrNone();
        if (!AstralSkinRarityManager.INSTANCE.shouldRenderBadge(rarity)) return;
        CharacterSkinRarityDefinition definition = AstralSkinRarityManager.INSTANCE.getOrDefault(rarity);
        int color = definition.borderColor();
        graphics.fill(this.x + 2, this.y + 2, this.x + this.width - 2, this.y + 4, color);
        graphics.fill(this.x + 2, this.y + 2, this.x + 4, this.y + this.height - 2, color);
        graphics.fill(this.x + this.width - 4, this.y + 2, this.x + this.width - 2, this.y + this.height - 2, color);
        int badgeW = Math.clamp(this.screen.font().width(Component.translatable(definition.nameKey()).getString()) + 8, 34, this.width - 10);
        int badgeX = this.x + this.width - badgeW - 5;
        int badgeY = this.y + 6;
        graphics.fill(badgeX - 1, badgeY - 1, badgeX + badgeW + 1, badgeY + 10, 0xDD000000);
        graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 9, definition.badgeColor());
        this.screen.drawCenteredText(graphics, this.screen.ellipsize(Component.translatable(definition.nameKey()), badgeW - 3), badgeX, badgeY + 1, badgeW, definition.textColor());
    }

}