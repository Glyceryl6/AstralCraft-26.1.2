package com.astral_craft.client.gui.character;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralVerticalScrollbar;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.c2s.ExhibitionCharacterConfigPayload;
import com.astral_craft.common.network.s2c.OpenExhibitionCharacterConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class ExhibitionCharacterConfigScreen extends Screen {

    private static final int CHARACTER_CARD_W = 46;
    private static final int CHARACTER_CARD_H = 44;
    private static final int SKIN_CARD_W = 80;
    private static final int SKIN_CARD_H = 46;
    private static final int TEXT_LINE_H = 10;
    private static final int GAP = 4;
    private final int entityId;
    private final List<CharacterDefinition> characters;
    private final Identifier initialCharacterId;
    private final String initialSkinId;
    private final float initialYaw;
    private final float initialScale;
    private final String initialCustomName;
    private final boolean initialShowName;
    private final String initialSpeechText;
    private final boolean initialCustomSkinEnabled;
    private final boolean initialCustomSkinPlayer;
    private final String initialCustomSkinSource;
    private Identifier selectedCharacterId;
    private String selectedSkinId;
    private float yaw;
    private float scale;
    private String customName;
    private boolean showName;
    private String speechText;
    private boolean customSkinEnabled;
    private boolean customSkinPlayer;
    private String customSkinSource;
    private ConfigTab tab = ConfigTab.CHARACTER;
    private float contentScroll;
    private float skinScroll;
    private boolean draggingContentScrollbar;
    private boolean draggingSkinScrollbar;
    private boolean draggingWorldPreview;
    private double lastDragX;
    private boolean submitted;
    private boolean syncingFields;
    private int customSkinPreviewDelay;
    private EditBox customNameBox;
    private EditBox yawBox;
    private EditBox scaleBox;
    private EditBox speechBox;
    private EditBox customSkinBox;
    private ExhibitionCharacterEntity livePreviewEntity;

    public ExhibitionCharacterConfigScreen(OpenExhibitionCharacterConfigPayload payload) {
        super(Component.translatable("gui.astral_craft.exhibition_character.title"));
        this.entityId = payload.entityId();
        this.characters = payload.characters().isEmpty() ? List.of(CharacterDefinition.builtinDefault()) : List.copyOf(payload.characters());
        ClientCharacterDefinitionCache.INSTANCE.replace(this.characters);
        this.initialCharacterId = this.hasCharacter(payload.characterId()) ? payload.characterId() : this.characters.getFirst().id();
        this.initialSkinId = this.validSkin(this.character(this.initialCharacterId), payload.skinId());
        this.initialYaw = ExhibitionCharacterEntity.normalizeYaw(payload.yaw());
        this.initialScale = Mth.clamp(payload.scale(), ExhibitionCharacterEntity.MIN_SCALE, ExhibitionCharacterEntity.MAX_SCALE);
        this.initialCustomName = payload.customName();
        this.initialShowName = payload.showName();
        this.initialSpeechText = payload.speechText();
        this.initialCustomSkinEnabled = payload.customSkinEnabled();
        this.initialCustomSkinPlayer = payload.customSkinPlayer();
        this.initialCustomSkinSource = payload.customSkinSource();
        this.selectedCharacterId = this.initialCharacterId;
        this.selectedSkinId = this.initialSkinId;
        this.yaw = this.initialYaw;
        this.scale = this.initialScale;
        this.customName = this.initialCustomName;
        this.showName = this.initialShowName;
        this.speechText = this.initialSpeechText;
        this.customSkinEnabled = this.initialCustomSkinEnabled;
        this.customSkinPlayer = this.initialCustomSkinPlayer;
        this.customSkinSource = this.initialCustomSkinSource;
    }

    public static void open(OpenExhibitionCharacterConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ExhibitionCharacterConfigScreen(payload)));
    }

    @Override
    protected void init() {
        Layout layout = this.layout();
        this.customNameBox = this.addRenderableWidget(new EditBox(this.font, layout.contentX(), layout.displayNameBoxY(this.contentScroll), layout.contentW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.custom_name")));
        this.customNameBox.setMaxLength(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH);
        this.customNameBox.setHint(Component.translatable("gui.astral_craft.exhibition_character.custom_name_hint"));
        this.yawBox = this.addRenderableWidget(new EditBox(this.font, layout.contentX(), layout.displayNumberBoxY(this.contentScroll), layout.numberBoxW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.yaw")));
        this.yawBox.setMaxLength(10);
        this.yawBox.setFilter(ExhibitionCharacterConfigScreen::signedDecimalOrEmpty);
        this.scaleBox = this.addRenderableWidget(new EditBox(this.font, layout.scaleBoxX(), layout.displayNumberBoxY(this.contentScroll), layout.numberBoxW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.scale")));
        this.scaleBox.setMaxLength(8);
        this.scaleBox.setFilter(ExhibitionCharacterConfigScreen::decimalOrEmpty);
        this.speechBox = this.addRenderableWidget(new EditBox(this.font, layout.contentX(), layout.displaySpeechBoxY(this.contentScroll), layout.contentW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.speech")));
        this.speechBox.setMaxLength(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH);
        this.speechBox.setHint(Component.translatable("gui.astral_craft.exhibition_character.speech_hint"));
        this.customSkinBox = this.addRenderableWidget(new EditBox(this.font, layout.contentX(), this.customSkinContentLayout(layout, this.contentScroll).sourceBoxY(), layout.contentW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.custom_skin_source")));
        this.customSkinBox.setMaxLength(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH);
        this.customSkinBox.setHint(Component.translatable("gui.astral_craft.exhibition_character.custom_skin_source_hint"));
        this.syncFields();
        this.customNameBox.setResponder(value -> {
            if (this.syncingFields) return;
            this.customName = value;
            this.applyLivePreview();
        });
        this.yawBox.setResponder(this::yawChanged);
        this.scaleBox.setResponder(this::scaleChanged);
        this.speechBox.setResponder(value -> {
            if (this.syncingFields) return;
            this.speechText = value;
            this.applyLivePreview();
        });
        this.customSkinBox.setResponder(value -> {
            if (this.syncingFields) return;
            this.customSkinSource = value;
            this.customSkinPreviewDelay = this.customSkinPlayer ? 10 : 0;
            if (this.customSkinPreviewDelay == 0) this.applyLivePreview();
        });
        this.updateWidgetLayout(layout);
        this.applyLivePreview();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.customSkinPreviewDelay > 0 && --this.customSkinPreviewDelay == 0) this.applyLivePreview();
        if (this.livePreviewEntity == null || this.livePreviewEntity.isRemoved()) this.applyLivePreview();
    }

    @Override
    public void removed() {
        AstralFancyButton.setHandCursor(false);
        if (!this.submitted) this.restoreLivePreview();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        this.contentScroll = Mth.clamp(this.contentScroll, 0.0F, this.maxContentScroll(layout));
        this.updateWidgetLayout(layout);
        AstralFancyButton.renderOutlinedBox(graphics, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(),
                0x9E151723, 0xD9545B70, 0xA8101018, 1, 2);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelY() + 3, 0xDDE83CA8);
        graphics.centeredText(this.font, this.title, layout.titleCenterX(), layout.panelY() + 10, 0xFFFFFFFF);
        this.renderCloseButton(graphics, layout, mouseX, mouseY);
        this.renderTabs(graphics, layout, mouseX, mouseY);
        this.renderScrollableContent(graphics, layout, mouseX, mouseY);
        this.renderActions(graphics, layout, mouseX, mouseY);
        if (this.tab == ConfigTab.CHARACTER) this.renderSkinPanel(graphics, layout, mouseX, mouseY);
        this.renderWorldPreviewHint(graphics, layout);
        AstralFancyButton.setHandCursor(this.hoveredClickable(layout, mouseX, mouseY));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted) return true;
        Layout layout = this.layout();
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.closeButtonX(), layout.closeButtonY(), layout.closeButtonW(), layout.closeButtonH())) {
            this.onClose();
            return true;
        }
        if (event.button() == 0 && AstralVerticalScrollbar.contains(mouseX, mouseY, layout.scrollbarX(), layout.contentY(), layout.contentH(), this.maxContentScroll(layout))) {
            this.draggingContentScrollbar = true;
            this.updateContentScrollFromMouse(layout, mouseY);
            return true;
        }
        if (event.button() == 0 && this.tab == ConfigTab.CHARACTER
                && AstralVerticalScrollbar.contains(mouseX, mouseY, layout.skinScrollbarX(), layout.skinContentY(), layout.skinContentH(), this.maxSkinScroll(layout))) {
            this.draggingSkinScrollbar = true;
            this.updateSkinScrollFromMouse(layout, mouseY);
            return true;
        }
        if (event.button() == 0 && this.handleTabClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.tab == ConfigTab.CHARACTER && this.handleSkinPanelClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.tab == ConfigTab.CHARACTER && this.handleCharacterContentClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.tab == ConfigTab.CUSTOM_SKIN && this.handleCustomSkinContentClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.tab == ConfigTab.DISPLAY && this.handleDisplayContentClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.handleActionClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.isWorldPreviewArea(layout, mouseX, mouseY)) {
            this.draggingWorldPreview = true;
            this.lastDragX = mouseX;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        Layout layout = this.layout();
        if (this.draggingContentScrollbar) {
            this.updateContentScrollFromMouse(layout, event.y());
            return true;
        }
        if (this.draggingSkinScrollbar) {
            this.updateSkinScrollFromMouse(layout, event.y());
            return true;
        }
        if (this.draggingWorldPreview) {
            float delta = (float) (event.x() - this.lastDragX);
            this.lastDragX = event.x();
            this.setYaw(this.yaw - delta * 0.82F, true);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingContentScrollbar) {
            this.draggingContentScrollbar = false;
            return true;
        }
        if (event.button() == 0 && this.draggingSkinScrollbar) {
            this.draggingSkinScrollbar = false;
            return true;
        }
        if (event.button() == 0 && this.draggingWorldPreview) {
            this.draggingWorldPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        if (this.tab == ConfigTab.CHARACTER && this.isInside(mouseX, mouseY, layout.skinPanelX(), layout.skinPanelY(), layout.skinPanelW(), layout.skinPanelH())) {
            float maxScroll = this.maxSkinScroll(layout);
            if (maxScroll > 0.5F) this.skinScroll = Mth.clamp(this.skinScroll - (float) deltaY * 28.0F, 0.0F, maxScroll);
            return true;
        }
        if (this.isWorldPreviewArea(layout, mouseX, mouseY)) {
            this.setScale(this.scale + (float) deltaY * 0.1F, true);
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH())) {
            float maxScroll = this.maxContentScroll(layout);
            if (maxScroll > 0.5F) {
                this.contentScroll = Mth.clamp(this.contentScroll - (float) deltaY * 28.0F, 0.0F, maxScroll);
                this.updateWidgetLayout(layout);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        this.renderTab(graphics, layout, ConfigTab.CHARACTER, layout.characterTabX(), mouseX, mouseY,
                Component.translatable("gui.astral_craft.exhibition_character.tab_character"));
        this.renderTab(graphics, layout, ConfigTab.CUSTOM_SKIN, layout.customSkinTabX(), mouseX, mouseY,
                Component.translatable("gui.astral_craft.exhibition_character.tab_custom_skin"));
        this.renderTab(graphics, layout, ConfigTab.DISPLAY, layout.displayTabX(), mouseX, mouseY,
                Component.translatable("gui.astral_craft.exhibition_character.tab_display"));
    }

    private void renderTab(GuiGraphicsExtractor graphics, Layout layout, ConfigTab tab, int x, int mouseX, int mouseY, Component text) {
        boolean selected = this.tab == tab;
        boolean hovered = this.isInside(mouseX, mouseY, x, layout.tabY(), layout.tabW(), layout.tabH());
        float textScale = Math.clamp((layout.tabW() - 8.0F) / Math.max(1.0F, this.font.width(text)), 0.68F, 1.0F);
        AstralFancyButton.renderTab(graphics, this.font, text, x, layout.tabY(), layout.tabW(), layout.tabH(), selected, hovered,
                (selected ? AstralFancyButton.selectedButtonStyle() : AstralFancyButton.pinkButtonStyle()).withTextScale(textScale));
    }

    private void renderCloseButton(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean hovered = this.isInside(mouseX, mouseY, layout.closeButtonX(), layout.closeButtonY(), layout.closeButtonW(), layout.closeButtonH());
        AstralFancyButton.renderButton(graphics, this.font, Component.literal("X"),
                layout.closeButtonX(), layout.closeButtonY(), layout.closeButtonW(), layout.closeButtonH(), false, hovered, AstralFancyButton.pinkButtonStyle());
    }

    private void renderScrollableContent(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.enableScissor(layout.contentX(), layout.contentY(), layout.contentRight(), layout.contentBottom());
        if (this.tab == ConfigTab.CHARACTER) this.renderCharacterContent(graphics, layout, mouseX, mouseY);
        else if (this.tab == ConfigTab.CUSTOM_SKIN) this.renderCustomSkinContent(graphics, layout, mouseX, mouseY);
        else this.renderDisplayContent(graphics, layout, mouseX, mouseY);
        graphics.disableScissor();
        AstralVerticalScrollbar.render(graphics, layout.scrollbarX(), layout.contentY(), layout.contentH(), this.contentScroll, this.maxContentScroll(layout));
    }

    private void renderCharacterContent(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int scroll = Math.round(this.contentScroll);
        int characterLabelY = layout.contentY() + 2 - scroll;
        int characterStartY = layout.contentY() + 16 - scroll;
        int columns = this.characterColumns(layout);
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.character"), layout.contentX(), characterLabelY, 0xFFD7E4F2);
        for (int index = 0; index < this.characters.size(); index++) {
            CharacterDefinition definition = this.characters.get(index);
            int row = index / columns;
            int x = layout.contentX() + index % columns * (CHARACTER_CARD_W + GAP);
            int cardY = characterStartY + row * (CHARACTER_CARD_H + GAP);
            if (cardY + CHARACTER_CARD_H < layout.contentY() || cardY > layout.contentBottom()) continue;
            boolean selected = definition.id().equals(this.selectedCharacterId);
            boolean hovered = this.isInside(mouseX, mouseY, x, cardY, CHARACTER_CARD_W, CHARACTER_CARD_H);
            AstralFancyButton.renderIconFrame(graphics, x, cardY, CHARACTER_CARD_W, CHARACTER_CARD_H, selected, hovered);
            String skin = selected ? this.selectedSkinId : this.validSkin(definition, "");
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, definition.id(), skin, x + 12, cardY + 3, 22, 255, false);
            String name = this.font.plainSubstrByWidth(Component.translatable(definition.getDescriptionId()).getString(), CHARACTER_CARD_W - 4);
            graphics.centeredText(this.font, name, x + CHARACTER_CARD_W / 2, cardY + 31, 0xFFFFFFFF);
        }
    }

    private void renderSkinPanel(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        this.skinScroll = Mth.clamp(this.skinScroll, 0.0F, this.maxSkinScroll(layout));
        AstralFancyButton.renderOutlinedBox(graphics, layout.skinPanelX(), layout.skinPanelY(), layout.skinPanelW(), layout.skinPanelH(),
                0x76151723, 0xB8545B70, 0x60101018, 1, 2);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.exhibition_character.skin"),
                layout.skinPanelX() + layout.skinPanelW() / 2, layout.skinPanelY() + 8, 0xFFFFFFFF);
        graphics.enableScissor(layout.skinContentX(), layout.skinContentY(), layout.skinContentRight(), layout.skinContentBottom());
        List<CharacterSkinDefinition> skins = this.selectedCharacter().skins();
        int columns = this.skinColumns(layout);
        int gridW = columns * SKIN_CARD_W + Math.max(0, columns - 1) * GAP;
        int startX = layout.skinContentX() + Math.max(0, (layout.skinContentW() - gridW) / 2);
        int startY = layout.skinContentY() - Math.round(this.skinScroll);
        for (int index = 0; index < skins.size(); index++) {
            CharacterSkinDefinition skin = skins.get(index);
            int x = startX + index % columns * (SKIN_CARD_W + GAP);
            int cardY = startY + index / columns * (SKIN_CARD_H + GAP);
            if (cardY + SKIN_CARD_H < layout.skinContentY() || cardY > layout.skinContentBottom()) continue;
            boolean selected = skin.id().equals(this.selectedSkinId);
            boolean hovered = this.isInside(mouseX, mouseY, x, cardY, SKIN_CARD_W, SKIN_CARD_H);
            AstralFancyButton.renderIconFrame(graphics, x, cardY, SKIN_CARD_W, SKIN_CARD_H, selected, hovered);
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, this.selectedCharacterId, skin.id(), x + 5, cardY + 12, 22, 255, false);
            List<FormattedCharSequence> lines = this.font.split(Component.translatable(skin.nameKey()), SKIN_CARD_W - 35);
            int textY = cardY + Math.max(4, (SKIN_CARD_H - Math.max(1, lines.size()) * TEXT_LINE_H) / 2);
            for (int line = 0; line < lines.size(); line++) {
                graphics.text(this.font, lines.get(line), x + 31, textY + line * TEXT_LINE_H, 0xFFFFFFFF, false);
            }
        }
        graphics.disableScissor();
        AstralVerticalScrollbar.render(graphics, layout.skinScrollbarX(), layout.skinContentY(), layout.skinContentH(), this.skinScroll, this.maxSkinScroll(layout));
    }

    private void renderCustomSkinContent(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        CustomSkinContentLayout customLayout = this.customSkinContentLayout(layout, this.contentScroll);
        this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_description"),
                layout.contentX(), customLayout.descriptionY(), layout.contentW(), 0xFFAEB8CB);
        boolean enabledHover = this.isInside(mouseX, mouseY, layout.contentX(), customLayout.enabledButtonY(), layout.contentW(), 22);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable(this.customSkinEnabled
                        ? "gui.astral_craft.exhibition_character.custom_skin_enabled" : "gui.astral_craft.exhibition_character.custom_skin_disabled"),
                layout.contentX(), customLayout.enabledButtonY(), layout.contentW(), 22, this.customSkinEnabled, enabledHover,
                AstralFancyButton.ButtonStyle.button(0xFFB23B8C));
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_type"),
                layout.contentX(), customLayout.typeLabelY(), 0xFFD7E4F2);
        int sourceButtonW = (layout.contentW() - GAP) / 2;
        boolean playerHover = this.isInside(mouseX, mouseY, layout.contentX(), customLayout.typeButtonY(), sourceButtonW, 22);
        boolean resourceHover = this.isInside(mouseX, mouseY, layout.contentX() + sourceButtonW + GAP, customLayout.typeButtonY(), sourceButtonW, 22);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_player"),
                layout.contentX(), customLayout.typeButtonY(), sourceButtonW, 22, this.customSkinPlayer, playerHover, AstralFancyButton.ButtonStyle.button(0xFF5664B7));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_resource"),
                layout.contentX() + sourceButtonW + GAP, customLayout.typeButtonY(), sourceButtonW, 22, !this.customSkinPlayer, resourceHover,
                AstralFancyButton.ButtonStyle.button(0xFF5664B7));
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_source"),
                layout.contentX(), customLayout.sourceLabelY(), 0xFFD7E4F2);
        Component hint = Component.translatable(this.customSkinPlayer
                ? "gui.astral_craft.exhibition_character.custom_skin_player_hint" : "gui.astral_craft.exhibition_character.custom_skin_resource_hint");
        this.renderWrappedText(graphics, hint, layout.contentX(), customLayout.hintY(), layout.contentW(), 0xFFAEB8CB);
        if (this.customSkinEnabled && !this.validCustomSkinInput()) {
            this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_invalid"),
                    layout.contentX(), customLayout.statusY(), layout.contentW(), 0xFFFF8C9A);
        } else if (this.customSkinEnabled) {
            this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.exhibition_character.custom_skin_priority"),
                    layout.contentX(), customLayout.statusY(), layout.contentW(), 0xFF9FE3B0);
        }
    }

    private void renderDisplayContent(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int scroll = Math.round(this.contentScroll);
        int nameLabelY = layout.contentY() + 2 - scroll;
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.custom_name"), layout.contentX(), nameLabelY, 0xFFD7E4F2);
        int buttonY = layout.displayNameButtonY(this.contentScroll);
        int showButtonW = Math.max(52, (layout.contentW() - GAP) * 2 / 3);
        int clearButtonW = Math.max(36, layout.contentW() - showButtonW - GAP);
        boolean showHover = this.isInside(mouseX, mouseY, layout.contentX(), buttonY, showButtonW, 20);
        boolean clearHover = this.isInside(mouseX, mouseY, layout.contentX() + showButtonW + GAP, buttonY, clearButtonW, 20);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable(this.showName
                        ? "gui.astral_craft.exhibition_character.name_shown" : "gui.astral_craft.exhibition_character.name_hidden"),
                layout.contentX(), buttonY, showButtonW, 20, this.showName, showHover, AstralFancyButton.ButtonStyle.button(0xFF5664B7));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.exhibition_character.clear_name"),
                layout.contentX() + showButtonW + GAP, buttonY, clearButtonW, 20, false, clearHover, AstralFancyButton.ButtonStyle.button(0xFF646477));
        int numberLabelY = layout.displayNumberLabelY(this.contentScroll);
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.yaw"), layout.contentX(), numberLabelY, 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.scale"), layout.scaleBoxX(), numberLabelY, 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.speech"), layout.contentX(), layout.displaySpeechLabelY(this.contentScroll), 0xFFD7E4F2);
    }

    private void renderActions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean valid = this.validInput();
        this.renderAction(graphics, layout, mouseX, mouseY, 0, Component.translatable("gui.astral_craft.confirm"), !valid || this.submitted, 0xFF4F9D69);
        this.renderAction(graphics, layout, mouseX, mouseY, 1, Component.translatable("gui.astral_craft.exhibition_character.reset"), this.submitted, 0xFF5664B7);
        this.renderAction(graphics, layout, mouseX, mouseY, 2, Component.translatable("gui.astral_craft.cancel"), this.submitted, 0xFF646477);
        this.renderAction(graphics, layout, mouseX, mouseY, 3, Component.translatable("gui.astral_craft.exhibition_character.remove"), this.submitted, 0xFF9B5360);
    }

    private void renderAction(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY, int index, Component text, boolean disabled, int color) {
        int x = layout.actionX(index);
        int y = layout.actionY(index);
        boolean hovered = !disabled && this.isInside(mouseX, mouseY, x, y, layout.actionButtonW(), layout.actionButtonH());
        AstralFancyButton.ButtonStyle style = disabled ? AstralFancyButton.disabledButtonStyle() : AstralFancyButton.ButtonStyle.button(color);
        AstralFancyButton.renderButton(graphics, this.font, text, x, y, layout.actionButtonW(), layout.actionButtonH(), false, hovered, style);
    }

    private void renderWorldPreviewHint(GuiGraphicsExtractor graphics, Layout layout) {
        int previewRight = this.worldPreviewRight(layout);
        int previewW = previewRight - layout.worldPreviewX();
        if (previewW < 100) return;
        int center = layout.worldPreviewX() + previewW / 2;
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.exhibition_character.world_preview_hint"), center, 8, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.exhibition_character.preview_values", this.format(this.yaw), this.format(this.scale)), center, 19, 0xFFD7E4F2);
    }

    private boolean handleTabClick(Layout layout, double mouseX, double mouseY) {
        if (this.isInside(mouseX, mouseY, layout.characterTabX(), layout.tabY(), layout.tabW(), layout.tabH())) {
            this.tab = ConfigTab.CHARACTER;
        } else if (this.isInside(mouseX, mouseY, layout.customSkinTabX(), layout.tabY(), layout.tabW(), layout.tabH())) {
            this.tab = ConfigTab.CUSTOM_SKIN;
        } else if (this.isInside(mouseX, mouseY, layout.displayTabX(), layout.tabY(), layout.tabW(), layout.tabH())) {
            this.tab = ConfigTab.DISPLAY;
        } else {
            return false;
        }
        this.contentScroll = 0.0F;
        this.updateWidgetLayout(layout);
        return true;
    }

    private boolean handleSkinPanelClick(Layout layout, double mouseX, double mouseY) {
        if (!this.isInside(mouseX, mouseY, layout.skinContentX(), layout.skinContentY(), layout.skinContentW(), layout.skinContentH())) return false;
        List<CharacterSkinDefinition> skins = this.selectedCharacter().skins();
        int columns = this.skinColumns(layout);
        int gridW = columns * SKIN_CARD_W + Math.max(0, columns - 1) * GAP;
        int startX = layout.skinContentX() + Math.max(0, (layout.skinContentW() - gridW) / 2);
        int startY = layout.skinContentY() - Math.round(this.skinScroll);
        for (int index = 0; index < skins.size(); index++) {
            int x = startX + index % columns * (SKIN_CARD_W + GAP);
            int cardY = startY + index / columns * (SKIN_CARD_H + GAP);
            if (!this.isInside(mouseX, mouseY, x, cardY, SKIN_CARD_W, SKIN_CARD_H)) continue;
            this.selectedSkinId = skins.get(index).id();
            this.applyLivePreview();
            return true;
        }
        return true;
    }

    private boolean handleCharacterContentClick(Layout layout, double mouseX, double mouseY) {
        if (!this.isInside(mouseX, mouseY, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH())) return false;
        int scroll = Math.round(this.contentScroll);
        int characterStartY = layout.contentY() + 16 - scroll;
        int columns = this.characterColumns(layout);
        for (int index = 0; index < this.characters.size(); index++) {
            int row = index / columns;
            int x = layout.contentX() + index % columns * (CHARACTER_CARD_W + GAP);
            int cardY = characterStartY + row * (CHARACTER_CARD_H + GAP);
            if (!this.isInside(mouseX, mouseY, x, cardY, CHARACTER_CARD_W, CHARACTER_CARD_H)) continue;
            CharacterDefinition definition = this.characters.get(index);
            this.selectedCharacterId = definition.id();
            this.selectedSkinId = this.validSkin(definition, this.selectedSkinId);
            this.skinScroll = 0.0F;
            this.ensureSelectedCharacterVisible(layout, index);
            this.applyLivePreview();
            return true;
        }
        return true;
    }

    private boolean handleCustomSkinContentClick(Layout layout, double mouseX, double mouseY) {
        if (!this.isInside(mouseX, mouseY, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH())) return false;
        CustomSkinContentLayout customLayout = this.customSkinContentLayout(layout, this.contentScroll);
        if (this.isInside(mouseX, mouseY, layout.contentX(), customLayout.enabledButtonY(), layout.contentW(), 22)) {
            this.customSkinEnabled = !this.customSkinEnabled;
            this.applyLivePreview();
            return true;
        }
        int sourceButtonW = (layout.contentW() - GAP) / 2;
        if (this.isInside(mouseX, mouseY, layout.contentX(), customLayout.typeButtonY(), sourceButtonW, 22)) {
            this.customSkinPlayer = true;
            this.applyLivePreview();
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.contentX() + sourceButtonW + GAP, customLayout.typeButtonY(), sourceButtonW, 22)) {
            this.customSkinPlayer = false;
            this.applyLivePreview();
            return true;
        }
        return false;
    }

    private boolean handleDisplayContentClick(Layout layout, double mouseX, double mouseY) {
        if (!this.isInside(mouseX, mouseY, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH())) return false;
        int showButtonW = Math.max(52, (layout.contentW() - GAP) * 2 / 3);
        int clearButtonW = Math.max(36, layout.contentW() - showButtonW - GAP);
        int buttonY = layout.displayNameButtonY(this.contentScroll);
        if (this.isInside(mouseX, mouseY, layout.contentX(), buttonY, showButtonW, 20)) {
            this.showName = !this.showName;
            this.applyLivePreview();
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.contentX() + showButtonW + GAP, buttonY, clearButtonW, 20)) {
            this.customName = "";
            this.syncingFields = true;
            this.customNameBox.setValue("");
            this.syncingFields = false;
            this.applyLivePreview();
            return true;
        }
        return false;
    }

    private boolean handleActionClick(Layout layout, double mouseX, double mouseY) {
        for (int index = 0; index < 4; index++) {
            if (!this.isInside(mouseX, mouseY, layout.actionX(index), layout.actionY(index), layout.actionButtonW(), layout.actionButtonH())) continue;
            if (index == 0) {
                if (!this.validInput()) return true;
                this.readFields();
                this.submitted = true;
                ClientPacketDistributor.sendToServer(this.payload(false));
                this.onClose();
            } else if (index == 1) {
                this.setYaw(0.0F, true);
                this.setScale(1.0F, true);
            } else if (index == 2) {
                this.onClose();
            } else {
                this.submitted = true;
                ClientPacketDistributor.sendToServer(this.payload(true));
                this.onClose();
            }
            return true;
        }
        return false;
    }

    private void applyLivePreview() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (!(minecraft.level.getEntity(this.entityId) instanceof ExhibitionCharacterEntity entity)) return;
        this.livePreviewEntity = entity;
        CharacterDefinition definition = this.selectedCharacter();
        entity.setCharacterId(definition.id());
        entity.setSkinId(this.selectedSkinId);
        entity.setExhibitionYaw(this.yaw);
        entity.setDisplayScale(this.scale);
        entity.setDisplayCustomName(this.customName, this.showName);
        entity.setSpeechText(this.speechText);
        entity.setCustomSkinPlayer(this.customSkinPlayer);
        entity.setCustomSkinSource(this.customSkinSource);
        entity.setCustomSkinEnabled(this.customSkinEnabled && this.validCustomSkinInput());
    }

    private void restoreLivePreview() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(this.entityId) instanceof ExhibitionCharacterEntity entity)) return;
        entity.setCharacterId(this.initialCharacterId);
        entity.setSkinId(this.initialSkinId);
        entity.setExhibitionYaw(this.initialYaw);
        entity.setDisplayScale(this.initialScale);
        entity.setDisplayCustomName(this.initialCustomName, this.initialShowName);
        entity.setSpeechText(this.initialSpeechText);
        entity.setCustomSkinPlayer(this.initialCustomSkinPlayer);
        entity.setCustomSkinSource(this.initialCustomSkinSource);
        entity.setCustomSkinEnabled(this.initialCustomSkinEnabled);
    }

    private ExhibitionCharacterConfigPayload payload(boolean remove) {
        return new ExhibitionCharacterConfigPayload(this.entityId, this.selectedCharacterId, this.selectedSkinId,
                this.yaw, this.scale, this.customName, this.showName, this.speechText, this.customSkinEnabled,
                this.customSkinPlayer, this.customSkinSource, remove);
    }

    private void yawChanged(String value) {
        if (this.syncingFields) return;
        Float parsed = this.parseFloat(value);
        if (parsed == null) return;
        this.yaw = ExhibitionCharacterEntity.normalizeYaw(parsed);
        this.applyLivePreview();
    }

    private void scaleChanged(String value) {
        if (this.syncingFields) return;
        Float parsed = this.parseFloat(value);
        if (parsed == null || parsed < ExhibitionCharacterEntity.MIN_SCALE || parsed > ExhibitionCharacterEntity.MAX_SCALE) return;
        this.scale = parsed;
        this.applyLivePreview();
    }

    private void setYaw(float yaw, boolean syncField) {
        this.yaw = ExhibitionCharacterEntity.normalizeYaw(yaw);
        if (syncField && this.yawBox != null) {
            this.syncingFields = true;
            this.yawBox.setValue(this.format(this.yaw));
            this.syncingFields = false;
        }
        this.applyLivePreview();
    }

    private void setScale(float scale, boolean syncField) {
        this.scale = Mth.clamp(scale, ExhibitionCharacterEntity.MIN_SCALE, ExhibitionCharacterEntity.MAX_SCALE);
        if (syncField && this.scaleBox != null) {
            this.syncingFields = true;
            this.scaleBox.setValue(this.format(this.scale));
            this.syncingFields = false;
        }
        this.applyLivePreview();
    }

    private void syncFields() {
        this.syncingFields = true;
        this.customNameBox.setValue(this.customName);
        this.yawBox.setValue(this.format(this.yaw));
        this.scaleBox.setValue(this.format(this.scale));
        this.speechBox.setValue(this.speechText);
        this.customSkinBox.setValue(this.customSkinSource);
        this.syncingFields = false;
    }

    private void readFields() {
        Float parsedYaw = this.parseFloat(this.yawBox.getValue());
        Float parsedScale = this.parseFloat(this.scaleBox.getValue());
        if (parsedYaw != null) this.yaw = ExhibitionCharacterEntity.normalizeYaw(parsedYaw);
        if (parsedScale != null) this.scale = Mth.clamp(parsedScale, ExhibitionCharacterEntity.MIN_SCALE, ExhibitionCharacterEntity.MAX_SCALE);
        this.customName = this.customNameBox.getValue();
        this.speechText = this.speechBox.getValue();
        this.customSkinSource = this.customSkinBox.getValue();
    }

    private boolean validInput() {
        if (this.customNameBox == null || this.yawBox == null || this.scaleBox == null || this.speechBox == null || this.customSkinBox == null) return false;
        Float parsedYaw = this.parseFloat(this.yawBox.getValue());
        Float parsedScale = this.parseFloat(this.scaleBox.getValue());
        return parsedYaw != null && Float.isFinite(parsedYaw) && parsedScale != null && Float.isFinite(parsedScale)
                && parsedScale >= ExhibitionCharacterEntity.MIN_SCALE && parsedScale <= ExhibitionCharacterEntity.MAX_SCALE
                && (!this.customSkinEnabled || this.validCustomSkinInput());
    }

    private boolean validCustomSkinInput() {
        String source = this.customSkinBox == null ? this.customSkinSource : this.customSkinBox.getValue();
        return ExhibitionCharacterEntity.validCustomSkinSource(this.customSkinPlayer, source);
    }

    private CharacterDefinition selectedCharacter() {
        return this.character(this.selectedCharacterId);
    }

    private CharacterDefinition character(Identifier id) {
        for (CharacterDefinition definition : this.characters) if (definition.id().equals(id)) return definition;
        return this.characters.getFirst();
    }

    private boolean hasCharacter(Identifier id) {
        if (id == null) return false;
        return this.characters.stream().anyMatch(definition -> definition.id().equals(id));
    }

    private String validSkin(CharacterDefinition definition, String preferred) {
        if (definition == null || definition.skins().isEmpty()) return "default";
        return definition.skins().stream().map(CharacterSkinDefinition::id).filter(id -> id.equals(preferred))
                .findFirst().orElse(definition.skins().getFirst().id());
    }

    private int characterColumns(Layout layout) {
        return Math.max(1, (layout.contentW() + GAP) / (CHARACTER_CARD_W + GAP));
    }

    private int skinColumns(Layout layout) {
        return Math.max(1, (layout.skinContentW() + GAP) / (SKIN_CARD_W + GAP));
    }

    private int skinContentHeight(Layout layout) {
        int rows = (this.selectedCharacter().skins().size() + this.skinColumns(layout) - 1) / this.skinColumns(layout);
        return Math.max(0, rows * (SKIN_CARD_H + GAP) - GAP);
    }

    private float maxSkinScroll(Layout layout) {
        return Math.max(0.0F, this.skinContentHeight(layout) - layout.skinContentH());
    }

    private void ensureSelectedCharacterVisible(Layout layout, int characterIndex) {
        int row = characterIndex / this.characterColumns(layout);
        int cardTop = 16 + row * (CHARACTER_CARD_H + GAP);
        int cardBottom = cardTop + CHARACTER_CARD_H;
        float scroll = this.contentScroll;
        if (cardTop < scroll) scroll = cardTop;
        if (cardBottom > scroll + layout.contentH()) scroll = cardBottom - layout.contentH();
        this.contentScroll = Mth.clamp(scroll, 0.0F, this.maxContentScroll(layout));
        this.updateWidgetLayout(layout);
    }

    private float maxContentScroll(Layout layout) {
        return Math.max(0.0F, this.contentHeight(layout) - layout.contentH());
    }

    private int contentHeight(Layout layout) {
        if (this.tab == ConfigTab.CUSTOM_SKIN) return this.customSkinContentLayout(layout, 0.0F).contentHeight();
        if (this.tab == ConfigTab.DISPLAY) return 154;
        int characterRows = (this.characters.size() + this.characterColumns(layout) - 1) / this.characterColumns(layout);
        int characterGridH = Math.max(0, characterRows * (CHARACTER_CARD_H + GAP) - GAP);
        return 16 + characterGridH + 5;
    }

    private CustomSkinContentLayout customSkinContentLayout(Layout layout, float scroll) {
        int baseY = layout.contentY() - Math.round(scroll);
        int y = baseY + 2;
        int descriptionY = y;
        y += this.wrappedTextHeight(Component.translatable("gui.astral_craft.exhibition_character.custom_skin_description"), layout.contentW()) + 8;
        int enabledButtonY = y;
        y += 30;
        int typeLabelY = y;
        y += 13;
        int typeButtonY = y;
        y += 31;
        int sourceLabelY = y;
        y += 13;
        int sourceBoxY = y;
        y += 25;
        int hintY = y;
        Component hint = Component.translatable(this.customSkinPlayer
                ? "gui.astral_craft.exhibition_character.custom_skin_player_hint" : "gui.astral_craft.exhibition_character.custom_skin_resource_hint");
        y += this.wrappedTextHeight(hint, layout.contentW()) + 5;
        int statusY = y;
        int statusH = Math.max(
                this.wrappedTextHeight(Component.translatable("gui.astral_craft.exhibition_character.custom_skin_invalid"), layout.contentW()),
                this.wrappedTextHeight(Component.translatable("gui.astral_craft.exhibition_character.custom_skin_priority"), layout.contentW()));
        y += statusH + 2;
        return new CustomSkinContentLayout(descriptionY, enabledButtonY, typeLabelY, typeButtonY, sourceLabelY, sourceBoxY, hintY, statusY, y - baseY + 2);
    }

    private int wrappedTextHeight(Component text, int width) {
        return Math.max(TEXT_LINE_H, this.font.split(text, Math.max(1, width)).size() * TEXT_LINE_H);
    }

    private void renderWrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, Math.max(1, width));
        for (int line = 0; line < lines.size(); line++) {
            graphics.text(this.font, lines.get(line), x, y + line * TEXT_LINE_H, color, false);
        }
    }

    private void updateContentScrollFromMouse(Layout layout, double mouseY) {
        this.contentScroll = AstralVerticalScrollbar.scrollFromMouse(mouseY, layout.contentY(), layout.contentH(), this.maxContentScroll(layout));
        this.updateWidgetLayout(layout);
    }

    private void updateSkinScrollFromMouse(Layout layout, double mouseY) {
        this.skinScroll = AstralVerticalScrollbar.scrollFromMouse(mouseY, layout.skinContentY(), layout.skinContentH(), this.maxSkinScroll(layout));
    }

    private void updateWidgetLayout(Layout layout) {
        if (this.customNameBox == null || this.yawBox == null || this.scaleBox == null || this.speechBox == null || this.customSkinBox == null) return;
        this.customNameBox.setX(layout.contentX());
        this.customNameBox.setY(layout.displayNameBoxY(this.contentScroll));
        this.yawBox.setX(layout.contentX());
        this.yawBox.setY(layout.displayNumberBoxY(this.contentScroll));
        this.scaleBox.setX(layout.scaleBoxX());
        this.scaleBox.setY(layout.displayNumberBoxY(this.contentScroll));
        this.speechBox.setX(layout.contentX());
        this.speechBox.setY(layout.displaySpeechBoxY(this.contentScroll));
        this.customSkinBox.setX(layout.contentX());
        this.customSkinBox.setY(this.customSkinContentLayout(layout, this.contentScroll).sourceBoxY());
        this.customNameBox.setVisible(this.tab == ConfigTab.DISPLAY && this.insideContent(layout, this.customNameBox));
        this.yawBox.setVisible(this.tab == ConfigTab.DISPLAY && this.insideContent(layout, this.yawBox));
        this.scaleBox.setVisible(this.tab == ConfigTab.DISPLAY && this.insideContent(layout, this.scaleBox));
        this.speechBox.setVisible(this.tab == ConfigTab.DISPLAY && this.insideContent(layout, this.speechBox));
        this.customSkinBox.setVisible(this.tab == ConfigTab.CUSTOM_SKIN && this.insideContent(layout, this.customSkinBox));
    }

    private boolean insideContent(Layout layout, EditBox box) {
        return box.getY() >= layout.contentY() && box.getY() + box.getHeight() <= layout.contentBottom();
    }

    private boolean hoveredClickable(Layout layout, double mouseX, double mouseY) {
        if (this.isWorldPreviewArea(layout, mouseX, mouseY)) return true;
        if (AstralVerticalScrollbar.contains(mouseX, mouseY, layout.scrollbarX(), layout.contentY(), layout.contentH(), this.maxContentScroll(layout))) return true;
        if (this.isInside(mouseX, mouseY, layout.closeButtonX(), layout.closeButtonY(), layout.closeButtonW(), layout.closeButtonH())) return true;
        if (this.tab == ConfigTab.CHARACTER && this.isInside(mouseX, mouseY, layout.skinPanelX(), layout.skinPanelY(), layout.skinPanelW(), layout.skinPanelH())) return true;
        if (this.isInside(mouseX, mouseY, layout.characterTabX(), layout.tabY(), layout.tabW(), layout.tabH())
                || this.isInside(mouseX, mouseY, layout.customSkinTabX(), layout.tabY(), layout.tabW(), layout.tabH())
                || this.isInside(mouseX, mouseY, layout.displayTabX(), layout.tabY(), layout.tabW(), layout.tabH())) return true;
        for (int index = 0; index < 4; index++) {
            if (this.isInside(mouseX, mouseY, layout.actionX(index), layout.actionY(index), layout.actionButtonW(), layout.actionButtonH())) return true;
        }
        return this.isInside(mouseX, mouseY, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH());
    }

    private boolean isWorldPreviewArea(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.worldPreviewX() && mouseX <= this.worldPreviewRight(layout) && mouseY >= 2 && mouseY <= this.height - 2;
    }

    private int worldPreviewRight(Layout layout) {
        return this.tab == ConfigTab.CHARACTER ? layout.skinPanelX() - 6 : this.width - 2;
    }

    private Float parseFloat(String value) {
        if (value == null || value.isBlank() || "-".equals(value) || ".".equals(value) || "-.".equals(value)) return null;
        try {
            float parsed = Float.parseFloat(value);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) return Integer.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Layout layout() {
        int panelX = 6;
        int panelY = 6;
        int minWorldPreview = Math.max(110, this.width * 40 / 100);
        int maxSafeWidth = Math.max(160, this.width - minWorldPreview - 18);
        int desiredWidth = Math.clamp(this.width * 44L / 100, 200, 440);
        int panelW = Math.min(desiredWidth, Math.clamp(this.width - 12, 160, maxSafeWidth));
        int panelH = Math.max(1, this.height - 12);
        int innerX = panelX + 9;
        int innerW = Math.max(100, panelW - 18);
        int tabY = panelY + 28;
        int tabH = 20;
        int tabW = Math.max(30, (innerW - GAP * 2) / 3);
        int actionRows = panelW >= 330 ? 1 : 2;
        int actionButtonH = 23;
        int actionH = actionRows * actionButtonH + (actionRows - 1) * GAP;
        int actionTop = panelY + panelH - actionH - 7;
        int contentY = tabY + tabH + 7;
        int contentH = Math.max(18, actionTop - contentY - 7);
        int contentW = Math.max(60, innerW - AstralVerticalScrollbar.DEFAULT_WIDTH - 5);
        int numberBoxW = Math.max(40, (contentW - GAP) / 2);
        int actionColumns = actionRows == 1 ? 4 : 2;
        int actionButtonW = Math.max(28, (innerW - (actionColumns - 1) * GAP) / actionColumns);
        return new Layout(panelX, panelY, panelW, panelH, innerX, innerW, tabY, tabH, tabW,
                contentY, contentW, contentH, numberBoxW, actionTop, actionRows, actionButtonW, actionButtonH);
    }

    private static boolean signedDecimalOrEmpty(String value) {
        if (value.isEmpty() || "-".equals(value)) return true;
        int dots = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '-' && index == 0) continue;
            if (character == '.' && dots++ == 0) continue;
            if (!Character.isDigit(character)) return false;
        }
        return true;
    }

    private static boolean decimalOrEmpty(String value) {
        if (value.isEmpty()) return true;
        int dots = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.' && dots++ == 0) continue;
            if (!Character.isDigit(character)) return false;
        }
        return true;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private enum ConfigTab {
        CHARACTER,
        CUSTOM_SKIN,
        DISPLAY
    }

    private record CustomSkinContentLayout(int descriptionY, int enabledButtonY, int typeLabelY, int typeButtonY, int sourceLabelY,
                                           int sourceBoxY, int hintY, int statusY, int contentHeight) {}

    private record Layout(int panelX, int panelY, int panelW, int panelH, int formX, int formW, int tabY, int tabH, int tabW,
                          int contentY, int contentW, int contentH, int numberBoxW, int actionTop, int actionRows,
                          int actionButtonW, int actionButtonH) {
        private int panelRight() { return this.panelX + this.panelW; }
        private int closeButtonW() { return 20; }
        private int closeButtonH() { return 18; }
        private int closeButtonX() { return this.panelRight() - this.closeButtonW() - 7; }
        private int closeButtonY() { return this.panelY + 6; }
        private int titleCenterX() { return this.panelX + Math.max(1, this.closeButtonX() - this.panelX) / 2; }
        private int characterTabX() { return this.formX; }
        private int customSkinTabX() { return this.characterTabX() + this.tabW + GAP; }
        private int displayTabX() { return this.customSkinTabX() + this.tabW + GAP; }
        private int contentX() { return this.formX; }
        private int contentRight() { return this.contentX() + this.contentW; }
        private int contentBottom() { return this.contentY + this.contentH; }
        private int scrollbarX() { return this.contentRight() + 3; }
        private int scaleBoxX() { return this.contentX() + this.numberBoxW + GAP; }
        private int displayNameBoxY(float scroll) { return this.contentY + 14 - Math.round(scroll); }
        private int displayNameButtonY(float scroll) { return this.contentY + 39 - Math.round(scroll); }
        private int displayNumberLabelY(float scroll) { return this.contentY + 67 - Math.round(scroll); }
        private int displayNumberBoxY(float scroll) { return this.contentY + 78 - Math.round(scroll); }
        private int displaySpeechLabelY(float scroll) { return this.contentY + 106 - Math.round(scroll); }
        private int displaySpeechBoxY(float scroll) { return this.contentY + 117 - Math.round(scroll); }
        private int actionsPerRow() { return this.actionRows == 1 ? 4 : 2; }
        private int actionX(int index) { return this.formX + index % this.actionsPerRow() * (this.actionButtonW + GAP); }
        private int actionY(int index) { return this.actionTop + index / this.actionsPerRow() * (this.actionButtonH + GAP); }
        private int worldPreviewX() { return this.panelRight() + 6; }
        private int skinPanelW() {
            int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int available = Math.max(126, screenW - this.panelRight() - 12);
            int desired = Math.clamp(screenW * 32L / 100, 150, 300);
            return Math.clamp(available - 100, 126, desired);
        }
        private int skinPanelX() { return Minecraft.getInstance().getWindow().getGuiScaledWidth() - this.skinPanelW() - 6; }
        private int skinPanelY() { return this.panelY; }
        private int skinPanelH() { return Math.max(60, Minecraft.getInstance().getWindow().getGuiScaledHeight() - this.skinPanelY() - 6); }
        private int skinContentX() { return this.skinPanelX() + 7; }
        private int skinContentY() { return this.skinPanelY() + 24; }
        private int skinContentW() { return Math.max(SKIN_CARD_W, this.skinPanelW() - 14 - AstralVerticalScrollbar.DEFAULT_WIDTH - 5); }
        private int skinContentH() { return Math.max(18, this.skinPanelH() - 31); }
        private int skinContentRight() { return this.skinContentX() + this.skinContentW(); }
        private int skinContentBottom() { return this.skinContentY() + this.skinContentH(); }
        private int skinScrollbarX() { return this.skinContentRight() + 3; }
    }
}
