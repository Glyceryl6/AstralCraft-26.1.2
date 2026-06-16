package com.astral_craft.client.gui.character;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.gameplay.character.*;
import com.astral_craft.common.network.CharacterSelectionPayload;
import com.astral_craft.common.network.CharacterSkinSelectionPayload;
import com.astral_craft.common.network.OpenCharacterSettingsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CharacterSettingsScreen extends Screen {

    protected static final int MIN_SCREEN_MARGIN = 10;
    protected static final int PANEL_GAP = 10;
    protected static final int MAX_TOTAL_WIDTH = 790;
    protected static final int MAX_TOTAL_HEIGHT = 420;
    protected static final int MIN_TOTAL_HEIGHT = 250;
    protected static final int MIN_LEFT_WIDTH = 104;
    protected static final int MAX_LEFT_WIDTH = 190;
    protected static final int MIN_RIGHT_WIDTH = 250;
    protected static final int GRID_GAP = 8;
    protected static final int TAB_GAP = 4;

    protected final List<CharacterDefinition> characters;
    protected Identifier selectedCharacterId;
    protected String selectedSkinId;
    protected int level;
    protected int experience;
    protected int friendship;
    protected ScreenMode mode = ScreenMode.LIST;
    protected MainTab mainTab = MainTab.ARCHIVE;
    protected ArchiveTab archiveTab = ArchiveTab.SKILLS;
    protected float characterScroll;
    protected float bodyScroll;
    protected float skinScroll;
    protected float previewYaw = 205.0F;
    protected float previewPitch = 8.0F;
    protected float previewRoll = 0.0F;
    protected float previewZoom = 1.0F;
    protected boolean draggingPreview;
    protected int dragButton;
    protected double lastDragX;
    protected double lastDragY;
    protected LivingEntity previewEntity;
    protected Object previewLevelKey;
    protected boolean hoveredClickable;

    public CharacterSettingsScreen(List<CharacterDefinition> characters, Identifier selectedCharacterId, String selectedSkinId, int level, int experience, int friendship) {
        super(Component.translatable("gui.astral_craft.character_settings.title"));
        this.characters = characters.isEmpty() ? List.of(CharacterDefinition.builtinDefault()) : characters;
        this.selectedCharacterId = selectedCharacterId;
        this.selectedSkinId = selectedSkinId == null || selectedSkinId.isBlank() ? "default" : selectedSkinId;
        this.level = level;
        this.experience = experience;
        this.friendship = friendship;
        if (this.selectedCharacterId == null) {
            this.selectedCharacterId = this.characters.getFirst().id();
        }
    }

    public static void open(OpenCharacterSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            List<CharacterDefinition> definitions = CharacterCodecLines.decode(payload.encodedCharacters());
            Identifier selected = definitions.stream().findFirst().map(CharacterDefinition::id).orElse(CharacterDefinition.builtinDefault().id());
            try {
                selected = Identifier.parse(payload.selectedCharacterId());
            } catch (Exception ignored) {}
            Minecraft.getInstance().setScreen(new CharacterSettingsScreen(definitions, selected, payload.selectedSkinId(), payload.level(), payload.experience(), payload.friendship()));
        });
    }

    @Override
    protected void init() {
        this.clearWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.clampScrolls();
        this.hoveredClickable = false;
        CharacterLayout layout = this.layout();
        this.renderShell(graphics, layout, mouseX, mouseY);
        this.renderPreview(graphics, layout, mouseX, mouseY);
        if (this.mode == ScreenMode.LIST) {
            this.renderCharacterListPage(graphics, layout, mouseX, mouseY);
        } else {
            this.renderDetailPage(graphics, layout, mouseX, mouseY);
        }

        AstralFancyButton.setHandCursor(this.hoveredClickable);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        AstralFancyButton.setHandCursor(false);
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        CharacterLayout layout = this.layout();
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.backX, layout.backY, layout.backW, layout.backH)) {
            this.handleBack();
            return true;
        }

        if (this.isOverPreview(layout, mouseX, mouseY)) {
            if (doubleClick) {
                this.resetPreviewRotation();
                return true;
            }

            this.draggingPreview = true;
            this.dragButton = event.button();
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            return true;
        }

        if (this.mode == ScreenMode.LIST) {
            if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.detailButtonX, layout.detailButtonY, layout.detailButtonW, layout.detailButtonH)) {
                this.mode = ScreenMode.DETAIL;
                this.mainTab = MainTab.ARCHIVE;
                this.archiveTab = ArchiveTab.SKILLS;
                this.bodyScroll = 0.0F;
                this.skinScroll = 0.0F;
                return true;
            }
            if (event.button() == 0 && this.handleCharacterGridClick(layout, mouseX, mouseY)) {
                return true;
            }
        } else {
            if (event.button() == 0 && this.handleMainTabClick(layout, mouseX, mouseY)) {
                return true;
            }
            if (event.button() == 0 && this.mainTab == MainTab.ARCHIVE && this.handleArchiveTabClick(layout, mouseX, mouseY)) {
                return true;
            }
            if (event.button() == 0 && this.mainTab == MainTab.SKINS && this.handleSkinClick(layout, mouseX, mouseY)) {
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingPreview) {
            float dx = (float) (event.x() - this.lastDragX);
            float dy = (float) (event.y() - this.lastDragY);
            if (this.dragButton == 1) {
                this.previewRoll += dx * 0.55F;
            } else {
                this.previewYaw += dx * 0.82F;
                this.previewPitch = Mth.clamp(this.previewPitch - dy * 0.62F, -72.0F, 72.0F);
            }

            this.lastDragX = event.x();
            this.lastDragY = event.y();
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingPreview && event.button() == this.dragButton) {
            this.draggingPreview = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        CharacterLayout layout = this.layout();
        if (this.isOverPreview(layout, mouseX, mouseY)) {
            this.previewZoom = Mth.clamp(this.previewZoom + (float) deltaY * 0.08F, 0.72F, 1.34F);
            return true;
        }

        if (this.mode == ScreenMode.LIST && this.isInside(mouseX, mouseY, layout.gridX, layout.gridY, layout.gridW, layout.gridH)) {
            this.characterScroll = Mth.clamp(this.characterScroll - (float) deltaY * 28.0F, 0.0F, this.maxCharacterScroll(layout));
            return true;
        }

        if (this.mode == ScreenMode.DETAIL && this.isInside(mouseX, mouseY, layout.bodyX, layout.bodyY, layout.bodyW, layout.bodyH)) {
            if (this.mainTab == MainTab.SKINS) {
                this.skinScroll = Mth.clamp(this.skinScroll - (float) deltaY * 28.0F, 0.0F, this.maxSkinScroll(layout));
                return true;
            }

            float maxScroll = this.maxBodyScroll(layout);
            if (maxScroll > 0.5F) {
                this.bodyScroll = Mth.clamp(this.bodyScroll - (float) deltaY * 22.0F, 0.0F, maxScroll);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.handleBack();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_R) {
            this.resetPreviewRotation();
            return true;
        }
        return super.keyPressed(event);
    }

    protected void renderShell(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        graphics.fill(layout.leftX, layout.topY, layout.leftX + layout.leftW, layout.topY + layout.totalH, 0xD214141D);
        graphics.fill(layout.rightX, layout.topY, layout.rightX + layout.rightW, layout.topY + layout.totalH, 0xD016161F);
        graphics.fill(layout.leftX, layout.topY, layout.leftX + layout.leftW, layout.topY + 30, 0xCC232333);
        graphics.fill(layout.rightX, layout.topY, layout.rightX + layout.rightW, layout.topY + 36, 0xCC232333);
        boolean backHover = this.isInside(mouseX, mouseY, layout.backX, layout.backY, layout.backW, layout.backH);
        Component backText = Component.translatable(this.mode == ScreenMode.DETAIL ? "gui.astral_craft.character_settings.back" : "gui.astral_craft.character_settings.close");
        this.renderFancyButton(graphics, backText, layout.backX, layout.backY, layout.backW, layout.backH, false, backHover, 0xCC2E74FF);
        Component title = Component.translatable(this.mode == ScreenMode.LIST ? "gui.astral_craft.character_settings.character_select" : "gui.astral_craft.character_settings.character_detail");
        graphics.text(this.font, title, layout.rightX + 14, layout.topY + 13, 0xFFFFFFFF, false);
    }

    protected void renderPreview(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        CharacterDefinition selected = this.selectedCharacter();
        int previewTop = layout.previewY;
        int previewBottom = layout.previewY + layout.previewH;
        graphics.fill(layout.previewX, previewTop, layout.previewX + layout.previewW, previewBottom, 0x99000000);
        graphics.fill(layout.previewX + 2, previewTop + 2, layout.previewX + layout.previewW - 2, previewBottom - 2, 0x55303548);
        graphics.fill(layout.previewX + 4, previewTop + 4, layout.previewX + layout.previewW - 4, previewTop + 5, 0x99FF4FAE);
        LivingEntity entity = this.previewEntity();
        if (entity != null) {
            this.renderEntityModel(graphics, entity, layout.previewX + 8, previewTop + 8, layout.previewX + layout.previewW - 8, previewBottom - 10, this.previewYaw, this.previewPitch, this.previewRoll, layout.previewEntityScale * this.previewZoom);
        }

        int infoY = layout.previewY + layout.previewH + 10;
        graphics.text(this.font, Component.translatable(selected.nameKey()), layout.leftX + 12, infoY, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.translatable(selected.titleKey()), layout.leftX + 12, infoY + 13, 0xFFE7E7E7, false);
        graphics.text(this.font, Component.translatable("gui.astral_craft.character_settings.preview_hint"), layout.leftX + 12, infoY + 29, 0xFFB0B0C0, false);
    }

    protected void renderCharacterListPage(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        boolean detailHover = this.isInside(mouseX, mouseY, layout.detailButtonX, layout.detailButtonY, layout.detailButtonW, layout.detailButtonH);
        Component detailText = Component.translatable("gui.astral_craft.character_settings.character_detail");
        this.renderFancyButton(graphics, detailText, layout.detailButtonX, layout.detailButtonY, layout.detailButtonW, layout.detailButtonH, false, detailHover, 0xFFE83CA8);
        graphics.fill(layout.gridX - 6, layout.gridY - 6, layout.gridX + layout.gridW + 6, layout.gridY + layout.gridH + 6, 0x66101018);
        graphics.enableScissor(layout.gridX, layout.gridY, layout.gridX + layout.gridW, layout.gridY + layout.gridH);
        int columns = this.characterGridColumns(layout);
        int cardW = layout.characterCardW;
        int cardH = layout.characterCardH;
        LivingEntity entity = this.previewEntity();
        for (int i = 0; i < this.characters.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int cardX = layout.gridX + column * (cardW + GRID_GAP);
            int cardY = layout.gridY + row * (cardH + GRID_GAP) - Math.round(this.characterScroll);
            if (cardY + cardH < layout.gridY || cardY > layout.gridY + layout.gridH) continue;
            CharacterDefinition definition = this.characters.get(i);
            boolean selected = definition.id().equals(this.selectedCharacterId);
            boolean hovered = this.isInside(mouseX, mouseY, cardX, cardY, cardW, cardH);
            this.renderCharacterCard(graphics, definition, entity, cardX, cardY, cardW, cardH, selected, hovered, layout.cardEntityScale);
        }

        graphics.disableScissor();
        this.renderVerticalScrollbar(graphics, layout.gridX + layout.gridW + 2, layout.gridY, layout.gridH, this.characterScroll, this.maxCharacterScroll(layout));
    }

    protected void renderDetailPage(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        this.renderMainTabs(graphics, layout, mouseX, mouseY);
        if (this.mainTab == MainTab.ARCHIVE) {
            this.renderArchiveTabs(graphics, layout, mouseX, mouseY);
        }

        this.renderBodyContainer(graphics, layout);
        if (this.mainTab == MainTab.ARCHIVE) {
            this.renderArchiveContent(graphics, layout);
        } else if (this.mainTab == MainTab.CONTRACT) {
            this.renderContractContent(graphics, layout);
        } else if (this.mainTab == MainTab.SKINS) {
            this.renderSkinContent(graphics, layout, mouseX, mouseY);
        } else {
            this.renderEmoteContent(graphics, layout);
        }
    }

    protected void renderMainTabs(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        int x = layout.mainTabX;
        int tabW = layout.mainTabW;
        for (MainTab value : MainTab.values()) {
            boolean selected = this.mainTab == value;
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.mainTabY, tabW, layout.mainTabH);
            this.renderTab(graphics, x, layout.mainTabY, tabW, layout.mainTabH, value.translationKey, selected, hovered, selected ? 0xFF8CFF20 : 0xFFE83CA8);
            x += tabW + TAB_GAP;
        }
    }

    protected void renderArchiveTabs(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        int x = layout.subTabX;
        int tabW = layout.subTabW;
        for (ArchiveTab value : ArchiveTab.values()) {
            boolean selected = this.archiveTab == value;
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.subTabY, tabW, layout.subTabH);
            Component text = Component.translatable(value.translationKey);
            this.renderFancyButton(graphics, text, x, layout.subTabY, tabW, layout.subTabH, selected, hovered, 0xFFE83CA8);
            x += tabW + TAB_GAP;
        }
    }

    protected void renderBodyContainer(GuiGraphicsExtractor graphics, CharacterLayout layout) {
        graphics.fill(layout.bodyX, layout.bodyY, layout.bodyX + layout.bodyW, layout.bodyY + layout.bodyH, 0xD00B0B11);
        graphics.fill(layout.bodyX + 2, layout.bodyY + 2, layout.bodyX + layout.bodyW - 2, layout.bodyY + layout.bodyH - 2, 0x88444852);
        graphics.fill(layout.bodyX + 8, layout.bodyY + 8, layout.bodyX + layout.bodyW - 8, layout.bodyY + 9, 0x99FFFFFF);
    }

    protected void renderArchiveContent(GuiGraphicsExtractor graphics, CharacterLayout layout) {
        CharacterDefinition definition = this.selectedCharacter();
        int headerX = layout.bodyX + 14;
        int headerY = layout.bodyY + 14;
        int maxWidth = layout.bodyW - 34;
        int contentX = layout.bodyX + 18;
        int contentTop = layout.bodyY + 38;
        int contentBottom = layout.bodyY + layout.bodyH - 12;
        int y = contentTop - Math.round(this.bodyScroll);
        this.drawHeader(graphics, Component.translatable(this.archiveTab.titleKey()), headerX, headerY, this.archiveTab.headerColor(), maxWidth);
        graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
        if (this.archiveTab == ArchiveTab.SKILLS) {
            for (CharacterSkillDefinition skill : definition.skills()) {
                y = this.drawHeader(graphics, Component.translatable(skill.nameKey()), contentX, y, 0xFFFFF2A0, maxWidth - 8);
                y = this.drawWrapped(graphics, Component.translatable(skill.descriptionKey()), contentX + 8, y + 2, 0xFFE7E7E7, maxWidth - 16);
                if (skill.cooldown() > 0) {
                    y = this.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.cooldown", skill.cooldown()), contentX + 8, y + 2, 0xFFB0B0B0, maxWidth - 16);
                }
                y += 8;
            }
            if (definition.skills().isEmpty()) {
                this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_skills"), contentX, y, 0xFFD0D0D0, maxWidth - 8);
            }
        } else if (this.archiveTab == ArchiveTab.LEVEL) {
            y = this.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.level_value", this.level), contentX + 8, y + 2, 0xFFFFFFFF, maxWidth - 16);
            y = this.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.experience_value", this.experience), contentX + 8, y + 2, 0xFFBFE6FF, maxWidth - 16);
            y = this.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.friendship_value", this.friendship), contentX + 8, y + 2, 0xFFFFC0E8, maxWidth - 16);
            this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.level_placeholder"), contentX + 8, y + 8, 0xFFE7E7E7, maxWidth - 16);
        } else if (this.archiveTab == ArchiveTab.POTENTIAL) {
            this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.potential_placeholder"), contentX + 8, y + 4, 0xFFE7E7E7, maxWidth - 16);
        } else {
            for (CharacterProfileSection section : definition.profileSections()) {
                if (this.shouldRenderProfileSectionHeader(section)) {
                    y = this.drawHeader(graphics, Component.translatable(section.titleKey()), contentX, y, 0xFFFFA0FF, maxWidth - 8);
                }
                y = this.drawWrapped(graphics, Component.translatable(section.bodyKey()), contentX + 8, y + 2, 0xFFE7E7E7, maxWidth - 16);
                y += 8;
            }

            if (definition.profileSections().isEmpty()) {
                this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_profile"), contentX, y, 0xFFD0D0D0, maxWidth - 8);
            }
        }

        graphics.disableScissor();
        this.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, contentTop, contentBottom - contentTop, this.bodyScroll, this.maxBodyScroll(layout));
    }

    protected void renderContractContent(GuiGraphicsExtractor graphics, CharacterLayout layout) {
        int headerX = layout.bodyX + 14;
        int headerY = layout.bodyY + 14;
        int contentX = layout.bodyX + 18;
        int contentTop = layout.bodyY + 38;
        int contentBottom = layout.bodyY + layout.bodyH - 12;
        int maxWidth = layout.bodyW - 36;
        int y = contentTop - Math.round(this.bodyScroll);
        this.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.main.contract"), headerX, headerY, 0xFF8CFF20, maxWidth);
        graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
        this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.contract_placeholder"), contentX + 8, y + 4, 0xFFE7E7E7, maxWidth - 16);
        graphics.disableScissor();
        this.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, contentTop, contentBottom - contentTop, this.bodyScroll, this.maxBodyScroll(layout));
    }

    protected void renderSkinContent(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        CharacterDefinition definition = this.selectedCharacter();
        int columns = this.skinColumns(layout);
        int cardW = layout.skinCardW;
        int cardH = layout.skinCardH;
        int listTop = layout.bodyY + 14;
        int listBottom = layout.bodyY + layout.bodyH - 36;
        LivingEntity entity = this.previewEntity();
        graphics.enableScissor(layout.bodyX + 8, listTop, layout.bodyX + layout.bodyW - 8, listBottom);
        for (int i = 0; i < definition.skins().size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int x = layout.bodyX + 14 + column * (cardW + GRID_GAP);
            int y = listTop + 2 + row * (cardH + GRID_GAP) - Math.round(this.skinScroll);
            if (y + cardH < listTop || y > listBottom) continue;
            CharacterSkinDefinition skin = definition.skins().get(i);
            boolean selected = skin.id().equals(this.selectedSkinId);
            boolean hovered = this.isInside(mouseX, mouseY, x, y, cardW, cardH);
            this.hoveredClickable |= hovered;
            AstralFancyButton.renderIconFrame(graphics, x, y, cardW, cardH, selected, hovered);
            if (entity != null) {
                this.renderEntityModel(graphics, entity, x + 8, y + 7, x + cardW - 8, y + cardH - 34, 205.0F, 10.0F, 0.0F, layout.skinEntityScale);
            }

            Component name = Component.translatable(skin.nameKey());
            this.drawCenteredText(graphics, this.ellipsize(name, cardW - 8), x, y + cardH - 22, cardW, selected ? 0xFF101018 : 0xFFFFFFFF);
        }

        if (definition.skins().isEmpty()) {
            this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_skins"), layout.bodyX + 18, layout.bodyY + 22, 0xFFD0D0D0, layout.bodyW - 36);
        }

        graphics.disableScissor();
        this.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, listTop, listBottom - listTop, this.skinScroll, this.maxSkinScroll(layout));
        CharacterSkinDefinition selectedSkin = definition.skinOrDefault(this.selectedSkinId);
        Component currentSkin = Component.translatable("gui.astral_craft.character_settings.current_skin", Component.translatable(selectedSkin.nameKey()));
        graphics.fill(layout.bodyX + 8, layout.bodyY + layout.bodyH - 28, layout.bodyX + layout.bodyW - 8, layout.bodyY + layout.bodyH - 8, 0xAA000000);
        graphics.text(this.font, currentSkin, layout.bodyX + 14, layout.bodyY + layout.bodyH - 22, 0xFFFFFFFF, false);
    }

    protected void renderEmoteContent(GuiGraphicsExtractor graphics, CharacterLayout layout) {
        int headerX = layout.bodyX + 14;
        int headerY = layout.bodyY + 14;
        int contentX = layout.bodyX + 18;
        int contentTop = layout.bodyY + 38;
        int contentBottom = layout.bodyY + layout.bodyH - 12;
        int maxWidth = layout.bodyW - 36;
        int y = contentTop - Math.round(this.bodyScroll);
        this.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.main.emotes"), headerX, headerY, 0xFFE83CA8, maxWidth);
        graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
        this.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.emotes_placeholder"), contentX + 8, y + 4, 0xFFE7E7E7, maxWidth - 16);
        graphics.disableScissor();
        this.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, contentTop, contentBottom - contentTop, this.bodyScroll, this.maxBodyScroll(layout));
    }

    protected void renderCharacterCard(GuiGraphicsExtractor graphics, CharacterDefinition definition, LivingEntity entity, int x, int y, int w, int h, boolean selected, boolean hovered, float scale) {
        this.hoveredClickable |= hovered;
        AstralFancyButton.renderIconFrame(graphics, x, y, w, h, selected, hovered);
        if (entity != null) {
            this.renderEntityModel(graphics, entity, x + 8, y + 7, x + w - 8, y + h - 26, 205.0F, 10.0F, 0.0F, scale);
        }

        Component name = Component.translatable(definition.nameKey());
        graphics.text(this.font, this.ellipsize(name, w - 8), x + 4, y + h - 17, 0xFFFFFFFF, false);
    }

    protected void renderTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String translationKey, boolean selected, boolean hovered, int accent) {
        Component text = Component.translatable(translationKey);
        this.hoveredClickable |= hovered;
        AstralFancyButton.renderTab(graphics, this.font, text, x, y, w, h, selected, hovered, accent);
    }

    protected void renderFancyButton(GuiGraphicsExtractor graphics, Component text, int x, int y, int w, int h, boolean selected, boolean hovered, int accent) {
        this.hoveredClickable |= hovered;
        AstralFancyButton.renderButton(graphics, this.font, text, x, y, w, h, selected, hovered, accent);
    }

    protected void drawCenteredText(GuiGraphicsExtractor graphics, Component text, int x, int y, int w, int color) {
        graphics.text(this.font, text, x + Math.max(0, (w - this.font.width(text)) / 2), y, color, false);
    }

    protected void renderPill(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int shadow) {
        AstralFancyButton.renderRoundedGradient(graphics, x, y, w, h, AstralFancyButton.brighten(fill, 12), AstralFancyButton.darken(fill, 20), 0xCC101018, shadow, 5);
    }

    protected void renderRoundedRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int shadow) {
        AstralFancyButton.renderRoundedGradient(graphics, x, y, w, h, AstralFancyButton.brighten(fill, 12), AstralFancyButton.darken(fill, 20), 0xCC101018, shadow, 5);
    }

    protected void renderVerticalScrollbar(GuiGraphicsExtractor graphics, int x, int y, int h, float scroll, float maxScroll) {
        if (maxScroll <= 0.5F) return;
        graphics.fill(x, y, x + 3, y + h, 0x5533333A);
        int thumbH = Math.max(18, (int) (h * (h / (h + maxScroll))));
        int thumbY = y + Math.round((h - thumbH) * (scroll / maxScroll));
        graphics.fill(x, thumbY, x + 3, thumbY + thumbH, 0xCCFFFFFF);
    }

    protected boolean handleMainTabClick(CharacterLayout layout, double mouseX, double mouseY) {
        int x = layout.mainTabX;
        for (MainTab value : MainTab.values()) {
            if (this.isInside(mouseX, mouseY, x, layout.mainTabY, layout.mainTabW, layout.mainTabH)) {
                this.mainTab = value;
                this.bodyScroll = 0.0F;
                this.skinScroll = 0.0F;
                return true;
            }
            x += layout.mainTabW + TAB_GAP;
        }

        return false;
    }

    protected boolean handleArchiveTabClick(CharacterLayout layout, double mouseX, double mouseY) {
        int x = layout.subTabX;
        for (ArchiveTab value : ArchiveTab.values()) {
            if (this.isInside(mouseX, mouseY, x, layout.subTabY, layout.subTabW, layout.subTabH)) {
                this.archiveTab = value;
                this.bodyScroll = 0.0F;
                return true;
            }
            x += layout.subTabW + TAB_GAP;
        }

        return false;
    }

    protected boolean handleCharacterGridClick(CharacterLayout layout, double mouseX, double mouseY) {
        int columns = this.characterGridColumns(layout);
        int cardW = layout.characterCardW;
        int cardH = layout.characterCardH;
        for (int i = 0; i < this.characters.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int x = layout.gridX + column * (cardW + GRID_GAP);
            int y = layout.gridY + row * (cardH + GRID_GAP) - Math.round(this.characterScroll);
            if (this.isInside(mouseX, mouseY, x, y, cardW, cardH)) {
                CharacterDefinition definition = this.characters.get(i);
                this.selectedCharacterId = definition.id();
                this.selectedSkinId = definition.skins().isEmpty() ? "default" : definition.skins().get(0).id();
                this.bodyScroll = 0.0F;
                this.skinScroll = 0.0F;
                ClientPacketDistributor.sendToServer(new CharacterSelectionPayload(this.selectedCharacterId.toString()));
                return true;
            }
        }

        return false;
    }

    protected boolean handleSkinClick(CharacterLayout layout, double mouseX, double mouseY) {
        CharacterDefinition definition = this.selectedCharacter();
        int columns = this.skinColumns(layout);
        int cardW = layout.skinCardW;
        int cardH = layout.skinCardH;
        for (int i = 0; i < definition.skins().size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int x = layout.bodyX + 14 + column * (cardW + GRID_GAP);
            int y = layout.bodyY + 16 + row * (cardH + GRID_GAP) - Math.round(this.skinScroll);
            if (this.isInside(mouseX, mouseY, x, y, cardW, cardH)) {
                this.selectedSkinId = definition.skins().get(i).id();
                ClientPacketDistributor.sendToServer(new CharacterSkinSelectionPayload(definition.id().toString(), this.selectedSkinId));
                return true;
            }
        }

        return false;
    }

    protected void handleBack() {
        if (this.mode == ScreenMode.DETAIL) {
            this.mode = ScreenMode.LIST;
            this.bodyScroll = 0.0F;
            this.skinScroll = 0.0F;
            return;
        }

        this.onClose();
    }

    protected boolean isOverPreview(CharacterLayout layout, double mouseX, double mouseY) {
        return this.isInside(mouseX, mouseY, layout.previewX, layout.previewY, layout.previewW, layout.previewH);
    }

    protected boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    protected void resetPreviewRotation() {
        this.previewYaw = 205.0F;
        this.previewPitch = 8.0F;
        this.previewRoll = 0.0F;
        this.previewZoom = 1.0F;
    }

    protected CharacterDefinition selectedCharacter() {
        for (CharacterDefinition definition : this.characters) {
            if (definition.id().equals(this.selectedCharacterId)) {
                return definition;
            }
        }

        return this.characters.getFirst();
    }

    protected int drawHeader(GuiGraphicsExtractor graphics, Component component, int x, int y, int color, int maxWidth) {
        graphics.fill(x - 6, y - 2, x + maxWidth, y + 12, 0xAA000000);
        graphics.text(this.font, this.ellipsize(component, maxWidth - 6), x, y, color, false);
        return y + 16;
    }

    protected int drawLine(GuiGraphicsExtractor graphics, Component component, int x, int y, int color, int maxWidth) {
        graphics.text(this.font, this.ellipsize(component, maxWidth), x, y, color, false);
        return y + 12;
    }

    protected int drawWrapped(GuiGraphicsExtractor graphics, Component component, int x, int y, int color, int maxWidth) {
        for (String line : this.wrap(component.getString(), maxWidth)) {
            graphics.text(this.font, Component.literal(line), x, y, color, false);
            y += 11;
        }

        return y + 4;
    }

    protected List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                lines.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(character);
            if (this.font.width(current.toString()) >= maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    protected Component ellipsize(Component input, int maxWidth) {
        String text = input.getString();
        if (this.font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (this.font.width(out.toString()) + this.font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }

        return Component.literal(out + suffix);
    }

    protected void renderEntityModel(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, float yaw, float pitch, float roll, float scaleMultiplier) {
        EntityRenderState renderState = this.extractEntityRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 0.0F;
            livingState.yRot = 0.0F;
            livingState.xRot = 0.0F;
            livingState.scale = 1.0F;
        }

        float boxWidth = Math.max(0.35F, renderState.boundingBoxWidth);
        float boxHeight = Math.max(1.2F, renderState.boundingBoxHeight);
        float viewWidth = Math.max(1.0F, x1 - x0);
        float viewHeight = Math.max(1.0F, y1 - y0);
        float scale = Math.min(viewWidth / (boxWidth * 1.35F), viewHeight / (boxHeight * 1.10F)) * scaleMultiplier;
        scale = Mth.clamp(scale, 8.0F, 94.0F);
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.toRadians(180.0F))
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .rotateZ((float) Math.toRadians(roll));
        Vector3f translation = new Vector3f(0.0F, boxHeight * 0.48F, 0.0F);
        graphics.entity(renderState, scale, translation, rotation, null, x0, y0, x1, y1);
    }

    protected EntityRenderState extractEntityRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }

    protected LivingEntity previewEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return minecraft.player;
        }

        if (this.previewEntity == null || this.previewLevelKey != minecraft.level) {
            Zombie zombie = new Zombie(EntityType.ZOMBIE, minecraft.level);
            zombie.setNoAi(true);
            zombie.setSilent(true);
            zombie.yBodyRot = 0.0F;
            zombie.yHeadRot = 0.0F;
            zombie.setYRot(0.0F);
            zombie.setXRot(0.0F);
            this.previewEntity = zombie;
            this.previewLevelKey = minecraft.level;
        }

        return this.previewEntity;
    }

    protected CharacterLayout layout() {
        int margin = MIN_SCREEN_MARGIN;
        int availableWidth = Math.max(260, this.width - margin * 2);
        int availableHeight = Math.max(120, this.height - margin * 2);
        int totalW = Math.min(MAX_TOTAL_WIDTH, availableWidth);
        int totalH = Mth.clamp(availableHeight, Math.min(MIN_TOTAL_HEIGHT, availableHeight), MAX_TOTAL_HEIGHT);
        int leftW = Mth.clamp((int) (totalW * 0.26F), MIN_LEFT_WIDTH, MAX_LEFT_WIDTH);
        int rightW = totalW - PANEL_GAP - leftW;
        if (rightW < MIN_RIGHT_WIDTH) {
            leftW = Math.max(92, totalW - PANEL_GAP - MIN_RIGHT_WIDTH);
            rightW = totalW - PANEL_GAP - leftW;
        }

        int realW = leftW + PANEL_GAP + rightW;
        int leftX = Math.max(margin, (this.width - realW) / 2);
        int topY = Math.max(margin, (this.height - totalH) / 2);
        int rightX = leftX + leftW + PANEL_GAP;
        int backX = leftX + 8;
        int backY = topY + 6;
        int backW = Math.min(58, leftW - 16);
        int backH = 20;
        int previewX = leftX + 10;
        int previewY = topY + 38;
        int previewW = leftW - 20;
        int previewH = Math.max(96, totalH - 96);
        int detailButtonW = Math.clamp(rightW / 4, 86, 120);
        int detailButtonH = 26;
        int detailButtonX = rightX + rightW - detailButtonW - 14;
        int detailButtonY = topY + 6;
        int gridX = rightX + 14;
        int gridY = topY + 48;
        int gridW = rightW - 28;
        int gridH = totalH - 62;
        int characterCardW = Mth.clamp((gridW - GRID_GAP * 3) / 4, 58, 84);
        int characterCardH = Mth.clamp((int) (characterCardW * 1.28F), 74, 108);
        int mainTabX = rightX + 14;
        int mainTabY = topY + 44;
        int mainTabH = 30;
        int mainTabW = Math.max(50, (rightW - 28 - TAB_GAP * (MainTab.values().length - 1)) / MainTab.values().length);
        int subTabX = rightX + 16;
        int subTabY = mainTabY + mainTabH + 8;
        int subTabH = 24;
        int subTabW = Math.max(48, (rightW - 32 - TAB_GAP * (ArchiveTab.values().length - 1)) / ArchiveTab.values().length);
        int bodyX = rightX + 14;
        int bodyY = this.mode == ScreenMode.DETAIL && this.mainTab == MainTab.ARCHIVE ? subTabY + subTabH + 8 : mainTabY + mainTabH + 10;
        int bodyW = rightW - 28;
        int bodyH = topY + totalH - bodyY - 12;
        int skinCardW = Mth.clamp((bodyW - 32) / 3, 72, 104);
        int skinCardH = Mth.clamp((int) (skinCardW * 1.62F), 112, 168);
        float previewEntityScale = Mth.clamp(leftW / 168.0F, 0.68F, 1.08F);
        float cardEntityScale = Mth.clamp(characterCardW / 75.0F, 0.50F, 0.72F);
        float skinEntityScale = Mth.clamp(skinCardW / 84.0F, 0.56F, 0.80F);
        return new CharacterLayout(leftX, rightX, topY, leftW, rightW, totalH, backX, backY, backW, backH,
                previewX, previewY, previewW, previewH, detailButtonX, detailButtonY, detailButtonW, detailButtonH,
                gridX, gridY, gridW, gridH, characterCardW, characterCardH, mainTabX, mainTabY, mainTabW, mainTabH,
                subTabX, subTabY, subTabW, subTabH, bodyX, bodyY, bodyW, bodyH, skinCardW, skinCardH,
                previewEntityScale, cardEntityScale, skinEntityScale);
    }

    protected int characterGridColumns(CharacterLayout layout) {
        return Math.max(1, (layout.gridW + GRID_GAP) / (layout.characterCardW + GRID_GAP));
    }

    protected int skinColumns(CharacterLayout layout) {
        return Math.max(1, (layout.bodyW - 28 + GRID_GAP) / (layout.skinCardW + GRID_GAP));
    }

    protected float maxCharacterScroll(CharacterLayout layout) {
        int columns = this.characterGridColumns(layout);
        int rows = (this.characters.size() + columns - 1) / columns;
        int content = rows * (layout.characterCardH + GRID_GAP) - GRID_GAP;
        return Math.max(0.0F, content - layout.gridH);
    }

    protected float maxSkinScroll(CharacterLayout layout) {
        CharacterDefinition definition = this.selectedCharacter();
        int columns = this.skinColumns(layout);
        int rows = (definition.skins().size() + columns - 1) / columns;
        int listHeight = Math.max(10, layout.bodyH - 50);
        int content = rows * (layout.skinCardH + GRID_GAP) - GRID_GAP;
        return Math.max(0.0F, content - listHeight);
    }

    protected float maxBodyScroll(CharacterLayout layout) {
        if (this.mode != ScreenMode.DETAIL || this.mainTab == MainTab.SKINS) return 0.0F;
        int visible = Math.max(10, layout.bodyH - 50);
        int content = this.estimatedBodyContentHeight(layout);
        return content > visible + 14 ? content - visible : 0.0F;
    }

    protected int estimatedBodyContentHeight(CharacterLayout layout) {
        CharacterDefinition definition = this.selectedCharacter();
        int maxWidth = Math.max(40, layout.bodyW - 52);
        int height = 10;
        if (this.mainTab == MainTab.ARCHIVE && this.archiveTab == ArchiveTab.SKILLS) {
            if (definition.skills().isEmpty()) {
                return this.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.empty_skills"), maxWidth) + 12;
            }

            for (CharacterSkillDefinition skill : definition.skills()) {
                height += 16;
                height += this.wrappedHeight(Component.translatable(skill.descriptionKey()), maxWidth - 8) + 10;
                if (skill.cooldown() > 0) {
                    height += 14;
                }
            }
        } else if (this.mainTab == MainTab.ARCHIVE && this.archiveTab == ArchiveTab.PROFILE) {
            if (definition.profileSections().isEmpty()) {
                return this.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.empty_profile"), maxWidth) + 12;
            }

            for (CharacterProfileSection section : definition.profileSections()) {
                if (this.shouldRenderProfileSectionHeader(section)) {
                    height += 16;
                }
                height += this.wrappedHeight(Component.translatable(section.bodyKey()), maxWidth - 8) + 12;
            }
        } else if (this.mainTab == MainTab.ARCHIVE && this.archiveTab == ArchiveTab.LEVEL) {
            height += 52 + this.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.level_placeholder"), maxWidth - 8);
        } else if (this.mainTab == MainTab.ARCHIVE && this.archiveTab == ArchiveTab.POTENTIAL) {
            height += this.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.potential_placeholder"), maxWidth - 8) + 20;
        } else if (this.mainTab == MainTab.CONTRACT) {
            height += this.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.contract_placeholder"), maxWidth - 8) + 20;
        } else if (this.mainTab == MainTab.EMOTES) {
            height += this.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.emotes_placeholder"), maxWidth - 8) + 20;
        }

        return height;
    }

    protected boolean shouldRenderProfileSectionHeader(CharacterProfileSection section) {
        return false;
    }

    protected int wrappedHeight(Component component, int maxWidth) {
        return this.wrap(component.getString(), maxWidth).size() * 11 + 4;
    }

    protected void clampScrolls() {
        CharacterLayout layout = this.layout();
        this.characterScroll = Mth.clamp(this.characterScroll, 0.0F, this.maxCharacterScroll(layout));
        this.bodyScroll = Mth.clamp(this.bodyScroll, 0.0F, this.maxBodyScroll(layout));
        this.skinScroll = Mth.clamp(this.skinScroll, 0.0F, this.maxSkinScroll(layout));
    }

    public enum ScreenMode {
        LIST,
        DETAIL
    }

    public enum MainTab {

        ARCHIVE("gui.astral_craft.character_settings.main.archive"),
        CONTRACT("gui.astral_craft.character_settings.main.contract"),
        SKINS("gui.astral_craft.character_settings.main.skins"),
        EMOTES("gui.astral_craft.character_settings.main.emotes");

        public String translationKey;

        MainTab(String translationKey) {
            this.translationKey = translationKey;
        }

    }

    public enum ArchiveTab {

        SKILLS("gui.astral_craft.character_settings.archive.skills", 0xFFFFF2A0),
        LEVEL("gui.astral_craft.character_settings.archive.level", 0xFF8CFF20),
        POTENTIAL("gui.astral_craft.character_settings.archive.potential", 0xFFDFA0FF),
        PROFILE("gui.astral_craft.character_settings.archive.profile", 0xFFFFA0FF);

        public String translationKey;
        public int color;

        ArchiveTab(String translationKey, int color) {
            this.translationKey = translationKey;
            this.color = color;
        }

        public String titleKey() {
            return this.translationKey;
        }

        public int headerColor() {
            return this.color;
        }

    }

    public static class CharacterLayout {
        public int leftX;
        public int rightX;
        public int topY;
        public int leftW;
        public int rightW;
        public int totalH;
        public int backX;
        public int backY;
        public int backW;
        public int backH;
        public int previewX;
        public int previewY;
        public int previewW;
        public int previewH;
        public int detailButtonX;
        public int detailButtonY;
        public int detailButtonW;
        public int detailButtonH;
        public int gridX;
        public int gridY;
        public int gridW;
        public int gridH;
        public int characterCardW;
        public int characterCardH;
        public int mainTabX;
        public int mainTabY;
        public int mainTabW;
        public int mainTabH;
        public int subTabX;
        public int subTabY;
        public int subTabW;
        public int subTabH;
        public int bodyX;
        public int bodyY;
        public int bodyW;
        public int bodyH;
        public int skinCardW;
        public int skinCardH;
        public float previewEntityScale;
        public float cardEntityScale;
        public float skinEntityScale;

        public CharacterLayout(int leftX, int rightX, int topY, int leftW, int rightW, int totalH, int backX, int backY, int backW, int backH,
                               int previewX, int previewY, int previewW, int previewH, int detailButtonX, int detailButtonY, int detailButtonW, int detailButtonH,
                               int gridX, int gridY, int gridW, int gridH, int characterCardW, int characterCardH, int mainTabX, int mainTabY, int mainTabW, int mainTabH,
                               int subTabX, int subTabY, int subTabW, int subTabH, int bodyX, int bodyY, int bodyW, int bodyH, int skinCardW, int skinCardH,
                               float previewEntityScale, float cardEntityScale, float skinEntityScale) {
            this.leftX = leftX;
            this.rightX = rightX;
            this.topY = topY;
            this.leftW = leftW;
            this.rightW = rightW;
            this.totalH = totalH;
            this.backX = backX;
            this.backY = backY;
            this.backW = backW;
            this.backH = backH;
            this.previewX = previewX;
            this.previewY = previewY;
            this.previewW = previewW;
            this.previewH = previewH;
            this.detailButtonX = detailButtonX;
            this.detailButtonY = detailButtonY;
            this.detailButtonW = detailButtonW;
            this.detailButtonH = detailButtonH;
            this.gridX = gridX;
            this.gridY = gridY;
            this.gridW = gridW;
            this.gridH = gridH;
            this.characterCardW = characterCardW;
            this.characterCardH = characterCardH;
            this.mainTabX = mainTabX;
            this.mainTabY = mainTabY;
            this.mainTabW = mainTabW;
            this.mainTabH = mainTabH;
            this.subTabX = subTabX;
            this.subTabY = subTabY;
            this.subTabW = subTabW;
            this.subTabH = subTabH;
            this.bodyX = bodyX;
            this.bodyY = bodyY;
            this.bodyW = bodyW;
            this.bodyH = bodyH;
            this.skinCardW = skinCardW;
            this.skinCardH = skinCardH;
            this.previewEntityScale = previewEntityScale;
            this.cardEntityScale = cardEntityScale;
            this.skinEntityScale = skinEntityScale;
        }
    }

}