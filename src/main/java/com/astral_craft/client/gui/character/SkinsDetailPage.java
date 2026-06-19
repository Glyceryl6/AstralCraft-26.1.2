package com.astral_craft.client.gui.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterSkinDefinition;
import com.astral_craft.common.network.CharacterSkinSelectionPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class SkinsDetailPage implements CharacterDetailPage {

    protected final CharacterSettingsScreen screen;

    public SkinsDetailPage(CharacterSettingsScreen screen) {
        this.screen = screen;
    }

    @Override
    public boolean usesSkinScroll() {
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        this.screen.renderBodyContainer(graphics, layout);
        CharacterDefinition definition = this.screen.selectedCharacter();
        int cardW = layout.skinCardW;
        int cardH = layout.skinCardH;
        int listLeft = layout.bodyX + 14;
        int listTop = layout.bodyY + 14;
        int listRight = layout.bodyX + layout.bodyW - 14;
        int listBottom = Math.max(listTop + 1, Math.min(layout.bodyY + layout.bodyH - 26, listTop + cardH + 8));
        graphics.enableScissor(listLeft, listTop, listRight, listBottom);
        for (int i = 0; i < definition.skins().size(); i++) {
            int x = listLeft + i * (cardW + CharacterLayout.GRID_GAP) - Math.round(this.screen.skinScroll);
            int y = listTop + 2;
            if (x + cardW < listLeft || x > listRight) continue;
            CharacterSkinDefinition skin = definition.skins().get(i);
            boolean selected = skin.id().equals(this.screen.selectedSkinId);
            boolean equipped = skin.id().equals(this.screen.equippedSkinId) && definition.id().equals(this.screen.equippedCharacterId);
            boolean unlocked = this.screen.isCharacterUnlocked(definition) && this.screen.isSkinUnlocked(definition, skin);
            boolean hovered = this.screen.isInside(mouseX, mouseY, x, y, cardW, cardH);
            new SkinCardComponent(this.screen, definition, skin, this.screen.entityFor(definition, skin.id()), x, y, cardW, cardH, selected, equipped, unlocked, hovered, layout.skinEntityScale).render(graphics);
        }

        if (definition.skins().isEmpty()) {
            this.screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_skins"), layout.bodyX + 18, layout.bodyY + 22, 0xFFD0D0D0, layout.bodyW - 36);
        }

        graphics.disableScissor();
        this.screen.renderHorizontalScrollbar(graphics, listLeft, layout.bodyY + layout.bodyH - 18, listRight - listLeft, this.screen.skinScroll, this.maxScroll(layout));
    }

    @Override
    public boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
        CharacterDefinition definition = this.screen.selectedCharacter();
        int cardW = layout.skinCardW;
        int cardH = layout.skinCardH;
        int listLeft = layout.bodyX + 14;
        int listTop = layout.bodyY + 14;
        for (int i = 0; i < definition.skins().size(); i++) {
            int x = listLeft + i * (cardW + CharacterLayout.GRID_GAP) - Math.round(this.screen.skinScroll);
            int y = listTop + 2;
            if (this.screen.isInside(mouseX, mouseY, x, y, cardW, cardH)) {
                CharacterSkinDefinition skin = definition.skins().get(i);
                this.screen.selectedSkinId = skin.id();
                if (this.screen.isCharacterUnlocked(definition) && this.screen.isSkinUnlocked(definition, skin)) {
                    this.screen.equippedSkinId = this.screen.selectedSkinId;
                    ClientPacketDistributor.sendToServer(new CharacterSkinSelectionPayload(definition.id().toString(), this.screen.selectedSkinId));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public float maxScroll(CharacterLayout layout) {
        return this.screen.maxSkinScroll(layout);
    }

}
