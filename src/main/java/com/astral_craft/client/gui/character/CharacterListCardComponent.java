package com.astral_craft.client.gui.character;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

public record CharacterListCardComponent(
        CharacterSettingsScreen screen, CharacterDefinition definition,
        LivingEntity entity, int x, int y, int width, int height,
        boolean selected, boolean unlocked, boolean hovered, float scale) {

    public void render(GuiGraphicsExtractor graphics) {
        this.screen.hoveredClickable |= this.hovered;
        AstralFancyButton.renderIconFrame(graphics, this.x, this.y, this.width, this.height, this.selected, this.hovered);
        if (this.entity != null) {
            this.screen.renderEntityModel(graphics, this.entity, this.x + 8, this.y + 7, this.x + this.width - 8, this.y + this.height - 38, -225.0F, -10.0F, 0.0F, this.scale);
        }

        CharacterProgressEntry entry = this.screen.progressEntry(this.definition.id());
        if (!this.unlocked) {
            this.screen.renderLockedOverlay(graphics, this.x, this.y, this.width, this.height, Component.translatable(this.definition.unlockHintKey()));
        }

        MutableComponent name = Component.translatable(this.definition.nameKey());
        this.screen.drawLine(graphics, this.screen.ellipsize(name, this.width - 8), this.x + 4, this.y + this.height - 29, this.unlocked ? 0xFFFFFFFF : 0xFFB7B7B7, this.width - 8);
        this.screen.drawCenteredText(graphics, Component.translatable("gui.astral_craft.character_settings.card_pve", entry.level(), this.definition.maxPveLevel()), this.x + 3, this.y + this.height - 18, Math.max(20, this.width / 2 - 3), this.unlocked ? 0xFFBFE6FF : 0xFF888888);
        this.screen.drawCenteredText(graphics, Component.translatable("gui.astral_craft.character_settings.card_friendship", entry.friendship(), this.definition.maxFriendshipLevel()), this.x + Math.max(20, this.width / 2), this.y + this.height - 18, Math.max(20, this.width / 2 - 3), this.unlocked ? 0xFFFFC0E8 : 0xFF888888);
    }

}